package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private com.driveflow.demo_driveflow.users.StaffRepository staffRepository;

    // --- PAYMENTS ---

    @GetMapping
    public String listPayments(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            boolean isStaff = staffRepository.findByEmail(email).isPresent();
            if (!isStaff) {
                return "redirect:/profile#invoices";
            }
        }
        model.addAttribute("payments", paymentService.getAllPayments());
        return "payment/payment-list";
    }

    @GetMapping("/new")
    public String showPaymentForm(Model model) {
        Payment payment = new Payment();
        payment.setPaymentDate(LocalDate.now());
        model.addAttribute("payment", payment);
        model.addAttribute("invoices", paymentService.getAllInvoices());
        return "payment/payment-form";
    }

    @PostMapping
    public String processPayment(@ModelAttribute Payment payment,
                                 @RequestParam(value = "invoiceId", required = false) Long invoiceId,
                                 @RequestParam(value = "paymentMethod", required = false, defaultValue = "CASH") String paymentMethod,
                                 @RequestParam(value = "bankName", required = false) String bankName,
                                 @RequestParam(value = "cardNo", required = false) String cardNo) {
        Payment toSave = payment;
        if ("CREDIT_CARD".equalsIgnoreCase(paymentMethod)) {
            CreditCardPay cc = new CreditCardPay();
            cc.setRefNo(payment.getRefNo());
            cc.setPaymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now());
            cc.setAmountPaid(payment.getAmountPaid());
            cc.setStatus("COMPLETED");
            cc.setBankName(bankName);
            cc.setCardNo(cardNo);
            toSave = cc;
        }

        if (invoiceId != null) {
            Invoice invoice = paymentService.getInvoiceById(invoiceId);
            toSave.setInvoice(invoice);
            if (toSave.getAmountPaid() != null && invoice.getTotalAmt() != null
                    && toSave.getAmountPaid().compareTo(invoice.getTotalAmt()) >= 0) {
                invoice.setStatus("PAID");
                paymentService.updateInvoice(invoice.getInvoiceId(), invoice);
            }
        }
        if (toSave.getPaymentDate() == null) {
            toSave.setPaymentDate(LocalDate.now());
        }
        if (toSave.getStatus() == null || toSave.getStatus().isBlank()) {
            toSave.setStatus("COMPLETED");
        }

        paymentService.processPayment(toSave);
        return "redirect:/payments";
    }

    @GetMapping("/{id}/edit")
    public String showPaymentEditForm(@PathVariable Long id, Model model) {
        Payment payment = paymentService.getPaymentById(id);
        model.addAttribute("payment", payment);
        model.addAttribute("invoices", paymentService.getAllInvoices());
        return "payment/payment-form";
    }

    @PostMapping("/{id}")
    public String updatePayment(@PathVariable Long id,
                                @ModelAttribute Payment payment,
                                @RequestParam(value = "invoiceId", required = false) Long invoiceId) {
        if (invoiceId != null) {
            payment.setInvoice(paymentService.getInvoiceById(invoiceId));
        }
        paymentService.updatePayment(id, payment);
        return "redirect:/payments";
    }

    @PostMapping("/{id}/cancel")
    public String cancelPaymentPost(@PathVariable Long id,
                                    @RequestParam(value = "cancelReason", required = false) String cancelReason,
                                    RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.getPaymentById(id);
            paymentService.cancelPayment(id);
            String ref = payment.getRefNo() != null ? payment.getRefNo() : ("#PAY-" + id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment transaction " + ref + " has been successfully cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel payment: " + e.getMessage());
        }
        return "redirect:/payments";
    }

    @GetMapping("/{id}/cancel")
    public String cancelPayment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.getPaymentById(id);
            paymentService.cancelPayment(id);
            String ref = payment.getRefNo() != null ? payment.getRefNo() : ("#PAY-" + id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment transaction " + ref + " has been successfully cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel payment: " + e.getMessage());
        }
        return "redirect:/payments";
    }

    // --- INVOICES ---

    @GetMapping("/invoices")
    public String listInvoices(Model model) {
        model.addAttribute("invoices", paymentService.getAllInvoices());
        return "payment/invoice-list";
    }

    @GetMapping("/invoices/new")
    public String showInvoiceForm(Model model) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(LocalDate.now());
        model.addAttribute("invoice", invoice);
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "payment/invoice-form";
    }

    @PostMapping("/invoices")
    public String generateInvoice(@ModelAttribute Invoice invoice,
                                  @RequestParam(value = "bookingId", required = false) Long bookingId) {
        if (bookingId != null) {
            Booking booking = bookingService.getBookingById(bookingId);
            invoice.setBooking(booking);
        }
        if (invoice.getInvoiceDate() == null) {
            invoice.setInvoiceDate(LocalDate.now());
        }
        paymentService.generateInvoice(invoice);
        return "redirect:/payments/invoices";
    }

    @GetMapping("/invoices/{id}/edit")
    public String showInvoiceEditForm(@PathVariable Long id, Model model) {
        Invoice invoice = paymentService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "payment/invoice-form";
    }

    @PostMapping("/invoices/{id}")
    public String updateInvoice(@PathVariable Long id,
                                @ModelAttribute Invoice invoice,
                                @RequestParam(value = "bookingId", required = false) Long bookingId) {
        if (bookingId != null) {
            Booking booking = bookingService.getBookingById(bookingId);
            invoice.setBooking(booking);
        }
        paymentService.updateInvoice(id, invoice);
        return "redirect:/payments/invoices";
    }
}
