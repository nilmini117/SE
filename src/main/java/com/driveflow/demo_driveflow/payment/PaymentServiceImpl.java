package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.payment.strategy.*;
import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private CreditCardPayment creditCardPayment;

    @Autowired
    private PayPalPayment payPalPayment;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private HasDepositRepository hasDepositRepository;

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.email.EmailService emailService;

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
        if (payment.getInvoice() != null && payment.getInvoice().getBooking() != null) {
            String bookingStatus = payment.getInvoice().getBooking().getStatus();
            if (bookingStatus == null || !bookingStatus.equalsIgnoreCase("CONFIRMED")) {
                throw new IllegalStateException("Payment cannot be processed because booking #"
                        + payment.getInvoice().getBooking().getBookingId()
                        + " has not been confirmed by staff (status: " + bookingStatus + ").");
            }
        }
        if (payment.getStatus() == null || payment.getStatus().isBlank()) {
            payment.setStatus("COMPLETED");
        }
        Payment saved = paymentRepository.save(payment);

        // Lifecycle Email Trigger: Payment Successful (Digital Receipt in Rs.)
        if (emailService != null && "COMPLETED".equalsIgnoreCase(saved.getStatus())) {
            try {
                if (saved.getInvoice() != null && saved.getInvoice().getBooking() != null) {
                    com.driveflow.demo_driveflow.booking.Booking booking = saved.getInvoice().getBooking();
                    Customer cust = booking.getCustomer();
                    if (cust != null && cust.getEmail() != null && !cust.getEmail().isBlank()) {
                        String ref = saved.getRefNo() != null ? saved.getRefNo() : ("PAY-" + saved.getPaymentId());

                        // Resolve customer name from the Customer entity (fallback if empty)
                        String custName = "Valued Customer";
                        if (cust.getName() != null && !cust.getName().isBlank()) {
                            custName = cust.getName();
                        }

                        emailService.sendPaymentReceiptEmail(
                                cust.getEmail(),
                                custName,
                                ref,
                                saved.getInvoice().getInvoiceId(),
                                booking.getBookingId(),
                                saved.getAmountPaid(),
                                saved.getPaymentDate() != null ? saved.getPaymentDate() : java.time.LocalDate.now());
                        emailService.sendNotification(
                                cust.getEmail(),
                                "DriveFlow Payment Receipt - Ref: " + ref,
                                "Dear " + custName + ",\n\nWe have received your payment of Rs. "
                                        + saved.getAmountPaid() +
                                        " for invoice #" + saved.getInvoice().getInvoiceId() + " (Booking #"
                                        + booking.getBookingId() +
                                        ").\nReference No: " + ref
                                        + "\n\nThank you for choosing DriveFlow!\nDriveFlow Accounts");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return saved;
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
    public void processCustomerPayment(String methodType, double amount, String bookingId) {
        PaymentContext context = new PaymentContext();
        if (methodType != null && methodType.equalsIgnoreCase("CREDIT_CARD")) {
            context.setPaymentStrategy(creditCardPayment);
        } else if (methodType != null && methodType.equalsIgnoreCase("PAYPAL")) {
            context.setPaymentStrategy(payPalPayment);
        }
        context.checkout(amount, bookingId);
    }

    @Override
    public void processCustomerPayment(String methodType, double amount, String bookingId, String payerEmail) {
        PaymentContext context = new PaymentContext();

        // Dynamically assign the strategy based on frontend input
        if (methodType != null && methodType.equalsIgnoreCase("CREDIT_CARD")) {
            context.setPaymentStrategy(creditCardPayment);
        } else if (methodType != null && methodType.equalsIgnoreCase("PAYPAL")) {
            context.setPaymentStrategy(payPalPayment);
        }

        // Execute the payment without knowing the underlying details
        context.checkout(amount, bookingId, payerEmail);
    }

    @Override
    public BigDecimal calculateLateFee(long daysLate, BigDecimal dailyRentalRate) {
        if (daysLate <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = (dailyRentalRate != null) ? dailyRentalRate : BigDecimal.ZERO;
        BigDecimal days = BigDecimal.valueOf(daysLate);
        // Formula: Total Charge = (Days Late * Daily Rental Rate) + (Days Late * 1000)
        return days.multiply(rate).add(days.multiply(new BigDecimal("1000.00")));
    }

    @Override
    public BigDecimal calculateLateFee(com.driveflow.demo_driveflow.booking.Booking booking, LocalDate actualReturnDate) {
        if (booking == null || booking.getEndDate() == null || actualReturnDate == null) {
            return BigDecimal.ZERO;
        }
        if (!actualReturnDate.isAfter(booking.getEndDate())) {
            return BigDecimal.ZERO;
        }
        long daysLate = java.time.temporal.ChronoUnit.DAYS.between(booking.getEndDate(), actualReturnDate);
        BigDecimal dailyRate = BigDecimal.ZERO;
        if (booking.getVehicle() != null && booking.getVehicle().getDailyRate() != null) {
            dailyRate = booking.getVehicle().getDailyRate();
        } else if (booking.getChargedRate() != null && booking.getDuration() != null && booking.getDuration() > 0) {
            dailyRate = booking.getChargedRate().divide(BigDecimal.valueOf(booking.getDuration()), 2, java.math.RoundingMode.HALF_UP);
        }
        return calculateLateFee(daysLate, dailyRate);
    }

    @Override
    public Refund issueRefund(Refund refund) {
        if (refund.getApprovalStatus() == null || refund.getApprovalStatus().isBlank()) {
            refund.setApprovalStatus("PENDING");
        }
        if (refund.getRefundDate() == null) {
            refund.setRefundDate(LocalDate.now());
        }
        return refundRepository.save(refund);
    }

    @Override
    public List<Refund> getAllRefunds() {
        return refundRepository.findAllByOrderByRefundDateDesc();
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

    // Security Deposits (has_deposit)
    @Override
    public List<HasDeposit> getAllDeposits() {
        return hasDepositRepository.findAllByOrderByDepositDateDesc();
    }

    @Override
    public HasDeposit logSecurityDeposit(HasDeposit deposit) {
        if (deposit.getStatus() == null || deposit.getStatus().isBlank()) {
            deposit.setStatus("HELD");
        }
        if (deposit.getDepositDate() == null) {
            deposit.setDepositDate(LocalDate.now());
        }
        return hasDepositRepository.save(deposit);
    }

    @Override
    public HasDeposit getDepositById(Long depositId) {
        return hasDepositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Security deposit not found: " + depositId));
    }

    @Override
    public HasDeposit refundDeposit(Long depositId) {
        HasDeposit deposit = getDepositById(depositId);
        deposit.setStatus("REFUNDED");
        return hasDepositRepository.save(deposit);
    }

    @Override
    public HasDeposit forfeitDeposit(Long depositId) {
        HasDeposit deposit = getDepositById(depositId);
        deposit.setStatus("FORFEITED");
        return hasDepositRepository.save(deposit);
    }

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.maintenance.MaintenanceRepository maintenanceRepository;

    @Override
    public CompanySalesSummaryDto getCompanySalesSummary() {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalMaintenanceCosts = BigDecimal.ZERO;

        // Try the native cross-table SQL aggregation query first
        try {
            CompanySalesProjection projection = paymentRepository.getCompanySalesSummarySql();
            if (projection != null) {
                totalRevenue = projection.getTotalRevenue() != null ? projection.getTotalRevenue() : BigDecimal.ZERO;
                totalMaintenanceCosts = projection.getTotalMaintenanceCosts() != null
                        ? projection.getTotalMaintenanceCosts()
                        : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            // Fallback to separate repository calculations
            try {
                BigDecimal rev = paymentRepository.calculateTotalRevenue();
                if (rev != null)
                    totalRevenue = rev;
            } catch (Exception ignored) {
            }

            try {
                if (maintenanceRepository != null) {
                    BigDecimal mnt = maintenanceRepository.calculateTotalMaintenanceCosts();
                    if (mnt != null)
                        totalMaintenanceCosts = mnt;
                }
            } catch (Exception ignored) {
            }
        }

        // Real net income calculation: SUM(revenue) - SUM(maintenance_costs)
        BigDecimal netIncome = totalRevenue.subtract(totalMaintenanceCosts);

        // Fetch detailed records for the data tables
        List<Payment> approvedPayments = paymentRepository.findAllApprovedPayments();
        List<com.driveflow.demo_driveflow.maintenance.Maintenance> maintenanceRecords = java.util.Collections
                .emptyList();
        if (maintenanceRepository != null) {
            maintenanceRecords = maintenanceRepository.findAll();
        }

        return CompanySalesSummaryDto.builder()
                .totalRevenue(totalRevenue)
                .totalMaintenanceCosts(totalMaintenanceCosts)
                .netIncome(netIncome)
                .approvedPaymentsCount(approvedPayments.size())
                .maintenanceServicesCount(maintenanceRecords.size())
                .beneficiaryName("DriveFlow Car Rental Systems Inc.")
                .bankName("Commercial Bank of Ceylon (Corporate Banking Division)")
                .accountName("DriveFlow Corporate Operating Fund")
                .accountNumber("1000-8842-9931-5021")
                .routingNumber("071000288")
                .swiftCode("CBCLKLX")
                .branchName("Colombo Central Main Hub")
                .depositInstructions(
                        "Please quote Customer Invoice ID (#INV-XXXX) or Booking ID (#BK-XXXX) in payment reference.")
                .approvedPayments(approvedPayments)
                .maintenanceRecords(maintenanceRecords)
                .build();
    }
}
