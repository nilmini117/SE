package com.driveflow.demo_driveflow.booking.pricing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBreakdownItem {
    private String itemCode;      // BASE_RATE, PROMOTION_DISCOUNT, PAYMENT_GATE, TOTAL_PAYABLE
    private String description;   // Detailed human-readable line item description
    private BigDecimal amount;    // Monetary amount (can be positive charge or negative discount)
    private String type;          // CHARGE, CREDIT, INFO, TOTAL
}
