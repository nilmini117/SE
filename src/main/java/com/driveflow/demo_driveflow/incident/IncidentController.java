package com.driveflow.demo_driveflow.incident;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @GetMapping
    public String listIncidents(Model model) {
        model.addAttribute("incidents", incidentService.getAllIncidents());
        return "incident/incident-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("incident", new Incident());
        return "incident/incident-form";
    }

    @PostMapping
    public String logIncident(@ModelAttribute Incident incident) {
        incidentService.logIncident(incident);
        return "redirect:/incidents";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("incident", incidentService.getIncidentById(id));
        return "incident/incident-form";
    }

    @PostMapping("/{id}")
    public String updateIncident(@PathVariable Long id, @ModelAttribute Incident incident) {
        incidentService.updateIncident(id, incident);
        return "redirect:/incidents";
    }

    @GetMapping("/{id}/delete")
    public String deleteIncident(@PathVariable Long id) {
        incidentService.removeIncident(id);
        return "redirect:/incidents";
    }
}
