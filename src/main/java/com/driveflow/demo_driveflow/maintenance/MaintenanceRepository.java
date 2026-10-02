package com.driveflow.demo_driveflow.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    List<Maintenance> findByVehicle_VehicleId(Long vehicleId);
    boolean existsByVehicle_VehicleId(Long vehicleId);
    List<Maintenance> findByMaintenanceCompany_CompanyId(Long companyId);

    @Query(value = "SELECT COALESCE(SUM(m.approximated_cost), 0) FROM maintenance m", nativeQuery = true)
    BigDecimal calculateTotalMaintenanceCosts();
}

