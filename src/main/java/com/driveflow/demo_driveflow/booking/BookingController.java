package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private com.driveflow.demo_driveflow.payment.InvoiceRepository invoiceRepository;

    private boolean isStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        boolean hasStaffRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"));
        if (hasStaffRole) {
            return true;
        }
        return staffRepository.findByEmail(authentication.getName()).isPresent();
    }

    @GetMapping
    public String listBookings(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "success", required = false) String success,
            Model model,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        boolean staff = isStaff(authentication);
        String email = authentication.getName();
        model.addAttribute("isStaff", staff);

        if (error != null && !model.containsAttribute("errorMessage")) {
            if ("unauthorized".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "You are not authorized to perform this booking action.");
            } else if ("cannot-cancel".equalsIgnoreCase(error)) {
                model.addAttribute("errorMessage", "Only pending bookings can be cancelled by customers.");
            } else {
                model.addAttribute("errorMessage", "Booking operation error: " + error);
            }
        }
        if (success != null && !model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", success);
        }

        List<Booking> bookingsList;
        if (staff) {
            bookingsList = bookingService.getAllBookings();
        } else {
            Optional<Customer> customerOpt = customerRepository.findByEmail(email);
            if (customerOpt.isPresent()) {
                bookingsList = bookingService.getBookingsByCustomer(customerOpt.get());
                model.addAttribute("currentCustomer", customerOpt.get());
            } else {
                bookingsList = List.of();
            }
        }
        model.addAttribute("bookings", bookingsList);

        java.util.Map<Long, com.driveflow.demo_driveflow.payment.Invoice> bookingInvoices = new java.util.HashMap<>();
        for (Booking b : bookingsList) {
            invoiceRepository.findByBooking(b).ifPresent(inv -> bookingInvoices.put(b.getBookingId(), inv));
        }
        model.addAttribute("bookingInvoices", bookingInvoices);

        return "booking/booking-list";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        // Staff cannot book vehicles — reserved for customers
        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff cannot book vehicles. Vehicle reservations are for customers only.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);

        Booking booking = new Booking();
        booking.setBookingDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setDuration(3);
        booking.setQuantity(1);
        booking.setChargedRate(new BigDecimal("150.00"));
        booking.setStatus("PENDING");

        if (vehicleId != null) {
            try {
                Vehicle v = vehicleService.getVehicleById(vehicleId);
                booking.setVehicle(v);
                model.addAttribute("selectedVehicle", v);
            } catch (Exception ignored) {}
        }

        if (customerOpt.isPresent()) {
            booking.setCustomer(customerOpt.get());
            model.addAttribute("currentCustomer", customerOpt.get());
        }

        model.addAttribute("isStaff", false);
        model.addAttribute("booking", booking);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "booking/booking-form";
    }

    @PostMapping
    public String createBooking(
            @ModelAttribute Booking booking,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        // Staff cannot book vehicles
        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff cannot book vehicles. Vehicle reservations are for customers only.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Optional<Customer> currentCustomerOpt = customerRepository.findByEmail(email);

        if (currentCustomerOpt.isPresent()) {
            booking.setCustomer(currentCustomerOpt.get());
        }

        if (booking.getCustomer() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "A registered customer account is required to place a reservation.");
            return "redirect:/bookings/new";
        }

        if (vehicleId != null) {
            try {
                Vehicle v = vehicleService.getVehicleById(vehicleId);
                booking.setVehicle(v);
            } catch (Exception ignored) {}
        }

        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDate.now());
        }
        if (booking.getEndDate() == null) {
            booking.setEndDate(booking.getBookingDate().plusDays(1));
        }
        if (booking.getDuration() == null || booking.getDuration() <= 0) {
            long days = ChronoUnit.DAYS.between(booking.getBookingDate(), booking.getEndDate());
            booking.setDuration(days > 0 ? (int) days : 1);
        }
        if (booking.getQuantity() == null) {
            booking.setQuantity(1);
        }
        if (booking.getChargedRate() == null) {
            booking.setChargedRate(java.math.BigDecimal.valueOf(75.00));
        }

        // Server-side status enforcement: Customers always create PENDING bookings
        booking.setStatus("PENDING");

        if (booking.getPickupBranch() == null) {
            branchRepository.findAll().stream().findFirst().ifPresent(booking::setPickupBranch);
        }
        if (booking.getReturnBranch() == null) {
            booking.setReturnBranch(booking.getPickupBranch());
        }

        bookingService.createBooking(booking);
        redirectAttributes.addFlashAttribute("successMessage",
                "Vehicle reservation submitted successfully! Status is PENDING staff approval.");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        boolean staff = isStaff(authentication);
        String email = authentication.getName();
        model.addAttribute("isStaff", staff);

        Booking booking = bookingService.getBookingById(id);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (!staff && (booking.getCustomer() == null || !booking.getCustomer().getEmail().equalsIgnoreCase(email))) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this booking.");
            return "redirect:/bookings";
        }

        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        customerOpt.ifPresent(c -> model.addAttribute("currentCustomer", c));

        model.addAttribute("booking", booking);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("customers", customerRepository.findAll());
        return "booking/booking-form";
    }

    @PostMapping("/{id}")
    public String updateBooking(
            @PathVariable Long id,
            @ModelAttribute Booking booking,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "customerId", required = false) Long customerId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        boolean staff = isStaff(authentication);
        String email = authentication.getName();

        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (!staff && (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email))) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this booking.");
            return "redirect:/bookings";
        }

        if (vehicleId != null) {
            try {
                Vehicle v = vehicleService.getVehicleById(vehicleId);
                booking.setVehicle(v);
            } catch (Exception ignored) {}
        }
        if (staff && customerId != null) {
            customerRepository.findById(customerId).ifPresent(booking::setCustomer);
        } else {
            booking.setCustomer(existing.getCustomer());
        }

        // Server-side guard: Customer cannot flip status on edit — remains PENDING
        if (!staff) {
            booking.setStatus("PENDING");
        } else if (booking.getStatus() == null || booking.getStatus().isBlank()) {
            booking.setStatus(existing.getStatus());
        }

        // If staff changes status to CANCELLED in edit form, run cancel logic
        if (staff && "CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " has been cancelled.");
            return "redirect:/bookings";
        }

        bookingService.updateBooking(id, booking);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " updated successfully.");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/approve")
    public String approveBooking(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only staff can approve reservations.");
            return "redirect:/bookings";
        }

        bookingService.approveBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " has been approved successfully.");
        return "redirect:/bookings";
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public String cancelBooking(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        boolean staff = isStaff(authentication);
        String email = authentication.getName();

        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (!staff && (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email))) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to cancel this booking.");
            return "redirect:/bookings";
        }
        // Customers can only cancel pending bookings
        if (!staff && !"PENDING".equalsIgnoreCase(existing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending bookings can be cancelled by customers.");
            return "redirect:/bookings";
        }

        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " has been successfully cancelled.");
        return "redirect:/bookings";
    }
}
