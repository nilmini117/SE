package com.driveflow.demo_driveflow.maintenance;

import java.util.List;

public interface MaintenanceCompanyService {
    List<MaintenanceCompany> getAllCompanies();
    MaintenanceCompany getCompanyById(Long id);
    MaintenanceCompany createCompany(MaintenanceCompany company);
    MaintenanceCompany updateCompany(Long id, MaintenanceCompany company);
    void deleteCompany(Long id);
}
