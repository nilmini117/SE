package com.driveflow.demo_driveflow.users;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

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

        model.addAttribute("customer", customer);
        model.addAttribute("bookings", customerBookings);
        model.addAttribute("invoices", customerInvoices);
        model.addAttribute("payments", customerPayments);
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
}
