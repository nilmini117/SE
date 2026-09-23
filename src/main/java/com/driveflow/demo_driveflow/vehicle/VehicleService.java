package com.driveflow.demo_driveflow.vehicle;

import java.util.List;

public interface VehicleService {
    List<Vehicle> getAllVehicles();
    List<Vehicle> searchVehicles(String search, String status);
    Vehicle getVehicleById(Long id);
    Vehicle registerVehicle(Vehicle vehicle);
    Vehicle updateVehicle(Long id, Vehicle vehicle);
    void removeVehicle(Long id);
}
