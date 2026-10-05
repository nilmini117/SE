package com.driveflow.demo_driveflow.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    java.util.Optional<Vehicle> findByRegNo(String regNo);
    java.util.Optional<Vehicle> findByRegNoIgnoreCase(String regNo);

    List<Vehicle> findByStatus(String status);
    List<Vehicle> findByStatusIgnoreCase(String status);

    long countByStatusIgnoreCase(String status);
    long countByStatusNotIgnoreCase(String status);

    List<Vehicle> findByBranch_BranchId(Long branchId);
    List<Vehicle> findByBranch_BranchIdAndStatusIgnoreCase(Long branchId, String status);

    List<Vehicle> findByIsRegisteredTrue();
    List<Vehicle> findByIsRegisteredTrueAndStatusIgnoreCase(String status);

    @Query("SELECT v FROM Vehicle v WHERE (v.isRegistered = true) AND UPPER(v.status) != 'DECOMMISSIONED' ORDER BY v.vehicleId ASC")
    List<Vehicle> findDashboardVehicles();

    @Query("SELECT v FROM Vehicle v WHERE (v.isRegistered = true) AND UPPER(v.status) = 'AVAILABLE' ORDER BY v.vehicleId ASC")
    List<Vehicle> findAvailableDashboardVehicles();

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(v.isRegistered = true) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.regNo) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "( ((:status IS NULL OR :status = '' OR :status = 'ALL') AND (v.status IS NULL OR UPPER(v.status) != 'DECOMMISSIONED')) OR " +
           "  (UPPER(:status) = 'DECOMMISSIONED' AND UPPER(v.status) = 'DECOMMISSIONED') OR " +
           "  (UPPER(:status) != 'ALL' AND UPPER(:status) != 'DECOMMISSIONED' AND UPPER(v.status) = UPPER(:status)) ) " +
           "ORDER BY v.vehicleId ASC")
    List<Vehicle> searchDashboardVehicles(@Param("search") String search, @Param("status") String status);

    @Query("SELECT v FROM Vehicle v WHERE v.branch.branchId = :branchId AND UPPER(v.status) = 'AVAILABLE'")
    List<Vehicle> findAvailableByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT v FROM Vehicle v WHERE LOWER(v.model) LIKE LOWER(CONCAT(:brand, '%')) AND UPPER(v.status) != 'DECOMMISSIONED' ORDER BY v.vehicleId ASC")
    List<Vehicle> findByBrand(@Param("brand") String brand);

    @Query("SELECT v FROM Vehicle v WHERE LOWER(v.model) LIKE LOWER(CONCAT(:brand, '%')) AND UPPER(v.status) = 'AVAILABLE' ORDER BY v.vehicleId ASC")
    List<Vehicle> findAvailableByBrand(@Param("brand") String brand);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.regNo) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "( ((:status IS NULL OR :status = '' OR :status = 'ALL') AND (v.status IS NULL OR UPPER(v.status) != 'DECOMMISSIONED')) OR " +
           "  (UPPER(:status) = 'DECOMMISSIONED' AND UPPER(v.status) = 'DECOMMISSIONED') OR " +
           "  (UPPER(:status) != 'ALL' AND UPPER(:status) != 'DECOMMISSIONED' AND UPPER(v.status) = UPPER(:status)) ) " +
           "ORDER BY v.vehicleId ASC")
    List<Vehicle> searchVehicles(@Param("search") String search, @Param("status") String status);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.regNo) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:brand IS NULL OR :brand = '' OR :brand = 'ALL' OR LOWER(v.model) LIKE LOWER(CONCAT(:brand, '%'))) AND " +
           "( ((:status IS NULL OR :status = '' OR :status = 'ALL') AND (v.status IS NULL OR UPPER(v.status) != 'DECOMMISSIONED')) OR " +
           "  (UPPER(:status) = 'DECOMMISSIONED' AND UPPER(v.status) = 'DECOMMISSIONED') OR " +
           "  (UPPER(:status) != 'ALL' AND UPPER(:status) != 'DECOMMISSIONED' AND UPPER(v.status) = UPPER(:status)) ) " +
           "ORDER BY v.vehicleId ASC")
    List<Vehicle> searchVehiclesWithBrand(@Param("search") String search, @Param("status") String status, @Param("brand") String brand);
}
