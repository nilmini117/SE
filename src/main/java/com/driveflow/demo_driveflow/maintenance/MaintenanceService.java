package com.driveflow.demo_driveflow.maintenance;

import java.util.List;

public interface MaintenanceService {
    // Maintenance records & schedules
    List<Maintenance> getAllRecords();
    Maintenance getRecordById(Long id);
    Maintenance scheduleService(Maintenance record);
    Maintenance updateRecord(Long id, Maintenance record);
    void removeRecord(Long id);

    // Vehicle documents (kept for underlying data layer)
    List<VehicleDocument> getAllDocuments();
    VehicleDocument getDocumentById(Long id);
    VehicleDocument addDocument(VehicleDocument document);
    VehicleDocument updateDocument(Long id, VehicleDocument document);
    void removeDocument(Long id);
}
