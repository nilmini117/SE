package com.driveflow.demo_driveflow.incident;

import java.util.List;

public interface IncidentService {
    List<Incident> getAllIncidents();
    Incident getIncidentById(Long id);
    Incident logIncident(Incident incident);
    Incident updateIncident(Long id, Incident incident);
    void removeIncident(Long id);
}
