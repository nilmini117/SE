package com.driveflow.demo_driveflow.payment;

import java.math.BigDecimal;

/**
 * Projection interface for the cross-table SQL aggregation query:
 * SUM(revenue) - SUM(maintenance_costs).
 */
public interface CompanySalesProjection {
    BigDecimal getTotalRevenue();
    BigDecimal getTotalMaintenanceCosts();
    BigDecimal getNetIncome();
}
