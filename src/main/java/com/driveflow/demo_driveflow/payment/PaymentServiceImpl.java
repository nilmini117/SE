package com.driveflow.demo_driveflow.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice generateInvoice(Invoice invoice) {
        invoice.setStatus("UNPAID");
        return invoiceRepository.save(invoice);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    @Override
    public Payment processPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public void cancelPayment(Long id) {
        paymentRepository.deleteById(id);
    }

    @Override
    public Refund issueRefund(Refund refund) {
        refund.setApprovalStatus("PENDING");
        return refundRepository.save(refund);
    }
}
