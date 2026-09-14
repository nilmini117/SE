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
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
    }

    @Override
    public Vehicle registerVehicle(Vehicle vehicle) {
        vehicle.setStatus("AVAILABLE");
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
        return vehicleRepository.save(existing);
    }

    @Override
    public void removeVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}
