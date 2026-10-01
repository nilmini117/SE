package com.driveflow.demo_driveflow.booking.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingBreakdown {
    private BigDecimal baseDailyRate;
    private int durationDays;
    private BigDecimal baseCost;
    private String promotionTitle;
    private String couponId;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private BigDecimal finalTotalCost;
    private boolean promotionApplied;

    @Builder.Default
    private List<PaymentBreakdownItem> paymentBreakdownArray = new ArrayList<>();
}
