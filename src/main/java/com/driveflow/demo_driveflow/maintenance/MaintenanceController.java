package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.vehicle.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private VehicleService vehicleService;

    // --- MAINTENANCE SCHEDULES ---

    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute("records", maintenanceService.getAllRecords());
        return "maintenance/schedule-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("record", new MaintenanceRecord());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "maintenance/schedule-form";
    }

    @PostMapping
    public String scheduleService(@ModelAttribute MaintenanceRecord record,
                                  @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                  RedirectAttributes redirectAttributes) {
        if (vehicleId != null) {
            record.setVehicle(vehicleService.getVehicleById(vehicleId));
        }
        maintenanceService.scheduleService(record);
        redirectAttributes.addFlashAttribute("successMessage", "New maintenance service scheduled successfully.");
        return "redirect:/maintenance";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("record", maintenanceService.getRecordById(id));
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "maintenance/schedule-form";
    }

    @PostMapping("/{id}")
    public String updateSchedule(@PathVariable Long id,
                                 @ModelAttribute MaintenanceRecord record,
                                 @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                 RedirectAttributes redirectAttributes) {
        if (vehicleId != null) {
            record.setVehicle(vehicleService.getVehicleById(vehicleId));
        }
        maintenanceService.updateRecord(id, record);
        redirectAttributes.addFlashAttribute("successMessage", "Maintenance service record #MNT-" + id + " updated successfully.");
        return "redirect:/maintenance";
    }

    @PostMapping("/{id}/delete")
    public String removeRecordPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return removeRecord(id, redirectAttributes);
    }

    @GetMapping("/{id}/delete")
    public String removeRecord(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceService.removeRecord(id);
            redirectAttributes.addFlashAttribute("successMessage", "Maintenance service record #MNT-" + id + " has been successfully deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove service record: " + e.getMessage());
        }
        return "redirect:/maintenance";
    }

    // --- VEHICLE DOCUMENTS ---

    @GetMapping("/documents")
    public String listDocuments(Model model) {
        model.addAttribute("documents", maintenanceService.getAllDocuments());
        return "maintenance/document-list";
    }

    @GetMapping("/documents/new")
    public String showDocumentForm(Model model) {
        model.addAttribute("document", new VehicleDocument());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "maintenance/document-form";
    }

    @PostMapping("/documents")
    public String addDocument(@ModelAttribute VehicleDocument document,
                              @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                              RedirectAttributes redirectAttributes) {
        if (vehicleId != null) {
            document.setVehicle(vehicleService.getVehicleById(vehicleId));
        }
        maintenanceService.addDocument(document);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle document added successfully.");
        return "redirect:/maintenance/documents";
    }

    @GetMapping("/documents/{id}/edit")
    public String showDocumentEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("document", maintenanceService.getDocumentById(id));
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "maintenance/document-form";
    }

    @PostMapping("/documents/{id}")
    public String updateDocument(@PathVariable Long id,
                                 @ModelAttribute VehicleDocument document,
                                 @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                 RedirectAttributes redirectAttributes) {
        if (vehicleId != null) {
            document.setVehicle(vehicleService.getVehicleById(vehicleId));
        }
        maintenanceService.updateDocument(id, document);
        redirectAttributes.addFlashAttribute("successMessage", "Vehicle document #DOC-" + id + " updated successfully.");
        return "redirect:/maintenance/documents";
    }

    @PostMapping("/documents/{id}/delete")
    public String removeDocumentPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return removeDocument(id, redirectAttributes);
    }

    @GetMapping("/documents/{id}/delete")
    public String removeDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceService.removeDocument(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle document #DOC-" + id + " has been successfully deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove document: " + e.getMessage());
        }
        return "redirect:/maintenance/documents";
    }
}
