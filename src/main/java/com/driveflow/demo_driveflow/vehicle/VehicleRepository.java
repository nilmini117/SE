package com.driveflow.demo_driveflow.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(String status);

    long countByStatusIgnoreCase(String status);
    long countByStatusNotIgnoreCase(String status);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.regNo) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "( ((:status IS NULL OR :status = '' OR :status = 'ALL') AND (v.status IS NULL OR UPPER(v.status) != 'DECOMMISSIONED')) OR " +
           "  (UPPER(:status) = 'DECOMMISSIONED' AND UPPER(v.status) = 'DECOMMISSIONED') OR " +
           "  (UPPER(:status) != 'ALL' AND UPPER(:status) != 'DECOMMISSIONED' AND UPPER(v.status) = UPPER(:status)) ) " +
           "ORDER BY v.vehicleId ASC")
    List<Vehicle> searchVehicles(@Param("search") String search, @Param("status") String status);
}
