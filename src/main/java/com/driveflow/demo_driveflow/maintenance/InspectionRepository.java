package com.driveflow.demo_driveflow.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByType(String type);
    List<Inspection> findAllByOrderByInspectionDateDesc();
    List<Inspection> findByVehicleVehicleId(Long vehicleId);
}
