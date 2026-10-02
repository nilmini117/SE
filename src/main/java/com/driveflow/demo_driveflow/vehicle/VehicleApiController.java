package com.driveflow.demo_driveflow.vehicle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleApiController {

    @Autowired
    private VehicleService vehicleService;

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
            map.put("branchName", v.getBranch() != null ? v.getBranch().getBranchName() : "Main Branch");
            map.put("branchId", v.getBranch() != null ? v.getBranch().getBranchId() : null);
            return map;
        }).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
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
    public ResponseEntity<?> deleteVehicleByRegNo(@PathVariable String regNo) {
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
