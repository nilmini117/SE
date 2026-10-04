package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.users.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleApiController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired(required = false)
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
        if (staffRepository != null) {
            return staffRepository.findByEmail(authentication.getName()).isPresent();
        }
        return false;
    }

    @GetMapping
    public List<Map<String, Object>> getActiveVehicles() {
        return vehicleService.getVehiclesByBrand(null).stream().map(v -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vehicleId", v.getVehicleId());
            map.put("regNo", v.getRegNo());
            map.put("model", v.getModel());
            map.put("brand", v.getBrand());
            map.put("color", v.getColor());
            map.put("mileage", v.getMileage());
            map.put("status", v.getStatus());
            map.put("transmission", v.getTransmission());
            map.put("capacity", v.getCapacity());
            map.put("fuel", v.getFuel());
            map.put("dailyRate", v.getDailyRate());
            map.put("daily_rate", v.getDailyRate());
            map.put("imageUrl", v.getImageUrl());
            map.put("image", v.getImageUrl());
            map.put("isRegistered", v.getIsRegistered());
            map.put("isUnderMaintenance", v.isUnderMaintenance());
            map.put("serviceEndDate", v.getServiceEndDate());
            map.put("formattedServiceEndDate", v.getFormattedServiceEndDate());
            map.put("categoryBadge", v.getCategoryBadge());
            map.put("branchName", v.getBranch() != null ? v.getBranch().getBranchName() : "Main Branch");
            map.put("branchId", v.getBranch() != null ? v.getBranch().getBranchId() : null);
            return map;
        }).toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle, Authentication authentication) {
        if (authentication == null || !isStaff(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Access denied: Only staff members are permitted to modify vehicles."
            ));
        }
        try {
            Vehicle updated = vehicleService.updateVehicle(id, vehicle);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vehicle #" + id + " has been successfully updated.",
                    "vehicle", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to update vehicle #" + id + ": " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !isStaff(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Access denied: Only staff members are permitted to delete vehicles."
            ));
        }
        try {
            vehicleService.removeVehicle(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vehicle #VH-" + id + " has been successfully removed from fleet operations.",
                    "deletedId", id
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to delete vehicle #" + id + ": " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/reg/{regNo}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> deleteVehicleByRegNo(@PathVariable String regNo, Authentication authentication) {
        if (authentication == null || !isStaff(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Access denied: Only staff members are permitted to delete vehicles."
            ));
        }
        try {
            List<Vehicle> all = vehicleService.getAllVehicles();
            Vehicle vehicle = all.stream()
                    .filter(v -> v.getRegNo() != null && v.getRegNo().equalsIgnoreCase(regNo.trim()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with registration: " + regNo));

            vehicleService.removeVehicle(vehicle.getVehicleId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vehicle " + regNo + " has been successfully removed from fleet operations.",
                    "deletedId", vehicle.getVehicleId(),
                    "regNo", regNo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to delete vehicle with registration " + regNo + ": " + e.getMessage()
            ));
        }
    }
}
