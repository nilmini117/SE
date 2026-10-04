package com.driveflow.demo_driveflow.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API Controller for Company Sales Information and Profit & Loss
 * calculations.
 * Accessible for frontend dashboards and aggregation queries:
 * SUM(revenue) - SUM(maintenance_costs).
 */
@RestController
@RequestMapping("/api/company-sales")
public class CompanySalesApiController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public ResponseEntity<CompanySalesSummaryDto> getCompanySalesSummary() {
        return ResponseEntity.ok(paymentService.getCompanySalesSummary());
    }
}
