package com.driveflow.demo_driveflow.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Override
    public List<MaintenanceRecord> getAllRecords() {
        return maintenanceRecordRepository.findAll();
    }

    @Override
    public MaintenanceRecord getRecordById(Long id) {
        return maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found: " + id));
    }

    @Override
    public MaintenanceRecord scheduleService(MaintenanceRecord record) {
        return maintenanceRecordRepository.save(record);
    }

    @Override
    public MaintenanceRecord updateRecord(Long id, MaintenanceRecord updated) {
        MaintenanceRecord existing = getRecordById(id);
        existing.setServiceDate(updated.getServiceDate());
        existing.setCost(updated.getCost());
        if (updated.getVehicle() != null) {
            existing.setVehicle(updated.getVehicle());
        }
        return maintenanceRecordRepository.save(existing);
    }

    @Override
    public void removeRecord(Long id) {
        maintenanceRecordRepository.deleteById(id);
    }

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
    public VehicleDocument addDocument(VehicleDocument document) {
        return vehicleDocumentRepository.save(document);
    }

    @Override
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
    public void removeDocument(Long id) {
        vehicleDocumentRepository.deleteById(id);
    }
}
