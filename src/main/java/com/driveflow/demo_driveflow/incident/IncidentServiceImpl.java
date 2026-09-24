package com.driveflow.demo_driveflow.incident;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentServiceImpl implements IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Override
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    @Override
    public List<Incident> searchIncidents(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return incidentRepository.findAll();
        }
        return incidentRepository.findByStatusIgnoreCase(status.trim());
    }

    @Override
    public Incident getIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found with id: " + id));
    }

    @Override
    public Incident logIncident(Incident incident) {
        if (incident.getStatus() == null || incident.getStatus().isBlank()) {
            incident.setStatus("OPEN");
        }
        return incidentRepository.save(incident);
    }

    @Override
    public Incident updateIncident(Long id, Incident updatedIncident) {
        Incident existing = getIncidentById(id);
        existing.setDate(updatedIncident.getDate());
        existing.setDescription(updatedIncident.getDescription());
        existing.setSeverity(updatedIncident.getSeverity());
        existing.setStatus(updatedIncident.getStatus());
        existing.setVehicle(updatedIncident.getVehicle());
        existing.setCustomer(updatedIncident.getCustomer());
        if (updatedIncident.getStaffMessage() != null) {
            existing.setStaffMessage(updatedIncident.getStaffMessage());
        }
        return incidentRepository.save(existing);
    }

    @Override
    public Incident updateIncidentStatus(Long id, String status, String staffMessage) {
        Incident existing = getIncidentById(id);
        if (status != null && !status.isBlank()) {
            existing.setStatus(status.trim().toUpperCase());
        }
        if (staffMessage != null && !staffMessage.isBlank()) {
            existing.setStaffMessage(staffMessage.trim());
        }
        return incidentRepository.save(existing);
    }

    @Override
    public List<Incident> getIncidentsByCustomer(Customer customer) {
        if (customer == null) {
            return List.of();
        }
        return incidentRepository.findByCustomerOrderByDateDesc(customer);
    }

    @Override
    public Map<String, Long> getIncidentStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("ALL", incidentRepository.count());
        counts.put("OPEN", incidentRepository.countByStatusIgnoreCase("OPEN"));
        counts.put("IN_PROGRESS", incidentRepository.countByStatusIgnoreCase("IN_PROGRESS"));
        counts.put("RESOLVED", incidentRepository.countByStatusIgnoreCase("RESOLVED"));
        counts.put("CLOSED", incidentRepository.countByStatusIgnoreCase("CLOSED"));
        return counts;
    }

    @Override
    public void removeIncident(Long id) {
        incidentRepository.deleteById(id);
    }
}
