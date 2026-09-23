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

    @GetMapping
    public String listBookings(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        String email = authentication.getName();
        boolean isStaff = staffRepository.findByEmail(email).isPresent();
        model.addAttribute("isStaff", isStaff);

        List<Booking> bookingsList;
        if (isStaff) {
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
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        String email = authentication.getName();
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        boolean isStaff = staffRepository.findByEmail(email).isPresent();

        Booking booking = new Booking();
        booking.setBookingDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setDuration(3);
        booking.setQuantity(1);
        booking.setChargedRate(new BigDecimal("150.00"));
        booking.setStatus(isStaff ? "CONFIRMED" : "PENDING");

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
        } else if (isStaff) {
            model.addAttribute("customers", customerRepository.findAll());
        }

        model.addAttribute("isStaff", isStaff);
        model.addAttribute("booking", booking);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "booking/booking-form";
    }

    @PostMapping
    public String createBooking(
            @ModelAttribute Booking booking,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "customerId", required = false) Long customerId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        String email = authentication.getName();
        Optional<Customer> currentCustomerOpt = customerRepository.findByEmail(email);
        boolean isStaff = staffRepository.findByEmail(email).isPresent();

        if (currentCustomerOpt.isPresent()) {
            // Logged in as customer: customer is always themselves
            booking.setCustomer(currentCustomerOpt.get());
        } else if (customerId != null) {
            // Staff booking on behalf of a specific customer
            customerRepository.findById(customerId).ifPresent(booking::setCustomer);
        }

        if (booking.getCustomer() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "A registered customer must be assigned to this booking.");
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
        if (!isStaff) {
            booking.setStatus("PENDING");
        } else if (booking.getStatus() == null || booking.getStatus().isBlank()) {
            booking.setStatus("CONFIRMED");
        }

        if (booking.getPickupBranch() == null) {
            branchRepository.findAll().stream().findFirst().ifPresent(booking::setPickupBranch);
        }
        if (booking.getReturnBranch() == null) {
            booking.setReturnBranch(booking.getPickupBranch());
        }

        bookingService.createBooking(booking);
        redirectAttributes.addFlashAttribute("successMessage",
                isStaff ? "Vehicle reservation confirmed successfully!" : "Vehicle reservation submitted successfully! Status is PENDING staff approval.");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login?bookingRequired=true";
        }

        String email = authentication.getName();
        boolean isStaff = staffRepository.findByEmail(email).isPresent();
        model.addAttribute("isStaff", isStaff);

        Booking booking = bookingService.getBookingById(id);
        if (!isStaff && (booking.getCustomer() == null || !booking.getCustomer().getEmail().equalsIgnoreCase(email))) {
            return "redirect:/bookings?error=unauthorized";
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
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        boolean isStaff = staffRepository.findByEmail(email).isPresent();

        Booking existing = bookingService.getBookingById(id);
        if (!isStaff && (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email))) {
            return "redirect:/bookings?error=unauthorized";
        }

        if (vehicleId != null) {
            try {
                Vehicle v = vehicleService.getVehicleById(vehicleId);
                booking.setVehicle(v);
            } catch (Exception ignored) {}
        }
        if (isStaff && customerId != null) {
            customerRepository.findById(customerId).ifPresent(booking::setCustomer);
        } else {
            booking.setCustomer(existing.getCustomer());
        }

        // Server-side guard: Customer cannot flip status on edit — remains PENDING
        if (!isStaff) {
            booking.setStatus("PENDING");
        } else if (booking.getStatus() == null || booking.getStatus().isBlank()) {
            booking.setStatus(existing.getStatus());
        }

        bookingService.updateBooking(id, booking);
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/approve")
    public String approveBooking(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        boolean isStaff = staffRepository.findByEmail(email).isPresent();
        if (!isStaff) {
            return "redirect:/bookings?error=unauthorized";
        }

        bookingService.approveBooking(id);
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        boolean isStaff = staffRepository.findByEmail(email).isPresent();

        Booking existing = bookingService.getBookingById(id);
        if (!isStaff && (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email))) {
            return "redirect:/bookings?error=unauthorized";
        }
        // Customers can only cancel pending bookings
        if (!isStaff && !"PENDING".equalsIgnoreCase(existing.getStatus())) {
            return "redirect:/bookings?error=cannot-cancel";
        }

        bookingService.cancelBooking(id);
        return "redirect:/bookings";
    }
}
