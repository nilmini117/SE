package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BranchRepository branchRepository;

    @GetMapping
    public String listVehicles(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            Model model,
            Authentication authentication) {
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

        if (status == null || status.isBlank()) {
            status = isStaff ? "ALL" : "AVAILABLE";
        }

        model.addAttribute("vehicles", vehicleService.searchVehicles(search, status));
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("status", status);
        model.addAttribute("isStaff", isStaff);
        return "vehicle/vehicle-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        model.addAttribute("branches", branchRepository.findAll());
        return "vehicle/vehicle-form";
    }

    @PostMapping
    public String registerVehicle(@ModelAttribute Vehicle vehicle,
                                  @RequestParam(value = "branchId", required = false) Long branchId) {
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        vehicleService.registerVehicle(vehicle);
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
                                @RequestParam(value = "branchId", required = false) Long branchId) {
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        vehicleService.updateVehicle(id, vehicle);
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id) {
        vehicleService.removeVehicle(id);
        return "redirect:/vehicles";
    }
}
