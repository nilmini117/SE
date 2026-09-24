package com.driveflow.demo_driveflow.users;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.payment.CreditCardPay;
import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.Payment;
import com.driveflow.demo_driveflow.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private com.driveflow.demo_driveflow.incident.IncidentService incidentService;

    @GetMapping
    public String viewProfile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (userService.findStaffByEmail(email).isPresent()) {
            return "redirect:/"; // Redirect staff members to home dashboard
        }

        Optional<Customer> customerOpt = userService.findCustomerByEmail(email);
        if (customerOpt.isEmpty()) {
            return "redirect:/login";
        }

        Customer customer = customerOpt.get();
        List<Booking> customerBookings = bookingService.getBookingsByCustomer(customer);
        List<Invoice> customerInvoices = paymentService.getInvoicesByCustomer(customer);
        List<Payment> customerPayments = paymentService.getPaymentsByCustomer(customer);
        List<com.driveflow.demo_driveflow.incident.Incident> customerIncidents = incidentService.getIncidentsByCustomer(customer);

        model.addAttribute("customer", customer);
        model.addAttribute("bookings", customerBookings);
        model.addAttribute("invoices", customerInvoices);
        model.addAttribute("payments", customerPayments);
        model.addAttribute("incidents", customerIncidents);
        return "profile/profile";
    }

    @GetMapping("/edit")
    public String showEditProfileForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (userService.findStaffByEmail(email).isPresent()) {
            return "redirect:/";
        }

        Customer customer = userService.findCustomerByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        model.addAttribute("customer", customer);
        return "profile/profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@RequestParam("firstName") String firstName,
                                @RequestParam("lastName") String lastName,
                                @RequestParam("contactNumber") String contactNumber,
                                @RequestParam("drivingLicense") String drivingLicense,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        try {
            userService.updateCustomerProfile(email, firstName, lastName, contactNumber, drivingLicense);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile/edit";
        }

        return "redirect:/profile";
    }

    @GetMapping("/password")
    public String showChangePasswordForm(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        return "profile/password-change";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirm password do not match.");
            return "redirect:/profile/password";
        }

        try {
            userService.changePassword(authentication.getName(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile/password";
        }

        return "redirect:/profile";
    }

    @GetMapping("/invoices/{id}/pay")
    public String showPayInvoiceForm(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (userService.findStaffByEmail(email).isPresent()) {
            return "redirect:/payments";
        }

        Invoice invoice = paymentService.getInvoiceById(id);
        if (invoice == null || invoice.getBooking() == null || invoice.getBooking().getCustomer() == null
                || invoice.getBooking().getCustomer().getEmail() == null
                || !invoice.getBooking().getCustomer().getEmail().trim().equalsIgnoreCase(email.trim())) {
            return "redirect:/profile?error=unauthorized";
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            return "redirect:/profile?info=already-paid";
        }

        Customer customer = invoice.getBooking().getCustomer();
        model.addAttribute("invoice", invoice);
        model.addAttribute("customer", customer);
        return "profile/invoice-pay";
    }

    @PostMapping("/invoices/{id}/pay")
    public String processInvoicePayment(
            @PathVariable Long id,
            @RequestParam("bankName") String bankName,
            @RequestParam("cardNo") String cardNo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (userService.findStaffByEmail(email).isPresent()) {
            return "redirect:/payments";
        }

        Invoice invoice = paymentService.getInvoiceById(id);
        if (invoice == null || invoice.getBooking() == null || invoice.getBooking().getCustomer() == null
                || invoice.getBooking().getCustomer().getEmail() == null
                || !invoice.getBooking().getCustomer().getEmail().trim().equalsIgnoreCase(email.trim())) {
            return "redirect:/profile?error=unauthorized";
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "This invoice has already been paid.");
            return "redirect:/profile";
        }

        CreditCardPay cc = new CreditCardPay();
        cc.setBankName(bankName != null && !bankName.isBlank() ? bankName : "Card Payment");
        cc.setCardNo(cardNo);
        cc.setAmountPaid(invoice.getTotalAmt());
        cc.setPaymentDate(LocalDate.now());
        cc.setStatus("COMPLETED");
        cc.setRefNo("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cc.setInvoice(invoice);

        paymentService.processPayment(cc);

        invoice.setStatus("PAID");
        paymentService.updateInvoice(invoice.getInvoiceId(), invoice);

        redirectAttributes.addFlashAttribute("successMessage",
                "Payment of $" + invoice.getTotalAmt() + " processed successfully! Invoice #INV-" + id + " is now marked as PAID.");
        return "redirect:/profile";
    }
}
