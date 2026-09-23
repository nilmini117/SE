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
            @RequestParam(value = "status", required = false) String status,
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
            // Default status filter to PENDING on load for staff
            String activeStatus = (status == null || status.isBlank()) ? "PENDING" : status.trim().toUpperCase();
            model.addAttribute("selectedStatus", activeStatus);
            model.addAttribute("statusCounts", bookingService.getBookingStatusCounts());
            bookingsList = bookingService.getBookingsByStatus(activeStatus);
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
        booking.setChargedRate(new BigDecimal("9000.00")); // 3 days * Rs. 3,000.00/day
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
        if (booking.getEndDate().isBefore(booking.getBookingDate())) {
            booking.setEndDate(booking.getBookingDate());
        }

        long days = ChronoUnit.DAYS.between(booking.getBookingDate(), booking.getEndDate());
        int durationDays = days > 0 ? (int) days : 1;
        booking.setDuration(durationDays);
        booking.setQuantity(1); // Enforce only one vehicle per booking
        booking.setChargedRate(BigDecimal.valueOf(durationDays * 3000.00)); // Rs. 3,000 per day

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

        // Staff cannot edit booking records directly
        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff cannot directly edit customer booking details. Use Approve or Decline instead.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Booking booking = bookingService.getBookingById(id);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (booking.getCustomer() == null || !booking.getCustomer().getEmail().equalsIgnoreCase(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this booking.");
            return "redirect:/bookings";
        }

        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending bookings can be modified.");
            return "redirect:/bookings";
        }

        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        customerOpt.ifPresent(c -> model.addAttribute("currentCustomer", c));

        model.addAttribute("isStaff", false);
        model.addAttribute("booking", booking);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "booking/booking-form";
    }

    @PostMapping("/{id}")
    public String updateBooking(
            @PathVariable Long id,
            @ModelAttribute Booking booking,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        // Staff cannot edit booking records directly
        if (isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff cannot directly edit customer booking details.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this booking.");
            return "redirect:/bookings";
        }

        if (!"PENDING".equalsIgnoreCase(existing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending bookings can be modified.");
            return "redirect:/bookings";
        }

        if (vehicleId != null) {
            try {
                Vehicle v = vehicleService.getVehicleById(vehicleId);
                booking.setVehicle(v);
            } catch (Exception ignored) {}
        }

        if (booking.getBookingDate() == null) {
            booking.setBookingDate(existing.getBookingDate() != null ? existing.getBookingDate() : LocalDate.now());
        }
        if (booking.getEndDate() == null) {
            booking.setEndDate(booking.getBookingDate().plusDays(1));
        }
        if (booking.getEndDate().isBefore(booking.getBookingDate())) {
            booking.setEndDate(booking.getBookingDate());
        }
        long days = ChronoUnit.DAYS.between(booking.getBookingDate(), booking.getEndDate());
        int durationDays = days > 0 ? (int) days : 1;
        booking.setDuration(durationDays);
        booking.setQuantity(1);
        booking.setChargedRate(BigDecimal.valueOf(durationDays * 3000.00));

        booking.setCustomer(existing.getCustomer());
        booking.setStatus("PENDING");

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
        redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " has been approved successfully and confirmation sent to customer.");
        return "redirect:/bookings?status=PENDING";
    }

    @PostMapping("/{id}/decline")
    public String declineBooking(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only staff can decline reservations.");
            return "redirect:/bookings";
        }

        if (reason == null || reason.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "A decline reason is required so the customer can be informed.");
            return "redirect:/bookings?status=PENDING";
        }

        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        bookingService.declineBooking(id, reason.trim());
        redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " has been declined and customer notified with reason: \"" + reason.trim() + "\"");
        return "redirect:/bookings?status=PENDING";
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.GET, RequestMethod.POST})
    public String cancelBooking(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        boolean staff = isStaff(authentication);
        if (staff) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff must provide a reason to decline or cancel a booking. Please use the Decline action.");
            return "redirect:/bookings";
        }

        String email = authentication.getName();
        Booking existing = bookingService.getBookingById(id);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
            return "redirect:/bookings";
        }

        if (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to cancel this booking.");
            return "redirect:/bookings";
        }
        // Customers can only cancel pending bookings
        if (!"PENDING".equalsIgnoreCase(existing.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending bookings can be cancelled by customers.");
            return "redirect:/bookings";
        }

        bookingService.cancelBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Booking #BK-" + id + " has been successfully cancelled.");
        return "redirect:/bookings";
    }
}
