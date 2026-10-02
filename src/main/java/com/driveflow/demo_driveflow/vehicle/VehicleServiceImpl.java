package com.driveflow.demo_driveflow.vehicle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class VehicleServiceImpl implements VehicleService {

    public static final List<String> BRAND_CATALOG = List.of("Toyota", "Suzuki", "Honda", "Tesla", "Benz");

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
    public List<Vehicle> searchVehiclesWithBrand(String search, String status, String brand) {
        String cleanSearch = (search != null) ? search.trim() : "";
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ALL";
        String cleanBrand = (brand != null && !brand.isBlank() && !"ALL".equalsIgnoreCase(brand)) ? brand.trim() : null;
        return vehicleRepository.searchVehiclesWithBrand(cleanSearch, cleanStatus, cleanBrand);
    }

    @Override
    public List<Vehicle> getVehiclesByBrand(String brand) {
        if (brand == null || brand.isBlank() || "ALL".equalsIgnoreCase(brand)) {
            return vehicleRepository.findAll().stream()
                    .filter(v -> v.getStatus() == null || !"DECOMMISSIONED".equalsIgnoreCase(v.getStatus()))
                    .toList();
        }
        return vehicleRepository.findByBrand(brand.trim());
    }

    @Override
    public List<Vehicle> getAvailableVehiclesByBrand(String brand) {
        if (brand == null || brand.isBlank() || "ALL".equalsIgnoreCase(brand)) {
            return vehicleRepository.findByStatusIgnoreCase("AVAILABLE");
        }
        return vehicleRepository.findAvailableByBrand(brand.trim());
    }

    @Override
    public List<String> getAllBrands() {
        return BRAND_CATALOG;
    }

    @Override
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
    }

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.branch.BranchRepository branchRepository;

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.booking.BookingRepository bookingRepository;

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.maintenance.MaintenanceRepository maintenanceRepository;

    @Override
    public Vehicle registerVehicle(Vehicle vehicle) {
        // Strict Business Rule: Enforce hardcoded defaults for vehicle registration.
        // Ignore any incoming values for status and quantity from the client payload.
        vehicle.setStatus("AVAILABLE");
        vehicle.setQuantity(1);

        if (vehicle.getBrand() == null || vehicle.getBrand().isBlank()) {
            if (vehicle.getModel() != null && vehicle.getModel().contains(" ")) {
                vehicle.setBrand(vehicle.getModel().substring(0, vehicle.getModel().indexOf(" ")));
            }
        }
        if (vehicle.getBranch() == null && branchRepository != null) {
            branchRepository.findAll().stream().findFirst().ifPresent(vehicle::setBranch);
        }
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle registerVehicle(VehicleRegistrationDto dto) {
        Vehicle vehicle = dto.toEntity();
        if (dto.getBranchId() != null && branchRepository != null) {
            branchRepository.findById(dto.getBranchId()).ifPresent(vehicle::setBranch);
        }
        return registerVehicle(vehicle);
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

        boolean hasBookings = false;
        try {
            if (bookingRepository != null) {
                hasBookings = bookingRepository.existsByVehicle_VehicleId(id);
            }
        } catch (Exception ignored) {}

        boolean hasMaintenance = false;
        try {
            if (maintenanceRepository != null) {
                hasMaintenance = maintenanceRepository.existsByVehicle_VehicleId(id);
            }
        } catch (Exception ignored) {}

        if (hasBookings || hasMaintenance) {
            // Referential integrity: implement soft deletion for vehicles with historical or active bookings/maintenance
            existing.setStatus("DECOMMISSIONED");
            vehicleRepository.save(existing);
        } else {
            // Safely delete unbooked test records
            vehicleRepository.delete(existing);
        }
    }

    @Override
    public Map<String, Long> getVehicleStatusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        long allActive = vehicleRepository.countByStatusNotIgnoreCase("DECOMMISSIONED");
        long available = vehicleRepository.countByStatusIgnoreCase("AVAILABLE");
        long booked = vehicleRepository.countByStatusIgnoreCase("BOOKED");
        long maintenance = vehicleRepository.countByStatusIgnoreCase("MAINTENANCE");
        long unavailable = vehicleRepository.countByStatusIgnoreCase("UNAVAILABLE");
        long decommissioned = vehicleRepository.countByStatusIgnoreCase("DECOMMISSIONED");
        counts.put("ALL", allActive);
        counts.put("AVAILABLE", available);
        counts.put("BOOKED", booked);
        counts.put("MAINTENANCE", maintenance);
        counts.put("UNAVAILABLE", unavailable);
        counts.put("DECOMMISSIONED", decommissioned);
        return counts;
    }

    @Override
    public Map<String, Long> getBrandVehicleCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String brand : BRAND_CATALOG) {
            counts.put(brand, 0L);
        }
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        for (Vehicle v : allVehicles) {
            if (v.getStatus() != null && "DECOMMISSIONED".equalsIgnoreCase(v.getStatus())) {
                continue;
            }
            if (v.getBrand() != null && !v.getBrand().isBlank()) {
                for (String brand : BRAND_CATALOG) {
                    if (brand.equalsIgnoreCase(v.getBrand().trim())) {
                        counts.put(brand, counts.get(brand) + 1);
                        break;
                    }
                }
            }
        }
        return counts;
    }
}
