package com.driveflow.demo_driveflow.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MaintenanceCompanyRepository extends JpaRepository<MaintenanceCompany, Long> {
    Optional<MaintenanceCompany> findByCompanyName(String companyName);
    Optional<MaintenanceCompany> findByEmail(String email);
    boolean existsByEmail(String email);
}
