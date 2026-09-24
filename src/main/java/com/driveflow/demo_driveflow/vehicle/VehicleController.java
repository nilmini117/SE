package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private StaffRepository staffRepository;

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
    public String listVehicles(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            Model model,
            Authentication authentication) {
        boolean isStaff = isStaff(authentication);

        String cleanSearch = (search != null) ? search.trim() : "";
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : (isStaff ? "ALL" : "AVAILABLE");

        // Customers can never view decommissioned fleet records
        if (!isStaff && "DECOMMISSIONED".equalsIgnoreCase(cleanStatus)) {
            cleanStatus = "AVAILABLE";
        }

        model.addAttribute("vehicles", vehicleService.searchVehicles(cleanSearch, cleanStatus));
        model.addAttribute("search", cleanSearch);
        model.addAttribute("status", cleanStatus);
        model.addAttribute("isStaff", isStaff);
        if (isStaff) {
            model.addAttribute("statusCounts", vehicleService.getVehicleStatusCounts());
        }
        return "vehicle/vehicle-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setQuantity(1);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("branches", branchRepository.findAll());
        return "vehicle/vehicle-form";
    }

    @PostMapping
    public String registerVehicle(@ModelAttribute Vehicle vehicle,
                                  @RequestParam(value = "branchId", required = false) Long branchId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (vehicle.getQuantity() == null || vehicle.getQuantity() < 1) {
            model.addAttribute("errorMessage", "Quantity must be at least 1.");
            model.addAttribute("branches", branchRepository.findAll());
            return "vehicle/vehicle-form";
        }
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        vehicleService.registerVehicle(vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle " + vehicle.getModel() + " registered successfully.");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicleById(id));
        model.addAttribute("branches", branchRepository.findAll());
        return "vehicle/vehicle-form";
    }

    @PostMapping("/{id}")
    public String updateVehicle(@PathVariable Long id,
                                @ModelAttribute Vehicle vehicle,
                                @RequestParam(value = "branchId", required = false) Long branchId,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (vehicle.getQuantity() == null || vehicle.getQuantity() < 1) {
            model.addAttribute("errorMessage", "Quantity must be at least 1.");
            model.addAttribute("branches", branchRepository.findAll());
            vehicle.setVehicleId(id);
            return "vehicle/vehicle-form";
        }
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        vehicleService.updateVehicle(id, vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle #" + id + " updated successfully.");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.removeVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle #VH-" + id + " has been decommissioned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to decommission vehicle: " + e.getMessage());
        }
        return "redirect:/vehicles";
    }
}
