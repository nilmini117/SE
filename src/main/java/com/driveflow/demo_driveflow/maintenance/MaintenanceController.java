package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private MaintenanceCompanyService maintenanceCompanyService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.driveflow.demo_driveflow.payment.PaymentService paymentService;

    private BigDecimal getNetOperatingIncome() {
        if (paymentService != null) {
            try {
                com.driveflow.demo_driveflow.payment.CompanySalesSummaryDto sales = paymentService.getCompanySalesSummary();
                if (sales != null && sales.getNetIncome() != null) {
                    return sales.getNetIncome();
                }
            } catch (Exception ignored) {}
        }
        return BigDecimal.ZERO;
    }

    // =========================================================================
    // 1. MAINTENANCE SCHEDULES
    // =========================================================================

    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute("records", maintenanceService.getAllRecords());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
        model.addAttribute("netIncome", getNetOperatingIncome());
        return "maintenance/schedule-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Maintenance maintenance = new Maintenance();
        model.addAttribute("record", maintenance);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
        model.addAttribute("netIncome", getNetOperatingIncome());
        return "maintenance/schedule-form";
    }

    @PostMapping
    public String scheduleService(@ModelAttribute Maintenance record,
                                  @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                  @RequestParam(value = "companyId", required = false) Long companyId,
                                  @RequestParam(value = "approximatedCost", required = false) BigDecimal approximatedCost,
                                  jakarta.servlet.http.HttpServletResponse response,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        BigDecimal netIncome = getNetOperatingIncome();

        // Strict Validation: Vehicle, Outsourced Company, Numerical Approximated Cost
        if (vehicleId == null) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Mandatory requirement: Please select a vehicle from the fleet.");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            model.addAttribute("netIncome", netIncome);
            return "maintenance/schedule-form";
        }
        if (companyId == null) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Mandatory requirement: Please select an outsourced maintenance company from the dropdown.");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            model.addAttribute("netIncome", netIncome);
            return "maintenance/schedule-form";
        }
        if (approximatedCost == null || approximatedCost.compareTo(BigDecimal.ZERO) < 0) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Mandatory requirement: Please enter a valid numerical approximated cost.");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            model.addAttribute("netIncome", netIncome);
            return "maintenance/schedule-form";
        }

        // Strict Maintenance Cost Limit: Cost must be strictly less than current Real Net Operating Income
        if (netIncome != null && approximatedCost.compareTo(netIncome) >= 0) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Validation failed: Approximated maintenance cost (Rs. " 
                    + approximatedCost + ") must be strictly less than the company's current Real Net Operating Income (Rs. " 
                    + netIncome + ").");
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            model.addAttribute("netIncome", netIncome);
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
        } catch (IllegalArgumentException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Scheduling failed: " + e.getMessage());
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            model.addAttribute("netIncome", netIncome);
            return "maintenance/schedule-form";
        } catch (Exception e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Scheduling failed: " + e.getMessage());
            model.addAttribute("record", record);
            model.addAttribute("vehicles", vehicleService.getAllVehicles());
            model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
            model.addAttribute("netIncome", netIncome);
            return "maintenance/schedule-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("record", maintenanceService.getRecordById(id));
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("companies", maintenanceCompanyService.getAllCompanies());
        model.addAttribute("netIncome", getNetOperatingIncome());
        return "maintenance/schedule-form";
    }

    @PostMapping("/{id}")
    public String updateSchedule(@PathVariable Long id,
                                 @ModelAttribute Maintenance record,
                                 @RequestParam(value = "vehicleId", required = false) Long vehicleId,
                                 @RequestParam(value = "companyId", required = false) Long companyId,
                                 @RequestParam(value = "approximatedCost", required = false) BigDecimal approximatedCost,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        BigDecimal netIncome = getNetOperatingIncome();
        if (approximatedCost != null && netIncome != null && approximatedCost.compareTo(netIncome) >= 0) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: Approximated maintenance cost (Rs. " 
                    + approximatedCost + ") must be strictly less than the company's current Real Net Operating Income (Rs. " 
                    + netIncome + ").");
            return "redirect:/maintenance";
        }
        if (vehicleId != null) {
            record.setVehicle(vehicleService.getVehicleById(vehicleId));
        }
        if (companyId != null) {
            record.setMaintenanceCompany(maintenanceCompanyService.getCompanyById(companyId));
        }
        if (approximatedCost != null) {
            record.setApproximatedCost(approximatedCost);
        }
        try {
            maintenanceService.updateRecord(id, record);
            redirectAttributes.addFlashAttribute("successMessage", "Maintenance service record #MNT-" + id + " updated successfully.");
            return "redirect:/maintenance";
        } catch (IllegalArgumentException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            redirectAttributes.addFlashAttribute("errorMessage", "Update failed: " + e.getMessage());
            return "redirect:/maintenance";
        }
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
                                jakarta.servlet.http.HttpServletResponse response,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (company.getContactNumber() == null || !company.getContactNumber().trim().matches("^[0-9]{10}$")) {
            result.rejectValue("contactNumber", "error.contactNumber", "Contact number must be exactly 10 digits long.");
        }
        if (result.hasErrors()) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            return "maintenance/company-form";
        }
        try {
            maintenanceCompanyService.createCompany(company);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Maintenance Company '" + company.getCompanyName() + "' registered successfully.");
            return "redirect:/maintenance/companies";
        } catch (IllegalArgumentException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", e.getMessage());
            return "maintenance/company-form";
        } catch (Exception e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", "Failed to register company: " + e.getMessage());
            return "maintenance/company-form";
        }
    }

    @PostMapping(value = {"/companies", "/api/maintenance/companies"}, consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> createCompanyJson(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody MaintenanceCompany company) {
        MaintenanceCompany created = maintenanceCompanyService.createCompany(company);
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        java.util.Map<String, String> fieldErrors = new java.util.HashMap<>();
        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("status", org.springframework.http.HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed for maintenance company payload");
        body.put("errors", fieldErrors);
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(body);
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
                                jakarta.servlet.http.HttpServletResponse response,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (company.getContactNumber() == null || !company.getContactNumber().trim().matches("^[0-9]{10}$")) {
            result.rejectValue("contactNumber", "error.contactNumber", "Contact number must be exactly 10 digits long.");
        }
        if (result.hasErrors()) {
            company.setCompanyId(id);
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            return "maintenance/company-form";
        }
        try {
            maintenanceCompanyService.updateCompany(id, company);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Maintenance Company '" + company.getCompanyName() + "' updated successfully.");
            return "redirect:/maintenance/companies";
        } catch (IllegalArgumentException e) {
            company.setCompanyId(id);
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("errorMessage", e.getMessage());
            return "maintenance/company-form";
        } catch (Exception e) {
            company.setCompanyId(id);
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
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

    // =========================================================================
    // FLEET MAINTENANCE (Digital Legal Documents & Routine Inspections)
    // =========================================================================

    @GetMapping({"/fleet", "/documents"})
    public String showFleetMaintenance(Model model) {
        List<VehicleDocument> documents = maintenanceService.getAllDocuments();
        List<Inspection> inspections = maintenanceService.getAllInspections();
        List<Vehicle> vehicles = vehicleService.getAllVehicles();

        long validDocCount = documents.stream()
                .filter(d -> d.getExpiryDate() != null && !d.getExpiryDate().isBefore(java.time.LocalDate.now()))
                .count();
        long expiredDocCount = documents.size() - validDocCount;

        long passedCount = inspections.stream()
                .filter(i -> "PASSED".equalsIgnoreCase(i.getResult()))
                .count();
        long attentionCount = inspections.stream()
                .filter(i -> "NEEDS_REPAIR".equalsIgnoreCase(i.getResult()) || "FAILED".equalsIgnoreCase(i.getResult()))
                .count();

        model.addAttribute("documents", documents);
        model.addAttribute("inspections", inspections);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("validDocCount", validDocCount);
        model.addAttribute("expiredDocCount", expiredDocCount);
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("attentionCount", attentionCount);
        model.addAttribute("newDocument", new VehicleDocument());
        model.addAttribute("newInspection", new Inspection());
        return "maintenance/fleet-maintenance";
    }

    @PostMapping("/documents/upload")
    public String uploadVehicleDocument(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("docType") String docType,
            @RequestParam("expiryDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate expiryDate,
            @RequestParam(value = "documentFile", required = false) org.springframework.web.multipart.MultipartFile documentFile,
            RedirectAttributes redirectAttributes) {

        try {
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
            VehicleDocument doc = new VehicleDocument();
            doc.setVehicle(vehicle);
            doc.setDocType(docType);
            doc.setExpiryDate(expiryDate);
            doc.setUploadedAt(java.time.LocalDate.now());

            if (documentFile != null && !documentFile.isEmpty()) {
                String originalFilename = org.springframework.util.StringUtils.cleanPath(documentFile.getOriginalFilename());
                String ext = "";
                int idx = originalFilename.lastIndexOf('.');
                if (idx > 0) {
                    ext = originalFilename.substring(idx);
                }
                String safeName = "DOC_VEH_" + vehicleId + "_" + System.currentTimeMillis() + ext;
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "documents");
                if (!java.nio.file.Files.exists(uploadDir)) {
                    java.nio.file.Files.createDirectories(uploadDir);
                }
                java.nio.file.Path dest = uploadDir.resolve(safeName);
                java.nio.file.Files.copy(documentFile.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                doc.setFileName(originalFilename);
                doc.setFilePath("/uploads/documents/" + safeName);
            } else {
                doc.setFileName("Digital_Record_" + System.currentTimeMillis());
                doc.setFilePath(null);
            }

            maintenanceService.addDocument(doc);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Vehicle document '" + docType + "' successfully registered and uploaded for " + vehicle.getModel() + " (" + vehicle.getRegNo() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to register document: " + e.getMessage());
        }
        return "redirect:/maintenance/fleet";
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteVehicleDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceService.removeDocument(id);
            redirectAttributes.addFlashAttribute("successMessage", "Document successfully deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete document: " + e.getMessage());
        }
        return "redirect:/maintenance/fleet";
    }

    @PostMapping("/inspections")
    public String logRoutineInspection(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam(value = "inspectionDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate inspectionDate,
            @RequestParam(value = "fuelLevel", defaultValue = "100") Integer fuelLevel,
            @RequestParam(value = "result", defaultValue = "PASSED") String result,
            @RequestParam(value = "damageNotes", required = false) String damageNotes,
            RedirectAttributes redirectAttributes) {

        try {
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
            Inspection inspection = new Inspection();
            inspection.setVehicle(vehicle);
            inspection.setType("ROUTINE");
            inspection.setInspectionDate(inspectionDate != null ? inspectionDate : java.time.LocalDate.now());
            inspection.setFuelLevel(fuelLevel != null ? Math.max(0, Math.min(100, fuelLevel)) : 100);
            inspection.setResult(result != null ? result.toUpperCase() : "PASSED");
            inspection.setDamageNotes(damageNotes);

            maintenanceService.logInspection(inspection);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Routine inspection result (" + inspection.getResult() + ") logged for " + vehicle.getModel() + " (" + vehicle.getRegNo() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to log inspection: " + e.getMessage());
        }
        return "redirect:/maintenance/fleet";
    }

    @PostMapping("/inspections/{id}/delete")
    public String deleteRoutineInspection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            maintenanceService.removeInspection(id);
            redirectAttributes.addFlashAttribute("successMessage", "Inspection record successfully removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete inspection record: " + e.getMessage());
        }
        return "redirect:/maintenance/fleet";
    }
}
