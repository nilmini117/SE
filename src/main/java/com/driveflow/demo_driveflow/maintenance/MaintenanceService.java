package com.driveflow.demo_driveflow.maintenance;

import java.util.List;

public interface MaintenanceService {
    // Maintenance records
    List<MaintenanceRecord> getAllRecords();
    MaintenanceRecord getRecordById(Long id);
    MaintenanceRecord scheduleService(MaintenanceRecord record);
    MaintenanceRecord updateRecord(Long id, MaintenanceRecord record);
    void removeRecord(Long id);

    // Vehicle documents
    List<VehicleDocument> getAllDocuments();
    VehicleDocument getDocumentById(Long id);
    VehicleDocument addDocument(VehicleDocument document);
    VehicleDocument updateDocument(Long id, VehicleDocument document);
    void removeDocument(Long id);
}
