package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/finance")
public class FinanceController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping
    public String showFinancePanel(Model model) {
        List<HasDeposit> deposits = paymentService.getAllDeposits();
        List<Refund> refunds = paymentService.getAllRefunds();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentService.getAllPayments();

        BigDecimal totalDepositsHeld = deposits.stream()
                .filter(d -> "HELD".equalsIgnoreCase(d.getStatus()))
                .map(HasDeposit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDepositsRefunded = deposits.stream()
                .filter(d -> "REFUNDED".equalsIgnoreCase(d.getStatus()))
                .map(HasDeposit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingRefundsCount = refunds.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getApprovalStatus()))
                .count();

        BigDecimal approvedRefundsTotal = refunds.stream()
                .filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus()))
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("deposits", deposits);
        model.addAttribute("refunds", refunds);
        model.addAttribute("bookings", bookings);
        model.addAttribute("payments", payments);
        model.addAttribute("totalDepositsHeld", totalDepositsHeld);
        model.addAttribute("totalDepositsRefunded", totalDepositsRefunded);
        model.addAttribute("pendingRefundsCount", pendingRefundsCount);
        model.addAttribute("approvedRefundsTotal", approvedRefundsTotal);

        return "payment/finance-panel";
    }

    @PostMapping("/deposits")
    public String logDeposit(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam(value = "paymentMethod", defaultValue = "CASH") String paymentMethod,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

            HasDeposit deposit = new HasDeposit();
            deposit.setBooking(booking);
            deposit.setAmount(amount != null ? amount : new BigDecimal("200.00"));
            deposit.setStatus("HELD");
            deposit.setPaymentMethod(paymentMethod);
            deposit.setNotes(notes);
            deposit.setDepositDate(LocalDate.now());

            paymentService.logSecurityDeposit(deposit);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Security deposit of Rs. " + deposit.getAmount() + " successfully recorded for Booking #BK-" + bookingId + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to record security deposit: " + e.getMessage());
        }
        return "redirect:/finance";
    }

    @PostMapping("/deposits/{id}/refund")
    public String refundDeposit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            HasDeposit deposit = paymentService.refundDeposit(id);

            // Also record an approved refund entry for accounting
            Refund refund = new Refund();
            refund.setAmount(deposit.getAmount());
            refund.setBooking(deposit.getBooking());
            refund.setPayment(deposit.getPayment());
            refund.setApprovalStatus("APPROVED");
            refund.setReason("Security Deposit Return (Booking #BK-" + (deposit.getBooking() != null ? deposit.getBooking().getBookingId() : "N/A") + ")");
            refund.setRefundDate(LocalDate.now());
            paymentService.issueRefund(refund);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Security deposit #DEP-" + id + " marked as REFUNDED and logged in refund records.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to refund deposit: " + e.getMessage());
        }
        return "redirect:/finance";
    }

    @PostMapping("/deposits/{id}/forfeit")
    public String forfeitDeposit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.forfeitDeposit(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Security deposit #DEP-" + id + " marked as FORFEITED due to vehicle damage / contract breach.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to forfeit deposit: " + e.getMessage());
        }
        return "redirect:/finance";
    }

    @PostMapping("/refunds")
    public String issueRefund(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("reason") String reason,
            @RequestParam(value = "immediateApproval", defaultValue = "true") boolean immediateApproval,
            RedirectAttributes redirectAttributes) {

        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

            Refund refund = new Refund();
            refund.setBooking(booking);
            refund.setAmount(amount);
            refund.setReason(reason);
            refund.setApprovalStatus(immediateApproval ? "APPROVED" : "PENDING");
            refund.setRefundDate(LocalDate.now());

            paymentService.issueRefund(refund);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Refund of Rs. " + amount + " successfully issued for Booking #BK-" + bookingId + " (" + refund.getApprovalStatus() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to issue refund: " + e.getMessage());
        }
        return "redirect:/finance";
    }

    @PostMapping("/refunds/{id}/approve")
    public String approveRefund(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.approveRefund(id);
            redirectAttributes.addFlashAttribute("successMessage", "Refund #REF-" + id + " approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve refund: " + e.getMessage());
        }
        return "redirect:/finance";
    }

    @PostMapping("/refunds/{id}/reject")
    public String rejectRefund(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.rejectRefund(id);
            redirectAttributes.addFlashAttribute("successMessage", "Refund #REF-" + id + " rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject refund: " + e.getMessage());
        }
        return "redirect:/finance";
    }
}
