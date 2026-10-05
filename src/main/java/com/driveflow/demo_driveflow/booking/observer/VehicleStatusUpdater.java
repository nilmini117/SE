package com.driveflow.demo_driveflow.booking.observer;

import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Concrete Observer 2: Handles Vehicle Status Updates
 * Following lecture guidelines for the Observer Design Pattern.
 */
@Component
public class VehicleStatusUpdater implements BookingObserver {

    private static final Logger log = LoggerFactory.getLogger(VehicleStatusUpdater.class);

    @Autowired(required = false)
    private VehicleRepository vehicleRepository;

    public VehicleStatusUpdater() {
    }

    public VehicleStatusUpdater(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public void update(String bookingId, String vehicleRegNumber, String userEmail) {
        System.out.println("Vehicle Updater: Changing status of vehicle " + vehicleRegNumber + " to RENTED.");
        log.info("Vehicle Updater: Changing status of vehicle {} to RENTED for booking {}", vehicleRegNumber, bookingId);

        if (vehicleRepository != null && vehicleRegNumber != null && !vehicleRegNumber.isBlank()) {
            try {
                Optional<Vehicle> vehicleOpt = vehicleRepository.findByRegNoIgnoreCase(vehicleRegNumber.trim());
                if (vehicleOpt.isPresent()) {
                    Vehicle vehicle = vehicleOpt.get();
                    vehicle.setStatus("BOOKED");
                    vehicleRepository.save(vehicle);
                    log.info("Vehicle {} status successfully persisted as BOOKED.", vehicleRegNumber);
                }
            } catch (Exception ex) {
                log.warn("VehicleStatusUpdater: Failed to update vehicle status: {}", ex.getMessage());
            }
        }
    }
}
