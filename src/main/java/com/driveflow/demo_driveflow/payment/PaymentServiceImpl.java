package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
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
    public List<Invoice> getInvoicesByCustomer(Customer customer) {
        return invoiceRepository.findByBookingCustomer(customer);
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
    }

    @Override
    public Invoice generateInvoice(Invoice invoice) {
        BigDecimal rental = invoice.getRentalAmt() != null ? invoice.getRentalAmt() : BigDecimal.ZERO;
        BigDecimal lateFee = invoice.getLateFee() != null ? invoice.getLateFee() : BigDecimal.ZERO;
        if (invoice.getTotalAmt() == null) {
            invoice.setTotalAmt(rental.add(lateFee));
        }
        if (invoice.getStatus() == null || invoice.getStatus().isBlank()) {
            invoice.setStatus("UNPAID");
        }
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice updateInvoice(Long id, Invoice updated) {
        Invoice existing = getInvoiceById(id);
        existing.setInvoiceDate(updated.getInvoiceDate());
        existing.setRentalAmt(updated.getRentalAmt());
        existing.setLateFee(updated.getLateFee());
        BigDecimal rental = updated.getRentalAmt() != null ? updated.getRentalAmt() : BigDecimal.ZERO;
        BigDecimal lateFee = updated.getLateFee() != null ? updated.getLateFee() : BigDecimal.ZERO;
        existing.setTotalAmt(rental.add(lateFee));
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getBooking() != null) {
            existing.setBooking(updated.getBooking());
        }
        return invoiceRepository.save(existing);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public List<Payment> getPaymentsByCustomer(Customer customer) {
        return paymentRepository.findByInvoiceBookingCustomer(customer);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    @Override
    public Payment processPayment(Payment payment) {
        if (payment.getStatus() == null || payment.getStatus().isBlank()) {
            payment.setStatus("COMPLETED");
        }
        return paymentRepository.save(payment);
    }

    @Override
    public Payment updatePayment(Long id, Payment updated) {
        Payment existing = getPaymentById(id);
        existing.setRefNo(updated.getRefNo());
        existing.setPaymentDate(updated.getPaymentDate());
        existing.setAmountPaid(updated.getAmountPaid());
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getInvoice() != null) {
            existing.setInvoice(updated.getInvoice());
        }
        return paymentRepository.save(existing);
    }

    @Override
    public void cancelPayment(Long id) {
        Payment payment = getPaymentById(id);
        payment.setStatus("CANCELLED");
        paymentRepository.save(payment);
    }

    @Override
    public Refund issueRefund(Refund refund) {
        if (refund.getApprovalStatus() == null || refund.getApprovalStatus().isBlank()) {
            refund.setApprovalStatus("PENDING");
        }
        return refundRepository.save(refund);
    }

    @Override
    public List<Refund> getAllRefunds() {
        return refundRepository.findAll();
    }

    @Override
    public Refund getRefundById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Refund not found: " + id));
    }

    @Override
    public Refund approveRefund(Long id) {
        Refund refund = getRefundById(id);
        refund.setApprovalStatus("APPROVED");
        return refundRepository.save(refund);
    }

    @Override
    public Refund rejectRefund(Long id) {
        Refund refund = getRefundById(id);
        refund.setApprovalStatus("REJECTED");
        return refundRepository.save(refund);
    }
}
