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

import java.util.List;
import java.util.Map;

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
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "isStaff", required = false) Boolean isStaffParam,
            Model model,
            Authentication authentication) {
        boolean isStaff = (isStaffParam != null && isStaffParam) || isStaff(authentication);

        String cleanSearch = (search != null) ? search.trim() : "";
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : (isStaff ? "ALL" : "AVAILABLE");
        String cleanBrand = (brand != null && !brand.isBlank()) ? brand.trim() : null;

        // Customers can never view decommissioned fleet records
        if (!isStaff && "DECOMMISSIONED".equalsIgnoreCase(cleanStatus)) {
            cleanStatus = "AVAILABLE";
        }

        List<Vehicle> vehicles;
        if (cleanBrand != null && !cleanBrand.equalsIgnoreCase("ALL")) {
            if (isStaff) {
                vehicles = vehicleService.searchVehiclesWithBrand(cleanSearch, cleanStatus, cleanBrand);
            } else {
                // Customer view: only available vehicles under this brand
                vehicles = vehicleService.getAvailableVehiclesByBrand(cleanBrand);
            }
        } else {
            vehicles = vehicleService.searchVehicles(cleanSearch, cleanStatus);
        }

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("brands", vehicleService.getAllBrands());
        model.addAttribute("selectedBrand", cleanBrand);
        model.addAttribute("search", cleanSearch);
        model.addAttribute("status", cleanStatus);
        model.addAttribute("isStaff", isStaff);

        model.addAttribute("brandCounts", vehicleService.getBrandVehicleCounts());

        if (isStaff) {
            model.addAttribute("statusCounts", vehicleService.getVehicleStatusCounts());
        }

        return "vehicle/vehicle-list";
    }

    // JSON API endpoint for React / dynamic frontend to retrieve vehicles by brand
    @GetMapping("/api/brands")
    @ResponseBody
    public List<String> getAvailableBrands() {
        return vehicleService.getAllBrands();
    }

    @GetMapping("/api/brand-counts")
    @ResponseBody
    public Map<String, Long> getBrandCountsApi() {
        return vehicleService.getBrandVehicleCounts();
    }

    @GetMapping("/api/by-brand")
    @ResponseBody
    public List<Map<String, Object>> getVehiclesByBrandApi(
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "availableOnly", defaultValue = "false") boolean availableOnly) {
        List<Vehicle> vehicles = availableOnly
                ? vehicleService.getAvailableVehiclesByBrand(brand)
                : vehicleService.getVehiclesByBrand(brand);

        return vehicles.stream().map(v -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("vehicleId", v.getVehicleId());
            map.put("model", v.getModel());
            map.put("brand", v.getBrand());
            map.put("regNo", v.getRegNo());
            map.put("color", v.getColor());
            map.put("mileage", v.getMileage());
            map.put("status", v.getStatus());
            map.put("branchName", v.getBranch() != null ? v.getBranch().getBranchName() : "Main Branch");
            map.put("branchCity", v.getBranch() != null ? v.getBranch().getCity() : "Colombo");
            map.put("branchId", v.getBranch() != null ? v.getBranch().getBranchId() : null);
            map.put("bookingUrl", "/bookings/new?vehicleId=" + v.getVehicleId());
            return map;
        }).toList();
    }

    @PostMapping("/api/register")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> registerVehicleApi(@RequestBody VehicleRegistrationDto dto) {
        try {
            Vehicle saved = vehicleService.registerVehicle(dto);
            return org.springframework.http.ResponseEntity.ok(saved);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteVehicleApiRoot(@PathVariable Long id) {
        try {
            vehicleService.removeVehicle(id);
            return org.springframework.http.ResponseEntity.ok(Map.of("success", true, "deletedId", id));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteVehicleApi(@PathVariable Long id) {
        return deleteVehicleApiRoot(id);
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setQuantity(1);
        vehicle.setStatus("AVAILABLE");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("brands", vehicleService.getAllBrands());
        return "vehicle/vehicle-form";
    }

    @PostMapping
    public String registerVehicle(@ModelAttribute Vehicle vehicle,
                                  @RequestParam(value = "branchId", required = false) Long branchId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (vehicle.getBrand() == null || vehicle.getBrand().isBlank()) {
            model.addAttribute("errorMessage", "Vehicle Brand is mandatory. Please select from Toyota, Suzuki, Honda, Tesla, or Benz.");
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("brands", vehicleService.getAllBrands());
            return "vehicle/vehicle-form";
        }
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        Vehicle saved = vehicleService.registerVehicle(vehicle);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle " + saved.getModel() + " (" + saved.getBrand() + ") registered successfully.");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicleById(id));
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("brands", vehicleService.getAllBrands());
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
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle #VH-" + id + " has been deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete vehicle: " + e.getMessage());
        }
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/delete")
    public String deleteVehiclePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteVehicle(id, redirectAttributes);
    }
}
