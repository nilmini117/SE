package com.driveflow.demo_driveflow.promotion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Override
    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found with id: " + id));
    }

    @Override
    public Promotion createPromotion(Promotion promotion) {
        if (promotion.getCouponId() == null || promotion.getCouponId().trim().isEmpty()) {
            promotion.setCouponId("CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            promotion.setCouponId(promotion.getCouponId().trim().toUpperCase());
        }
        if (promotion.getCouponCode() == null || promotion.getCouponCode().trim().isEmpty()) {
            promotion.setCouponCode(promotion.getCouponId());
        }
        if (promotion.getStatus() == null || promotion.getStatus().trim().isEmpty()) {
            promotion.setStatus("ACTIVE");
        }
        if (promotion.getVehicleSeason() == null || promotion.getVehicleSeason().trim().isEmpty()) {
            promotion.setVehicleSeason("ALL_SEASONS");
        }
        if (promotion.getVehicleCategory() == null || promotion.getVehicleCategory().trim().isEmpty()) {
            promotion.setVehicleCategory("ALL");
        }
        return promotionRepository.save(promotion);
    }

    @Override
    public Promotion updatePromotion(Long id, Promotion updatedPromotion) {
        Promotion existing = getPromotionById(id);

        // Strict Immutability Rule 1: Attempting to modify discount percentage is strictly prohibited
        if (updatedPromotion.getDiscountRate() != null && existing.getDiscountRate() != null
                && existing.getDiscountRate().compareTo(updatedPromotion.getDiscountRate()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Modifying promotion discount percentage is strictly prohibited. Only End Date may be modified."
            );
        }

        // Strict Immutability Rule 2: Attempting to modify start date is strictly prohibited
        if (updatedPromotion.getStartDate() != null && existing.getStartDate() != null
                && !existing.getStartDate().isEqual(updatedPromotion.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Modifying promotion start date is strictly prohibited. Only End Date may be modified."
            );
        }

        // Strict Immutability Rule 3: Attempting to modify coupon ID is strictly prohibited
        if (updatedPromotion.getCouponId() != null && !updatedPromotion.getCouponId().trim().isEmpty()
                && existing.getCouponId() != null && !existing.getCouponId().equalsIgnoreCase(updatedPromotion.getCouponId().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Modifying promotion coupon ID is strictly prohibited. Only End Date may be modified."
            );
        }

        // Strictly allow End Date modification
        if (updatedPromotion.getEndDate() != null) {
            existing.setEndDate(updatedPromotion.getEndDate());
            if (updatedPromotion.getEndDate().isBefore(LocalDate.now())) {
                existing.setStatus("EXPIRED");
            } else if ("EXPIRED".equalsIgnoreCase(existing.getStatus()) && !updatedPromotion.getEndDate().isBefore(LocalDate.now())) {
                existing.setStatus("ACTIVE");
            }
        }

        if (updatedPromotion.getStatus() != null && !updatedPromotion.getStatus().trim().isEmpty()) {
            existing.setStatus(updatedPromotion.getStatus());
        }

        return promotionRepository.save(existing);
    }

    @Override
    public void removePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    @Override
    public Promotion getPromotionByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion/Coupon identifier is required.");
        }
        String clean = identifier.trim();

        // 1. If numeric ID (e.g. 1, 2)
        try {
            Long numId = Long.parseLong(clean);
            Optional<Promotion> byId = promotionRepository.findById(numId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (NumberFormatException ignored) {}

        // 2. Lookup by coupon ID (e.g. SUMMER15, CPN-1234)
        Optional<Promotion> byCouponId = promotionRepository.findByCouponIdIgnoreCase(clean);
        if (byCouponId.isPresent()) {
            return byCouponId.get();
        }

        // 3. Lookup by coupon Code
        Optional<Promotion> byCouponCode = promotionRepository.findByCouponCodeIgnoreCase(clean);
        if (byCouponCode.isPresent()) {
            return byCouponCode.get();
        }

        // 4. Fallback search by title / keyword
        List<Promotion> matched = promotionRepository.findMatchingPromotions(clean);
        if (!matched.isEmpty()) {
            return matched.get(0);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon or promotion not found: " + identifier);
    }

    @Override
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDate.now());
    }
}
