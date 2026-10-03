package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p WHERE p.invoice.booking.customer = :customer")
    List<Payment> findByInvoiceBookingCustomer(@Param("customer") Customer customer);

    List<Payment> findByInvoice(Invoice invoice);

    /**
     * Cross-table SQL aggregation query that aggregates from both the Payment (revenue)
     * and Maintenance (expenses) tables to calculate:
     * SUM(revenue) - SUM(maintenance_costs).
     */
    @Query(value = "SELECT " +
            "(SELECT COALESCE(SUM(p.amount_paid), 0) FROM payment p WHERE p.status IS NULL OR UPPER(p.status) != 'CANCELLED') AS totalRevenue, " +
            "(SELECT COALESCE(SUM(m.approximated_cost), 0) FROM maintenance m) AS totalMaintenanceCosts, " +
            "((SELECT COALESCE(SUM(p.amount_paid), 0) FROM payment p WHERE p.status IS NULL OR UPPER(p.status) != 'CANCELLED') - " +
            " (SELECT COALESCE(SUM(m.approximated_cost), 0) FROM maintenance m)) AS netIncome",
            nativeQuery = true)
    CompanySalesProjection getCompanySalesSummarySql();

    @Query(value = "SELECT COALESCE(SUM(p.amount_paid), 0) FROM payment p WHERE p.status IS NULL OR UPPER(p.status) != 'CANCELLED'", nativeQuery = true)
    java.math.BigDecimal calculateTotalRevenue();

    @Query("SELECT p FROM Payment p WHERE p.status IS NULL OR UPPER(p.status) != 'CANCELLED' ORDER BY p.paymentDate DESC, p.paymentId DESC")
    List<Payment> findAllApprovedPayments();
}

