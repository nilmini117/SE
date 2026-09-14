package com.driveflow.demo_driveflow.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceService maintenanceService;

    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute("records", maintenanceService.getAllRecords());
        return "maintenance/schedule-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("record", new MaintenanceRecord());
        return "maintenance/schedule-form";
    }

    @PostMapping
    public String scheduleService(@ModelAttribute MaintenanceRecord record) {
        maintenanceService.scheduleService(record);
        return "redirect:/maintenance";
    }

    @GetMapping("/{id}/delete")
    public String removeRecord(@PathVariable Long id) {
        maintenanceService.removeRecord(id);
        return "redirect:/maintenance";
    }

    @GetMapping("/documents")
    public String listDocuments(Model model) {
        model.addAttribute("documents", maintenanceService.getAllDocuments());
        return "maintenance/document-list";
    }

    @GetMapping("/documents/new")
    public String showDocumentForm(Model model) {
        model.addAttribute("document", new VehicleDocument());
        return "maintenance/document-form";
    }

    @PostMapping("/documents")
    public String addDocument(@ModelAttribute VehicleDocument document) {
        maintenanceService.addDocument(document);
        return "redirect:/maintenance/documents";
    }

    @GetMapping("/documents/{id}/delete")
    public String removeDocument(@PathVariable Long id) {
        maintenanceService.removeDocument(id);
        return "redirect:/maintenance/documents";
    }
}
