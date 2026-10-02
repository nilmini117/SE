package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.vehicle.VehicleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private MaintenanceCompanyService maintenanceCompanyService;

    @Autowired
    private VehicleService vehicleService;

    // =========================================================================
    // 1. MAINTENANCE SCHEDULES
    // =========================================================================

    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute("records", maintenanceService.getAllRecords());
        return "maintenance/schedule-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Maintenance maintenance = new Maintenance();
        model.addAttribute("record", maintenance);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
        return "maintenance/schedule-form";
    }

    @PostMapping
    public String scheduleService(@ModelAttribute Maintenance record,
                                  @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                  @RequestParam(value = "companyId", required = false) Long companyId,
                                  @RequestParam(value = "approximatedCost", required = false) BigDecimal approximatedCost,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        // Strict Validation: Vehicle, Outsourced Company, Numerical Approximated Cost
        if (vehicleId == null) {
            model.addAttribute("errorMessage", "Mandatory requirement: Please select a vehicle from the fleet.");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            return "maintenance/schedule-form";
        }
        if (companyId == null) {
            model.addAttribute("errorMessage", "Mandatory requirement: Please select an outsourced maintenance company from the dropdown.");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            return "maintenance/schedule-form";
        }
        if (approximatedCost == null || approximatedCost.compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("errorMessage", "Mandatory requirement: Please enter a valid numerical approximated cost.");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            return "maintenance/schedule-form";
        }

        try {
            record.setVehicle(vehicleService.getVehicleById(vehicleId));
            record.setMaintenanceCompany(maintenanceCompanyService.getCompanyById(companyId));
            record.setApproximatedCost(approximatedCost);

            Maintenance saved = maintenanceService.scheduleService(record);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Maintenance scheduled successfully! Vehicle status automatically changed to UNAVAILABLE, and notification email sent to "
                            + saved.getMaintenanceCompany().getEmail() + ".");
            return "redirect:/maintenance";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Scheduling failed: " + e.getMessage());
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            return "maintenance/schedule-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("record", maintenanceService.getRecordById(id));
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
        return "maintenance/schedule-form";
    }

    @PostMapping("/{id}")
    public String updateSchedule(@PathVariable Long id,
                                 @ModelAttribute Maintenance record,
                                 @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                 @RequestParam(value = "companyId", required = false) Long companyId,
                                 @RequestParam(value = "approximatedCost", required = false) BigDecimal approximatedCost,
                                 RedirectAttributes redirectAttributes) {
        if (vehicleId != null) {
            record.setVehicle(vehicleService.getVehicleById(vehicleId));
        }
        if (companyId != null) {
            record.setMaintenanceCompany(maintenanceCompanyService.getCompanyById(companyId));
        }
        if (approximatedCost != null) {
            record.setApproximatedCost(approximatedCost);
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

    // =========================================================================
    // 2. MAINTENANCE COMPANIES CRUD (NEW STAFF PORTAL TAB)
    // =========================================================================

    @GetMapping("/companies")
    public String listCompanies(Model model) {
        model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
        return "maintenance/company-list";
    }

    @GetMapping("/companies/new")
    public String showCreateCompanyForm(Model model) {
        model.addAttribute("company", new MaintenanceCompany());
        return "maintenance/company-form";
    }

    @PostMapping("/companies")
    public String createCompany(@Valid @ModelAttribute MaintenanceCompany company,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "maintenance/company-form";
        }
        try {
            maintenanceCompanyService.createCompany(company);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Maintenance Company '" + company.getCompanyName() + "' registered successfully.");
            return "redirect:/maintenance/companies";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to register company: " + e.getMessage());
            return "maintenance/company-form";
        }
    }

    @GetMapping("/companies/{id}/edit")
    public String showEditCompanyForm(@PathVariable Long id, Model model) {
        model.addAttribute("company", maintenanceCompanyService.getCompanyById(id));
        return "maintenance/company-form";
    }

    @PostMapping("/companies/{id}")
    public String updateCompany(@PathVariable Long id,
                                @Valid @ModelAttribute MaintenanceCompany company,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            company.setCompanyId(id);
            return "maintenance/company-form";
        }
        try {
            maintenanceCompanyService.updateCompany(id, company);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Maintenance Company '" + company.getCompanyName() + "' updated successfully.");
            return "redirect:/maintenance/companies";
        } catch (Exception e) {
            company.setCompanyId(id);
            model.addAttribute("errorMessage", "Failed to update company: " + e.getMessage());
            return "maintenance/company-form";
        }
    }

    @PostMapping("/companies/{id}/delete")
    public String deleteCompanyPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteCompany(id, redirectAttributes);
    }

    @GetMapping("/companies/{id}/delete")
    public String deleteCompany(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceCompanyService.deleteCompany(id);
            redirectAttributes.addFlashAttribute("successMessage", "Maintenance company successfully deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete company: " + e.getMessage());
        }
        return "redirect:/maintenance/companies";
    }
}
