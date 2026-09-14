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
    public MaintenanceRecord scheduleService(MaintenanceRecord record) {
        return maintenanceRecordRepository.save(record);
    }

    @Override
    public MaintenanceRecord updateRecord(Long id, MaintenanceRecord updated) {
        MaintenanceRecord existing = maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found: " + id));
        existing.setServiceDate(updated.getServiceDate());
        existing.setCost(updated.getCost());
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
    public VehicleDocument addDocument(VehicleDocument document) {
        return vehicleDocumentRepository.save(document);
    }

    @Override
    public VehicleDocument updateDocument(Long id, VehicleDocument updated) {
        VehicleDocument existing = vehicleDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        existing.setDocType(updated.getDocType());
        existing.setExpiryDate(updated.getExpiryDate());
        return vehicleDocumentRepository.save(existing);
    }

    @Override
    public void removeDocument(Long id) {
        vehicleDocumentRepository.deleteById(id);
    }
}
