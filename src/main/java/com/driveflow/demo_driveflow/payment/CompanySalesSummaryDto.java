package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.maintenance.Maintenance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for Company Sales Information, Bank Details,
 * and Profit & Loss summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySalesSummaryDto {
    private BigDecimal totalRevenue;
    private BigDecimal totalMaintenanceCosts;
    private BigDecimal netIncome;

    private int approvedPaymentsCount;
    private int maintenanceServicesCount;

    // Central Company Bank Details
    private String beneficiaryName;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String routingNumber;
    private String swiftCode;
    private String branchName;
    private String depositInstructions;

    // Breakdown records
    private List<Payment> approvedPayments;
    private List<Maintenance> maintenanceRecords;
}
