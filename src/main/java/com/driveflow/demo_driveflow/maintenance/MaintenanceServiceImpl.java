package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.email.EmailService;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceServiceImpl.class);

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private MaintenanceCompanyRepository maintenanceCompanyRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public List<Maintenance> getAllRecords() {
        return maintenanceRepository.findAll();
    }

    @Override
    public Maintenance getRecordById(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found: " + id));
    }

    @Override
    @Transactional
    public Maintenance scheduleService(Maintenance record) {
        // 1. Mandatory Vehicle Selection Validation
        if (record.getVehicle() == null || record.getVehicle().getVehicleId() == null) {
            throw new IllegalArgumentException("A vehicle must be selected for maintenance scheduling.");
        }

        // 2. Mandatory Outsourced Maintenance Company Validation
        if (record.getMaintenanceCompany() == null || record.getMaintenanceCompany().getCompanyId() == null) {
            throw new IllegalArgumentException("Mandatory selection: Please assign an external maintenance company.");
        }

        // 3. Mandatory Numerical Approximated Cost Validation
        if (record.getApproximatedCost() == null || record.getApproximatedCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mandatory input: A valid numerical approximated cost must be specified.");
        }

        // Retrieve managed vehicle
        Long vehicleId = record.getVehicle().getVehicleId();
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        // AUTOMATION: Automatically update vehicle's global status to UNAVAILABLE
        vehicle.setStatus("UNAVAILABLE");
        vehicleRepository.save(vehicle);
        record.setVehicle(vehicle);

        // Retrieve managed maintenance company
        Long companyId = record.getMaintenanceCompany().getCompanyId();
        MaintenanceCompany company = maintenanceCompanyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance company not found with ID: " + companyId));
        record.setMaintenanceCompany(company);

        // Set default dates and costs if unassigned
        if (record.getServiceDate() == null) {
            record.setServiceDate(LocalDate.now());
        }
        if (record.getCost() == null) {
            record.setCost(record.getApproximatedCost());
        }

        // Save maintenance schedule to database
        Maintenance saved = maintenanceRepository.save(record);
        log.info("Vehicle #VH-{} [{}] scheduled for service. Status switched to UNAVAILABLE.",
                vehicle.getVehicleId(), vehicle.getDisplayName());

        // NOTIFICATION: Automatically trigger email notification to the assigned maintenance company
        try {
            emailService.sendMaintenanceNotificationEmail(
                    company.getEmail(),
                    company.getCompanyName(),
                    vehicle.getDisplayName(),
                    saved.getServiceDate(),
                    saved.getApproximatedCost()
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch maintenance email notification to {}: {}", company.getEmail(), e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public Maintenance updateRecord(Long id, Maintenance updated) {
        Maintenance existing = getRecordById(id);
        existing.setServiceDate(updated.getServiceDate());
        existing.setCost(updated.getCost());
        if (updated.getApproximatedCost() != null) {
            existing.setApproximatedCost(updated.getApproximatedCost());
        }
        if (updated.getVehicle() != null && updated.getVehicle().getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(updated.getVehicle().getVehicleId())
                    .orElse(existing.getVehicle());
            existing.setVehicle(vehicle);
        }
        if (updated.getMaintenanceCompany() != null && updated.getMaintenanceCompany().getCompanyId() != null) {
            MaintenanceCompany company = maintenanceCompanyRepository.findById(updated.getMaintenanceCompany().getCompanyId())
                    .orElse(existing.getMaintenanceCompany());
            existing.setMaintenanceCompany(company);
        }
        return maintenanceRepository.save(existing);
    }

    @Override
    @Transactional
    public void removeRecord(Long id) {
        Maintenance record = getRecordById(id);
        maintenanceRepository.delete(record);
    }

    // Vehicle documents
    @Override
    public List<VehicleDocument> getAllDocuments() {
        return vehicleDocumentRepository.findAll();
    }

    @Override
    public VehicleDocument getDocumentById(Long id) {
        return vehicleDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    @Override
    @Transactional
    public VehicleDocument addDocument(VehicleDocument document) {
        return vehicleDocumentRepository.save(document);
    }

    @Override
    @Transactional
    public VehicleDocument updateDocument(Long id, VehicleDocument updated) {
        VehicleDocument existing = getDocumentById(id);
        existing.setDocType(updated.getDocType());
        existing.setExpiryDate(updated.getExpiryDate());
        if (updated.getVehicle() != null) {
            existing.setVehicle(updated.getVehicle());
        }
        return vehicleDocumentRepository.save(existing);
    }

    @Override
    @Transactional
    public void removeDocument(Long id) {
        vehicleDocumentRepository.deleteById(id);
    }
}
