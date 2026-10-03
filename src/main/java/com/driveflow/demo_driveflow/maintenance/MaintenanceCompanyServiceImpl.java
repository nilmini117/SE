package com.driveflow.demo_driveflow.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaintenanceCompanyServiceImpl implements MaintenanceCompanyService {

    @Autowired
    private MaintenanceCompanyRepository maintenanceCompanyRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Override
    public List<MaintenanceCompany> getAllCompanies() {
        return maintenanceCompanyRepository.findAll();
    }

    @Override
    public MaintenanceCompany getCompanyById(Long id) {
        return maintenanceCompanyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance company not found with id: " + id));
    }

    @Override
    @Transactional
    public MaintenanceCompany createCompany(MaintenanceCompany company) {
        if (company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be blank.");
        }
        if (company.getEmail() == null || company.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Company email cannot be blank.");
        }
        if (company.getContactNumber() == null || !company.getContactNumber().trim().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Contact number must be exactly 10 digits long.");
        }
        return maintenanceCompanyRepository.save(company);
    }

    @Override
    @Transactional
    public MaintenanceCompany updateCompany(Long id, MaintenanceCompany company) {
        if (company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be blank.");
        }
        if (company.getEmail() == null || company.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Company email cannot be blank.");
        }
        if (company.getContactNumber() == null || !company.getContactNumber().trim().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Contact number must be exactly 10 digits long.");
        }
        MaintenanceCompany existing = getCompanyById(id);
        existing.setCompanyName(company.getCompanyName());
        existing.setEmail(company.getEmail());
        existing.setContactNumber(company.getContactNumber());
        existing.setAddress(company.getAddress());
        existing.setSpeciality(company.getSpeciality());
        return maintenanceCompanyRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        MaintenanceCompany company = getCompanyById(id);
        List<Maintenance> linked = maintenanceRepository.findByMaintenanceCompany_CompanyId(id);
        if (!linked.isEmpty()) {
            throw new IllegalStateException("Cannot delete company '" + company.getCompanyName() + 
                    "' because it has " + linked.size() + " active maintenance record(s) linked to it.");
        }
        maintenanceCompanyRepository.delete(company);
    }
}
