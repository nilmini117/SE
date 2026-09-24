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

    // --- CUSTOMER INCIDENT REPORTING ---

    @GetMapping("/report")
    public String showCustomerReportForm(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        com.driveflow.demo_driveflow.users.Customer customer = userService.findCustomerByEmail(email).orElse(null);
        if (customer == null) {
            return "redirect:/incidents/new";
        }

        Incident incident = new Incident();
        incident.setStatus("OPEN");
        incident.setDate(java.time.LocalDate.now());
        incident.setCustomer(customer);

        java.util.List<com.driveflow.demo_driveflow.booking.Booking> bookings = bookingService.getBookingsByCustomer(customer);
        java.util.List<com.driveflow.demo_driveflow.vehicle.Vehicle> customerVehicles = bookings.stream()
                .map(com.driveflow.demo_driveflow.booking.Booking::getVehicle)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (customerVehicles.isEmpty()) {
            customerVehicles = vehicleRepository.findAll();
        }

        model.addAttribute("incident", incident);
        model.addAttribute("customer", customer);
        model.addAttribute("vehicles", customerVehicles);
        return "incident/incident-report";
    }

    @PostMapping("/report")
    public String submitCustomerReport(
            @ModelAttribute Incident incident,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        com.driveflow.demo_driveflow.users.Customer customer = userService.findCustomerByEmail(email).orElse(null);
        if (customer != null) {
            incident.setCustomer(customer);
        }
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(incident::setVehicle);
        }
        if (incident.getDate() == null) {
            incident.setDate(java.time.LocalDate.now());
        }
        incident.setStatus("OPEN");
        incidentService.logIncident(incident);
        redirectAttributes.addFlashAttribute("successMessage", "Incident report #INC-" + incident.getIncidentId() + " submitted successfully. Our safety & support team has been notified.");
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
    public String showCreateForm(Model model) {
        Incident incident = new Incident();
        incident.setStatus("OPEN");
        incident.setDate(java.time.LocalDate.now());
        model.addAttribute("incident", incident);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "incident/incident-form";
    }

    @PostMapping
    public String logIncident(
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
        incidentService.logIncident(incident);
        redirectAttributes.addFlashAttribute("successMessage", "Incident report #INC-" + incident.getIncidentId() + " logged successfully.");
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
