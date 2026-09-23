package com.driveflow.demo_driveflow.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(String status);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR v.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.regNo) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Vehicle> searchVehicles(@Param("search") String search, @Param("status") String status);
}
