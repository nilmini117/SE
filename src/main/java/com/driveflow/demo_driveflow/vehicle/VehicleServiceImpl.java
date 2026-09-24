package com.driveflow.demo_driveflow.vehicle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    public List<Vehicle> searchVehicles(String search, String status) {
        String cleanSearch = (search != null) ? search.trim() : "";
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";
        return vehicleRepository.searchVehicles(cleanSearch, cleanStatus);
    }

    @Override
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
    }

    @Override
    public Vehicle registerVehicle(Vehicle vehicle) {
        if (vehicle.getQuantity() == null || vehicle.getQuantity() < 1) {
            vehicle.setQuantity(1);
        }
        if (vehicle.getStatus() == null || vehicle.getStatus().isBlank()) {
            vehicle.setStatus("AVAILABLE");
        }
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle updateVehicle(Long id, Vehicle updatedVehicle) {
        Vehicle existing = getVehicleById(id);
        existing.setModel(updatedVehicle.getModel());
        existing.setColor(updatedVehicle.getColor());
        existing.setMileage(updatedVehicle.getMileage());
        existing.setStatus(updatedVehicle.getStatus());
        existing.setRegNo(updatedVehicle.getRegNo());
        int qty = (updatedVehicle.getQuantity() != null && updatedVehicle.getQuantity() >= 1) ? updatedVehicle.getQuantity() : 1;
        existing.setQuantity(qty);
        if (updatedVehicle.getBranch() != null) {
            existing.setBranch(updatedVehicle.getBranch());
        }
        return vehicleRepository.save(existing);
    }

    @Override
    public void removeVehicle(Long id) {
        Vehicle existing = getVehicleById(id);
        existing.setStatus("DECOMMISSIONED");
        vehicleRepository.save(existing);
    }

    @Override
    public java.util.Map<String, Long> getVehicleStatusCounts() {
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        long allActive = vehicleRepository.countByStatusNotIgnoreCase("DECOMMISSIONED");
        long available = vehicleRepository.countByStatusIgnoreCase("AVAILABLE");
        long booked = vehicleRepository.countByStatusIgnoreCase("BOOKED");
        long maintenance = vehicleRepository.countByStatusIgnoreCase("MAINTENANCE");
        long decommissioned = vehicleRepository.countByStatusIgnoreCase("DECOMMISSIONED");
        counts.put("ALL", allActive);
        counts.put("AVAILABLE", available);
        counts.put("BOOKED", booked);
        counts.put("MAINTENANCE", maintenance);
        counts.put("DECOMMISSIONED", decommissioned);
        return counts;
    }
}
