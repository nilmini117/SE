package com.driveflow.demo_driveflow.booking.pricing;

import java.time.LocalDate;

public interface PricingEngineService {

    /**
     * Calculates the itemized pricing structure by applying active seasonal promotions
     * or coupon discounts to the base rate, exposing the final calculated payment array.
     *
     * @param vehicleId      Selected vehicle ID
     * @param pickupBranchId Mandatory pickup branch ID
     * @param startDate      Booking start date
     * @param endDate        Booking end date
     * @param couponCode     Optional coupon ID / promotion code
     * @return Itemized PricingBreakdown with final calculated payment array
     */
    PricingBreakdown calculatePricing(Long vehicleId, Long pickupBranchId, LocalDate startDate, LocalDate endDate, String couponCode);

    void validateCouponForVehicle(com.driveflow.demo_driveflow.promotion.Promotion promotion, com.driveflow.demo_driveflow.vehicle.Vehicle vehicle);
}
