package com.driveflow.demo_driveflow.incident;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IncidentServiceImpl implements IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Override
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    @Override
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));
    }

    @Override
    public Incident logIncident(Incident incident) {
        incident.setStatus("OPEN");
        return incidentRepository.save(incident);
    }

    @Override
    public Incident updateIncident(Long id, Incident updatedIncident) {
        Incident existing = getIncidentById(id);
        existing.setDescription(updatedIncident.getDescription());
        existing.setSeverity(updatedIncident.getSeverity());
        existing.setStatus(updatedIncident.getStatus());
        return incidentRepository.save(existing);
    }

    @Override
    public void removeIncident(Long id) {
        incidentRepository.deleteById(id);
    }
}
