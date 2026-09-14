package com.driveflow.demo_driveflow.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    java.util.List<Vehicle> findByStatus(String status);
}
