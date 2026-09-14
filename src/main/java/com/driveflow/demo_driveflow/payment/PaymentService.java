package com.driveflow.demo_driveflow.payment;

import java.util.List;

public interface PaymentService {
    // Invoices
    List<Invoice> getAllInvoices();
    Invoice generateInvoice(Invoice invoice);

    // Payments
    List<Payment> getAllPayments();
    Payment getPaymentById(Long id);
    Payment processPayment(Payment payment);
    void cancelPayment(Long id);

    // Refunds
    Refund issueRefund(Refund refund);
}
