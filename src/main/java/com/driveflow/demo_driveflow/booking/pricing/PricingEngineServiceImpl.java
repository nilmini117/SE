package com.driveflow.demo_driveflow.booking.pricing;

import com.driveflow.demo_driveflow.promotion.Promotion;
import com.driveflow.demo_driveflow.promotion.PromotionRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PricingEngineServiceImpl implements PricingEngineService {

    public static final BigDecimal DEFAULT_BASE_DAILY_RATE = new BigDecimal("3000.00");

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public PricingBreakdown calculatePricing(Long vehicleId, Long pickupBranchId, LocalDate startDate, LocalDate endDate, String couponCode) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now();
        LocalDate end = (endDate != null) ? endDate : start.plusDays(1);
        if (end.isBefore(start)) {
            end = start;
        }

        long days = ChronoUnit.DAYS.between(start, end);
        int durationDays = days > 0 ? (int) days : 1;

        BigDecimal baseDailyRate = DEFAULT_BASE_DAILY_RATE;
        Vehicle vehicle = null;

        // Check if vehicle has any custom status or rate
        if (vehicleId != null) {
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isPresent()) {
                vehicle = vehicleOpt.get();
                if (vehicle.getDailyRate() != null && vehicle.getDailyRate().compareTo(BigDecimal.ZERO) > 0) {
                    baseDailyRate = vehicle.getDailyRate();
                }
            }
        }

        BigDecimal baseCost = baseDailyRate.multiply(BigDecimal.valueOf(durationDays)).setScale(2, RoundingMode.HALF_UP);

        // Fetch seasonal promotions or coupon IDs linked to the vehicle
        Promotion appliedPromo = null;
        String couponIdToRecord = null;
        boolean couponSpecified = (couponCode != null && !couponCode.trim().isBlank());
        boolean couponValid = false;
        String couponMessage = null;

        // 1. If customer entered a coupon ID / promotion code, evaluate it
        if (couponSpecified) {
            String cleanCode = couponCode.trim();
            Optional<Promotion> couponPromoOpt = promotionRepository.findByCouponIdIgnoreCaseAndStatusIgnoreCase(cleanCode, "ACTIVE");
            if (couponPromoOpt.isEmpty()) {
                couponPromoOpt = promotionRepository.findByCouponCodeIgnoreCaseAndStatusIgnoreCase(cleanCode, "ACTIVE");
            }
            if (couponPromoOpt.isEmpty()) {
                // Check if coupon exists but is inactive / expired
                Optional<Promotion> anyPromo = promotionRepository.findByCouponIdIgnoreCase(cleanCode);
                if (anyPromo.isEmpty()) {
                    anyPromo = promotionRepository.findByCouponCodeIgnoreCase(cleanCode);
                }
                if (anyPromo.isPresent()) {
                    Promotion p = anyPromo.get();
                    if (!"ACTIVE".equalsIgnoreCase(p.getStatus())) {
                        couponMessage = "Coupon '" + cleanCode + "' is currently " + (p.getStatus() != null ? p.getStatus().toLowerCase() : "inactive") + ".";
                    }
                }
            }
            if (couponPromoOpt.isEmpty() && couponMessage == null) {
                // Try matching by title
                List<Promotion> matched = promotionRepository.findMatchingPromotions(cleanCode);
                if (!matched.isEmpty()) {
                    couponPromoOpt = Optional.of(matched.get(0));
                }
            }
            if (couponPromoOpt.isPresent()) {
                Promotion promo = couponPromoOpt.get();
                try {
                    validateCouponForVehicle(promo, vehicle);
                    appliedPromo = promo;
                    couponIdToRecord = appliedPromo.getCouponId() != null ? appliedPromo.getCouponId() : cleanCode;
                    couponValid = true;
                    BigDecimal rate = appliedPromo.getDiscountRate() != null ? appliedPromo.getDiscountRate() : BigDecimal.ZERO;
                    couponMessage = "Coupon '" + couponIdToRecord + "' applied successfully! (" + rate.stripTrailingZeros().toPlainString() + "% OFF)";
                } catch (RuntimeException ex) {
                    couponValid = false;
                    couponMessage = ex.getMessage();
                }
            } else if (couponMessage == null) {
                couponMessage = "Invalid coupon code '" + cleanCode + "'. Please check the code and try again.";
            }
        }

        // 2. If no coupon was specified, fetch any active seasonal promotion for the vehicle or general fleet
        if (appliedPromo == null && !couponSpecified) {
            List<Promotion> activePromos;
            if (vehicleId != null) {
                activePromos = promotionRepository.findActivePromotionsForVehicle(vehicleId, LocalDate.now());
            } else {
                activePromos = promotionRepository.findActivePromotions(LocalDate.now());
            }

            if (!activePromos.isEmpty()) {
                // Pick active promotion with the most favorable discount rate
                appliedPromo = activePromos.stream()
                        .max((p1, p2) -> {
                            BigDecimal r1 = p1.getDiscountRate() != null ? p1.getDiscountRate() : BigDecimal.ZERO;
                            BigDecimal r2 = p2.getDiscountRate() != null ? p2.getDiscountRate() : BigDecimal.ZERO;
                            return r1.compareTo(r2);
                        }).orElse(activePromos.get(0));
                couponIdToRecord = appliedPromo.getCouponId() != null ? appliedPromo.getCouponId() : appliedPromo.getCouponCode();
            }
        }

        BigDecimal discountRate = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String promoTitle = null;
        boolean promoApplied = false;

        if (appliedPromo != null && appliedPromo.getDiscountRate() != null && appliedPromo.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
            discountRate = appliedPromo.getDiscountRate();
            discountAmount = baseCost.multiply(discountRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            promoTitle = appliedPromo.getTitle();
            promoApplied = true;
        }

        BigDecimal finalTotalCost = baseCost.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        if (finalTotalCost.compareTo(BigDecimal.ZERO) < 0) {
            finalTotalCost = BigDecimal.ZERO;
        }

        // Expose the final calculated payment array to the frontend
        List<PaymentBreakdownItem> paymentArray = new ArrayList<>();
        paymentArray.add(new PaymentBreakdownItem(
                "BASE_RENTAL",
                "Base Rental Rate (" + durationDays + " Day" + (durationDays > 1 ? "s" : "") + " @ Rs. " + baseDailyRate.setScale(2) + " / day)",
                baseCost,
                "CHARGE"
        ));

        if (promoApplied) {
            String discountDesc = "Seasonal Promotion: " + promoTitle + " (-" + discountRate.stripTrailingZeros().toPlainString() + "%)";
            if (couponIdToRecord != null && !couponIdToRecord.isBlank()) {
                discountDesc += " [Coupon: " + couponIdToRecord + "]";
            }
            paymentArray.add(new PaymentBreakdownItem(
                    "PROMOTION_DISCOUNT",
                    discountDesc,
                    discountAmount.negate(),
                    "CREDIT"
            ));
        }

        paymentArray.add(new PaymentBreakdownItem(
                "PAYMENT_GATE_LOCKED",
                "Payment Gate: Locked (No charge upon booking creation; payable upon staff approval)",
                BigDecimal.ZERO.setScale(2),
                "INFO"
        ));

        paymentArray.add(new PaymentBreakdownItem(
                "TOTAL_DUE",
                "Final Calculated Rental Cost",
                finalTotalCost,
                "TOTAL"
        ));

        return PricingBreakdown.builder()
                .baseDailyRate(baseDailyRate)
                .durationDays(durationDays)
                .baseCost(baseCost)
                .promotionTitle(promoTitle)
                .couponId(couponIdToRecord)
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .finalTotalCost(finalTotalCost)
                .promotionApplied(promoApplied)
                .couponValid(couponValid)
                .couponMessage(couponMessage)
                .paymentBreakdownArray(paymentArray)
                .build();
    }

    @Override
    public void validateCouponForVehicle(Promotion promotion, Vehicle vehicle) {
        if (promotion == null || vehicle == null) {
            return;
        }
        // Check if the coupon is universal OR matches the specific car type
        if (promotion.getCategory().equalsIgnoreCase("ALL") || 
            promotion.getCategory().equalsIgnoreCase("ALL_FLEET") ||
            promotion.getCategory().equalsIgnoreCase(vehicle.getCategory())) {
            
            // Apply the discount calculation
            
        } else {
            throw new RuntimeException("This coupon is not valid for this vehicle category.");
        }
    }
}
