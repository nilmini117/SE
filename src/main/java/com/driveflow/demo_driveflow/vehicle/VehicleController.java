package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private com.driveflow.demo_driveflow.feedback.FeedbackService feedbackService;

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
        boolean isStaff = isStaff(authentication);

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
        model.addAttribute("publicFeedbackList", feedbackService.getPubliclyVisibleFeedback());

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
            map.put("imageUrl", v.getImageUrl());
            map.put("image", v.getImageUrl());
            map.put("isRegistered", v.getIsRegistered());
            map.put("isUnderMaintenance", v.isUnderMaintenance());
            map.put("serviceEndDate", v.getServiceEndDate());
            map.put("formattedServiceEndDate", v.getFormattedServiceEndDate());
            map.put("dailyRate", v.getDailyRate());
            map.put("daily_rate", v.getDailyRate());
            map.put("transmission", v.getTransmission());
            map.put("capacity", v.getCapacity());
            map.put("fuel", v.getFuel());
            map.put("branchName", v.getBranch() != null ? v.getBranch().getBranchName() : "Main Branch");
            map.put("branchCity", v.getBranch() != null ? v.getBranch().getCity() : "Colombo");
            map.put("branchId", v.getBranch() != null ? v.getBranch().getBranchId() : null);
            map.put("bookingUrl", "/bookings/new?vehicleId=" + v.getVehicleId());
            return map;
        }).toList();
    }

    @PostMapping(value = "/api/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> registerVehicleApiMultipart(
            @ModelAttribute VehicleRegistrationDto dto,
            @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile) {
        try {
            org.springframework.web.multipart.MultipartFile upload = image != null ? image : (file != null ? file : imageFile);
            Vehicle saved = vehicleService.registerVehicle(dto, upload);
            return org.springframework.http.ResponseEntity.ok(saved);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/register")
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
    @PreAuthorize("hasRole('STAFF')")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteVehicleApiRoot(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !isStaff(authentication)) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Access denied: Only staff members are permitted to delete vehicles."
            ));
        }
        try {
            vehicleService.removeVehicle(id);
            return org.springframework.http.ResponseEntity.ok(Map.of("success", true, "deletedId", id));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasRole('STAFF')")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteVehicleApi(@PathVariable Long id, Authentication authentication) {
        return deleteVehicleApiRoot(id, authentication);
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('STAFF')")
    public String showCreateForm(Model model, Authentication authentication) {
        if (!isStaff(authentication)) {
            return "redirect:/vehicles?error=unauthorized";
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setQuantity(1);
        vehicle.setStatus("AVAILABLE");
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("brands", vehicleService.getAllBrands());
        return "vehicle/vehicle-form";
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF')")
    public String registerVehicle(@ModelAttribute Vehicle vehicle,
                                  @RequestParam(value = "branchId", required = false) Long branchId,
                                  @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                                  @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
                                  Model model,
                                  RedirectAttributes redirectAttributes,
                                  Authentication authentication) {
        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: Staff role required.");
            return "redirect:/vehicles";
        }
        if (vehicle.getBrand() == null || vehicle.getBrand().isBlank()) {
            model.addAttribute("errorMessage", "Vehicle Brand is mandatory. Please select from Toyota, Suzuki, Honda, Tesla, or Benz.");
            model.addAttribute("branches", branchRepository.findAll());
            model.addAttribute("brands", vehicleService.getAllBrands());
            return "vehicle/vehicle-form";
        }
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        org.springframework.web.multipart.MultipartFile upload = imageFile != null ? imageFile : file;
        Vehicle saved = vehicleService.registerVehicle(vehicle, upload);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle " + saved.getModel() + " (" + saved.getBrand() + ") registered successfully.");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('STAFF')")
    public String showEditForm(@PathVariable Long id, Model model, Authentication authentication) {
        if (!isStaff(authentication)) {
            return "redirect:/vehicles?error=unauthorized";
        }
        model.addAttribute("vehicle", vehicleService.getVehicleById(id));
        model.addAttribute("branches", branchRepository.findAll());
        model.addAttribute("brands", vehicleService.getAllBrands());
        return "vehicle/vehicle-form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public String updateVehicle(@PathVariable Long id,
                                @ModelAttribute Vehicle vehicle,
                                @RequestParam(value = "branchId", required = false) Long branchId,
                                @RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                                @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
                                Model model,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: Staff role required.");
            return "redirect:/vehicles";
        }
        if (vehicle.getQuantity() == null || vehicle.getQuantity() < 1) {
            model.addAttribute("errorMessage", "Quantity must be at least 1.");
            model.addAttribute("branches", branchRepository.findAll());
            vehicle.setVehicleId(id);
            return "vehicle/vehicle-form";
        }
        if (branchId != null) {
            branchRepository.findById(branchId).ifPresent(vehicle::setBranch);
        }
        org.springframework.web.multipart.MultipartFile upload = imageFile != null ? imageFile : file;
        vehicleService.updateVehicle(id, vehicle, upload);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle #" + id + " updated successfully.");
        return "redirect:/vehicles";
    }

    @GetMapping("/{id}/delete")
    @PreAuthorize("hasRole('STAFF')")
    public String deleteVehicle(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (!isStaff(authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied: Staff role required.");
            return "redirect:/vehicles";
        }
        try {
            vehicleService.removeVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle #VH-" + id + " has been deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete vehicle: " + e.getMessage());
        }
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('STAFF')")
    public String deleteVehiclePost(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        return deleteVehicle(id, redirectAttributes, authentication);
    }
}
