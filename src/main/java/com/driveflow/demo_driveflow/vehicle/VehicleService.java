package com.driveflow.demo_driveflow.vehicle;

import java.util.List;
import java.util.Map;

public interface VehicleService {
    List<Vehicle> getAllVehicles();
    List<Vehicle> searchVehicles(String search, String status);
    List<Vehicle> searchVehiclesWithBrand(String search, String status, String brand);
    List<Vehicle> getVehiclesByBrand(String brand);
    List<Vehicle> getAvailableVehiclesByBrand(String brand);
    List<String> getAllBrands();
    Vehicle getVehicleById(Long id);
    Vehicle registerVehicle(Vehicle vehicle);
    Vehicle registerVehicle(VehicleRegistrationDto dto);
    Vehicle registerVehicle(Vehicle vehicle, org.springframework.web.multipart.MultipartFile imageFile);
    Vehicle registerVehicle(VehicleRegistrationDto dto, org.springframework.web.multipart.MultipartFile imageFile);
    List<Vehicle> getDashboardVehicles();
    Vehicle updateVehicle(Long id, Vehicle vehicle);
    Vehicle updateVehicle(Long id, Vehicle vehicle, org.springframework.web.multipart.MultipartFile imageFile);
    void removeVehicle(Long id);
    Map<String, Long> getVehicleStatusCounts();
    Map<String, Long> getBrandVehicleCounts();
}
