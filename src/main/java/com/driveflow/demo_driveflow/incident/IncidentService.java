package com.driveflow.demo_driveflow.incident;

import com.driveflow.demo_driveflow.users.Customer;
import java.util.List;
import java.util.Map;

public interface IncidentService {
    List<Incident> getAllIncidents();
    List<Incident> searchIncidents(String status);
    Incident getIncidentById(Long id);
    Incident logIncident(Incident incident);
    Incident updateIncident(Long id, Incident incident);
    Incident updateIncidentStatus(Long id, String status, String staffMessage);
    List<Incident> getIncidentsByCustomer(Customer customer);
    Map<String, Long> getIncidentStatusCounts();
    void removeIncident(Long id);
}
