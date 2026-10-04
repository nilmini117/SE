package com.driveflow.demo_driveflow.incident;

import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private com.driveflow.demo_driveflow.users.UserService userService;

    @Autowired
    private com.driveflow.demo_driveflow.booking.BookingService bookingService;

    @Autowired
    private com.driveflow.demo_driveflow.booking.BookingRepository bookingRepository;

    // --- CUSTOMER SUPPORT ("Need to contact us") ---

    @GetMapping("/report")
    public String showCustomerReportForm(Model model, org.springframework.security.core.Authentication authentication) {
        com.driveflow.demo_driveflow.users.Customer customer = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String email = authentication.getName();
            customer = userService.findCustomerByEmail(email).orElse(null);
        }

        Incident incident = new Incident();
        incident.setStatus("OPEN");
        incident.setDate(java.time.LocalDate.now());

        com.driveflow.demo_driveflow.vehicle.Vehicle activeVehicle = null;
        com.driveflow.demo_driveflow.booking.Booking activeBooking = null;

        if (customer != null) {
            incident.setCustomer(customer);
            // Auto-fetch customer's currently active booking vehicle (Strict
            // single-active-booking rule)
            java.util.List<com.driveflow.demo_driveflow.booking.Booking> activeBookings = bookingRepository
                    .findActiveBookingsByCustomerId(customer.getSystemId());

            if (!activeBookings.isEmpty()) {
                activeBooking = activeBookings.get(0);
                activeVehicle = activeBooking.getVehicle();
            } else {
                // Fallback to most recent booking if available
                java.util.List<com.driveflow.demo_driveflow.booking.Booking> recentBookings = bookingRepository
                        .findByCustomerIdSorted(customer.getSystemId());
                if (!recentBookings.isEmpty()) {
                    activeBooking = recentBookings.get(0);
                    activeVehicle = activeBooking.getVehicle();
                }
            }
        }

        if (activeVehicle != null) {
            incident.setVehicle(activeVehicle);
        }

        model.addAttribute("incident", incident);
        model.addAttribute("customer", customer);
        model.addAttribute("activeVehicle", activeVehicle);
        model.addAttribute("activeBooking", activeBooking);
        return "incident/incident-report";
    }

    @GetMapping("/api/active-vehicle")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> getActiveVehicle(
            org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Unauthorized"));
        }
        String email = authentication.getName();
        com.driveflow.demo_driveflow.users.Customer customer = userService.findCustomerByEmail(email).orElse(null);
        if (customer == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Customer record not found"));
        }

        java.util.List<com.driveflow.demo_driveflow.booking.Booking> activeBookings = bookingRepository
                .findActiveBookingsByCustomerId(customer.getSystemId());
        com.driveflow.demo_driveflow.vehicle.Vehicle activeVehicle = null;
        Long bookingId = null;

        if (!activeBookings.isEmpty()) {
            com.driveflow.demo_driveflow.booking.Booking b = activeBookings.get(0);
            activeVehicle = b.getVehicle();
            bookingId = b.getBookingId();
        } else {
            java.util.List<com.driveflow.demo_driveflow.booking.Booking> recentBookings = bookingRepository
                    .findByCustomerIdSorted(customer.getSystemId());
            if (!recentBookings.isEmpty()) {
                com.driveflow.demo_driveflow.booking.Booking b = recentBookings.get(0);
                activeVehicle = b.getVehicle();
                bookingId = b.getBookingId();
            }
        }

        if (activeVehicle == null) {
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("hasActiveBooking", false));
        }

        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "hasActiveBooking", true,
                "bookingId", bookingId != null ? bookingId : 0L,
                "vehicleId", activeVehicle.getVehicleId(),
                "regNo", activeVehicle.getRegNo() != null ? activeVehicle.getRegNo() : "",
                "brand", activeVehicle.getBrand() != null ? activeVehicle.getBrand() : "",
                "model", activeVehicle.getModel() != null ? activeVehicle.getModel() : ""));
    }

    @PostMapping("/report")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public String submitCustomerReport(
            @ModelAttribute Incident incident,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"));
        if (isStaff) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Access Denied: Staff members cannot write or submit incidents.");
            return "redirect:/incidents";
        }
        String email = authentication.getName();
        com.driveflow.demo_driveflow.users.Customer customer = userService.findCustomerByEmail(email).orElse(null);
        if (customer != null) {
            incident.setCustomer(customer);
            // If vehicle is not yet set, automatically bind the active booking vehicle
            if (vehicleId == null && incident.getVehicle() == null) {
                java.util.List<com.driveflow.demo_driveflow.booking.Booking> activeBookings = bookingRepository
                        .findActiveBookingsByCustomerId(customer.getSystemId());
                if (!activeBookings.isEmpty() && activeBookings.get(0).getVehicle() != null) {
                    incident.setVehicle(activeBookings.get(0).getVehicle());
                } else {
                    java.util.List<com.driveflow.demo_driveflow.booking.Booking> recentBookings = bookingRepository
                            .findByCustomerIdSorted(customer.getSystemId());
                    if (!recentBookings.isEmpty() && recentBookings.get(0).getVehicle() != null) {
                        incident.setVehicle(recentBookings.get(0).getVehicle());
                    }
                }
            }
        }
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(incident::setVehicle);
        }
        if (incident.getDate() == null) {
            incident.setDate(java.time.LocalDate.now());
        }
        incident.setStatus("OPEN");
        incidentService.logIncident(incident);
        redirectAttributes.addFlashAttribute("successMessage", "Support request #INC-" + incident.getIncidentId()
                + " submitted successfully. Our safety & support team has been notified.");
        return "redirect:/profile#incidents";
    }

    @GetMapping
    public String listIncidents(
            @RequestParam(value = "status", required = false) String status,
            Model model) {
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";
        model.addAttribute("incidents", incidentService.searchIncidents(cleanStatus));
        model.addAttribute("selectedStatus", cleanStatus);
        model.addAttribute("statusCounts", incidentService.getIncidentStatusCounts());
        return "incident/incident-list";
    }

    @GetMapping("/new")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public String showCreateForm(Model model, org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Access Denied: Staff members cannot write or submit new incidents.");
            return "redirect:/incidents";
        }
        Incident incident = new Incident();
        incident.setStatus("OPEN");
        incident.setDate(java.time.LocalDate.now());
        model.addAttribute("incident", incident);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "incident/incident-form";
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public String logIncident(
            @ModelAttribute Incident incident,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"))) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Access Denied: Staff members cannot write or submit new incidents.");
            return "redirect:/incidents";
        }
        if (customerId != null) {
            customerRepository.findById(customerId).ifPresent(incident::setCustomer);
        }
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(incident::setVehicle);
        }
        incidentService.logIncident(incident);
        redirectAttributes.addFlashAttribute("successMessage",
                "Incident report #INC-" + incident.getIncidentId() + " logged successfully.");
        return "redirect:/incidents";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("incident", incidentService.getIncidentById(id));
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "incident/incident-form";
    }

    @PostMapping("/{id}")
    public String updateIncident(
            @PathVariable Long id,
            @ModelAttribute Incident incident,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            RedirectAttributes redirectAttributes) {
        if (customerId != null) {
            customerRepository.findById(customerId).ifPresent(incident::setCustomer);
        }
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(incident::setVehicle);
        }
        incidentService.updateIncident(id, incident);
        redirectAttributes.addFlashAttribute("successMessage", "Incident #INC-" + id + " updated successfully.");
        return "redirect:/incidents";
    }

    @PostMapping("/{id}/status")
    public String updateIncidentStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "staffMessage", required = false) String staffMessage,
            RedirectAttributes redirectAttributes) {
        try {
            incidentService.updateIncidentStatus(id, status, staffMessage);
            String label = "RESOLVED".equalsIgnoreCase(status) ? "Resolved" : "In Progress";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Incident #INC-" + id + " marked as " + label + " and notification message saved for customer.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating incident: " + ex.getMessage());
        }
        return "redirect:/incidents";
    }

    @GetMapping("/{id}/delete")
    public String deleteIncident(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            incidentService.removeIncident(id);
            redirectAttributes.addFlashAttribute("successMessage", "Incident report #INC-" + id + " deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting incident: " + ex.getMessage());
        }
        return "redirect:/incidents";
    }
}
