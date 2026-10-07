package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.users.Customer;
import java.util.List;

public interface PaymentService {
    // Invoices
    List<Invoice> getAllInvoices();
    List<Invoice> getInvoicesByCustomer(Customer customer);
    Invoice getInvoiceById(Long id);
    Invoice generateInvoice(Invoice invoice);
    Invoice updateInvoice(Long id, Invoice invoice);

    // Payments
    List<Payment> getAllPayments();
    List<Payment> getPaymentsByCustomer(Customer customer);
    Payment getPaymentById(Long id);
    Payment processPayment(Payment payment);
    Payment updatePayment(Long id, Payment payment);
    void cancelPayment(Long id);
    void processCustomerPayment(String methodType, double amount, String bookingId);
    void processCustomerPayment(String methodType, double amount, String bookingId, String payerEmail);

    // Late Fee Calculation
    java.math.BigDecimal calculateLateFee(long daysLate, java.math.BigDecimal dailyRentalRate);
    java.math.BigDecimal calculateLateFee(com.driveflow.demo_driveflow.booking.Booking booking, java.time.LocalDate actualReturnDate);

    // Refunds
    Refund issueRefund(Refund refund);
    List<Refund> getAllRefunds();
    Refund getRefundById(Long id);
    Refund approveRefund(Long id);
    Refund rejectRefund(Long id);

    // Security Deposits (has_deposit)
    List<HasDeposit> getAllDeposits();
    HasDeposit logSecurityDeposit(HasDeposit deposit);
    HasDeposit getDepositById(Long depositId);
    HasDeposit refundDeposit(Long depositId);
    HasDeposit forfeitDeposit(Long depositId);

    // Company Sales & Financial Summary
    CompanySalesSummaryDto getCompanySalesSummary();
}
