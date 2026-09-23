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

    // Refunds
    Refund issueRefund(Refund refund);
    List<Refund> getAllRefunds();
    Refund getRefundById(Long id);
    Refund approveRefund(Long id);
    Refund rejectRefund(Long id);
}
