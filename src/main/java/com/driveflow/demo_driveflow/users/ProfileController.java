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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
                                @RequestParam(value = "contactNumber", required = false) String contactNumber,
                                @RequestParam(value = "contactNumbers", required = false) java.util.List<String> contactNumbers,
                                @RequestParam("drivingLicense") String drivingLicense,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        try {
            userService.updateCustomerProfile(email, firstName, lastName, contactNumber, contactNumbers, drivingLicense);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile/edit";
        }

        return "redirect:/profile";
    }

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.otp.OtpService otpService;

    @GetMapping("/password")
    public String showChangePasswordForm(Authentication authentication, Model model) {
        if (authentication == null) {
            authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        model.addAttribute("email", authentication.getName());
        return "profile/password-change";
    }

    @PostMapping("/password/send-otp")
    public String sendPasswordChangeOtp(Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        if (otpService != null) {
            otpService.generatePasswordOtp(email);
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "A 6-digit OTP verification code has been sent to " + email + ". It is valid for 10 minutes.");
        return "redirect:/profile/password";
    }

    @PostMapping(value = "/api/profile/password/send-otp", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendPasswordChangeOtpApi(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        if (authentication == null) {
            authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        }
        String email = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            email = authentication.getName();
        } else if (body != null && body.containsKey("email")) {
            email = body.get("email");
        }

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "success", false,
                    "message", "Unauthorized or session expired. Please sign in again."));
        }

        String trimmedEmail = email.trim();
        if (otpService != null) {
            otpService.generatePasswordOtp(trimmedEmail);
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("message", "A 6-digit OTP verification code has been dispatched to " + trimmedEmail + " (valid for 10 minutes).");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 @RequestParam(value = "otp", required = false) String otp,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirm password do not match.");
            return "redirect:/profile/password";
        }

        if (otp == null || otp.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "6-digit OTP verification code is required.");
            return "redirect:/profile/password";
        }

        try {
            userService.changePassword(authentication.getName(), oldPassword, newPassword, otp.trim());
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile/password";
        }

        return "redirect:/profile";
    }

    @PostMapping(value = "/password/api", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePasswordApi(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String email = (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken))
                ? authentication.getName()
                : (body != null ? body.get("email") : null);

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", 401, "success", false, "message", "Unauthorized"));
        }
        String oldPassword = body != null ? body.get("oldPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        String otp = body != null ? body.get("otp") : null;

        if (otpService != null) {
            if (otp == null || (!otpService.verifyPasswordOtp(email, otp) && !otpService.verifyOtp(email, otp))) {
                return ResponseEntity.badRequest().body(Map.of("status", 400, "success", false, "message", "Invalid or expired OTP. Password change blocked."));
            }
        }

        try {
            userService.changePassword(email, oldPassword, newPassword, otp);
            return ResponseEntity.ok(Map.of("status", 200, "success", true, "message", "Password updated successfully!"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "success", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/invoices/{id}/pay")
    public String showPayInvoiceForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
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
            redirectAttributes.addFlashAttribute("errorMessage", "Invoice not found or you are not authorized to view it.");
            return "redirect:/profile";
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            redirectAttributes.addFlashAttribute("infoMessage", "Invoice #INV-" + id + " has already been paid.");
            return "redirect:/profile";
        }

        // Customer cannot pay for the booking until staff has confirmed/approved the booking
        Booking booking = invoice.getBooking();
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) && !"APPROVED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Payment cannot be processed yet. Booking #BK-" + booking.getBookingId()
                    + " is currently " + booking.getStatus() + " and must be approved by staff before payment can be accepted.");
            return "redirect:/profile";
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
            @RequestParam(value = "expiry", required = false) String expiry,
            @RequestParam(value = "cvv", required = false) String cvv,
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
            redirectAttributes.addFlashAttribute("errorMessage", "Invoice not found or you are not authorized to pay it.");
            return "redirect:/profile";
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "This invoice has already been paid.");
            return "redirect:/profile";
        }

        // Customer cannot pay for the booking until staff has confirmed/approved the booking
        Booking booking = invoice.getBooking();
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) && !"APPROVED".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Payment rejected. Booking #BK-" + booking.getBookingId()
                    + " has not been approved by staff (current status: " + booking.getStatus() + ").");
            return "redirect:/profile";
        }

        // Validate expiry date: strictly 12 months (01-12)
        if (expiry != null && !expiry.isBlank()) {
            if (!expiry.trim().matches("^(0[1-9]|1[0-2])\\/\\d{2}$")) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid Expiration Date: Month must be between 01 and 12 in MM/YY format (e.g. 12/28).");
                return "redirect:/profile/invoices/" + id + "/pay";
            }
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
                "Payment of Rs. " + invoice.getTotalAmt() + " processed successfully! Invoice #INV-" + id + " is now marked as PAID.");
        return "redirect:/profile";
    }
}
