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

    @org.springframework.beans.factory.annotation.Value("${upload.path:uploads}")
    private String uploadPath;

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
    // FLEET MAINTENANCE (Digital Legal Documents)
    // =========================================================================

    @GetMapping({"/fleet", "/documents"})
    public String showFleetMaintenance(Model model) {
        List<VehicleDocument> documents = maintenanceService.getAllDocuments();
        List<Vehicle> vehicles = vehicleService.getAllVehicles();

        long validDocCount = documents.stream()
                .filter(d -> d.getExpiryDate() != null && !d.getExpiryDate().isBefore(java.time.LocalDate.now()))
                .count();
        long expiredDocCount = documents.size() - validDocCount;

        model.addAttribute("documents", documents);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("totalVehicles", vehicles.size());
        model.addAttribute("validDocCount", validDocCount);
        model.addAttribute("expiredDocCount", expiredDocCount);
        model.addAttribute("newDocument", new VehicleDocument());
        return "maintenance/fleet-maintenance";
    }

    private String normalizeDocType(String input) {
        if (input == null || input.isBlank()) {
            return "INSURANCE";
        }
        String clean = input.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if (clean.contains("REVENUE") || clean.contains("LICENSE")) {
            return "REVENUE_LICENSE";
        }
        if (clean.contains("FITNESS")) {
            return "FITNESS_CERT";
        }
        if (clean.contains("EMISSION")) {
            return "EMISSION";
        }
        if (clean.contains("INSUR")) {
            return "INSURANCE";
        }
        if (clean.equals("REVENUE_LICENSE") || clean.equals("INSURANCE") || clean.equals("FITNESS_CERT") || clean.equals("EMISSION")) {
            return clean;
        }
        return "INSURANCE";
    }

    @PostMapping("/documents/upload")
    public String uploadVehicleDocument(
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "customDocType", required = false) String customDocType,
            @RequestParam(value = "expiryDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate expiryDate,
            @RequestParam(value = "documentFile", required = false) org.springframework.web.multipart.MultipartFile documentFile,
            RedirectAttributes redirectAttributes) {

        if (vehicleId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a fleet vehicle.");
            return "redirect:/maintenance/fleet";
        }

        String rawDocType = (docType != null && docType.equalsIgnoreCase("OTHER") && customDocType != null && !customDocType.isBlank())
                ? customDocType.trim()
                : (docType != null ? docType.trim() : "");

        if (rawDocType.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid document type.");
            return "redirect:/maintenance/fleet";
        }

        if (expiryDate == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a valid expiration date.");
            return "redirect:/maintenance/fleet";
        }

        String resolvedDocType = normalizeDocType(rawDocType);

        try {
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
            if (vehicle == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected vehicle could not be found.");
                return "redirect:/maintenance/fleet";
            }

            VehicleDocument doc = new VehicleDocument();
            doc.setVehicle(vehicle);
            doc.setDocType(resolvedDocType);
            doc.setExpiryDate(expiryDate);
            doc.setUploadedAt(java.time.LocalDate.now());

            saveUploadedDocumentFile(doc, documentFile, vehicleId, resolvedDocType);

            maintenanceService.addDocument(doc);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Vehicle document successfully registered for " + vehicle.getModel() + " (" + vehicle.getRegNo() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to register document: " + e.getMessage());
        }
        return "redirect:/maintenance/fleet";
    }

    private void saveUploadedDocumentFile(VehicleDocument doc, org.springframework.web.multipart.MultipartFile documentFile, Long vehicleId, String docType) {
        if (documentFile != null && !documentFile.isEmpty()) {
            try {
                String orig = documentFile.getOriginalFilename();
                String originalFilename = (orig != null && !orig.isBlank()) ? org.springframework.util.StringUtils.cleanPath(orig) : "document";
                String ext = "";
                int idx = originalFilename.lastIndexOf('.');
                if (idx >= 0) {
                    ext = originalFilename.substring(idx);
                }
                String safeName = "DOC_VEH_" + (vehicleId != null ? vehicleId : "0") + "_" + System.currentTimeMillis() + ext;
                String baseDirStr = (uploadPath != null && !uploadPath.isBlank()) ? uploadPath : "uploads";
                java.nio.file.Path targetDir = java.nio.file.Paths.get(baseDirStr, "documents").toAbsolutePath().normalize();
                if (!java.nio.file.Files.exists(targetDir)) {
                    java.nio.file.Files.createDirectories(targetDir);
                }
                java.nio.file.Path dest = targetDir.resolve(safeName);
                java.nio.file.Files.copy(documentFile.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Mirror copy to static folders for instant serving if possible
                try {
                    java.nio.file.Path staticDir = java.nio.file.Paths.get("src", "main", "resources", "static", "uploads", "documents").toAbsolutePath().normalize();
                    if (!java.nio.file.Files.exists(staticDir)) {
                        java.nio.file.Files.createDirectories(staticDir);
                    }
                    java.nio.file.Files.copy(dest, staticDir.resolve(safeName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}

                try {
                    java.nio.file.Path targetClassesDir = java.nio.file.Paths.get("target", "classes", "static", "uploads", "documents").toAbsolutePath().normalize();
                    if (!java.nio.file.Files.exists(targetClassesDir)) {
                        java.nio.file.Files.createDirectories(targetClassesDir);
                    }
                    java.nio.file.Files.copy(dest, targetClassesDir.resolve(safeName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}

                doc.setFileName(originalFilename);
                doc.setFilePath("/uploads/documents/" + safeName);
            } catch (Exception e) {
                throw new RuntimeException("Could not store uploaded document file: " + e.getMessage(), e);
            }
        } else if (doc.getFileName() == null || doc.getFileName().isBlank()) {
            doc.setFileName("Digital Record (" + (docType != null ? docType : "Document") + ")");
        }
    }

    @GetMapping("/documents/{id}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadVehicleDocument(@PathVariable Long id) {
        VehicleDocument doc = maintenanceService.getDocumentById(id);
        if (doc == null || doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        try {
            String filePath = doc.getFilePath();
            String rawFileName = (doc.getFileName() != null && !doc.getFileName().isBlank()) ? doc.getFileName() : "vehicle_document";
            String diskFileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            String baseDirStr = (uploadPath != null && !uploadPath.isBlank()) ? uploadPath : "uploads";

            java.nio.file.Path targetFile = java.nio.file.Paths.get(baseDirStr, "documents", diskFileName).toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(targetFile)) {
                targetFile = java.nio.file.Paths.get("src", "main", "resources", "static", "uploads", "documents", diskFileName).toAbsolutePath().normalize();
            }
            if (!java.nio.file.Files.exists(targetFile)) {
                targetFile = java.nio.file.Paths.get("target", "classes", "static", "uploads", "documents", diskFileName).toAbsolutePath().normalize();
            }

            if (!java.nio.file.Files.exists(targetFile)) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }

            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(targetFile.toUri());
            String contentType = java.nio.file.Files.probeContentType(targetFile);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String downloadName = rawFileName;
            int dotIdx = diskFileName.lastIndexOf('.');
            if (dotIdx > 0 && !downloadName.contains(".")) {
                downloadName += diskFileName.substring(dotIdx);
            }

            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName.replace("\"", "") + "\"")
                    .body(resource);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents/new")
    public String showCreateDocumentForm(Model model) {
        model.addAttribute("document", new VehicleDocument());
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "maintenance/document-form";
    }

    @PostMapping("/documents")
    public String createDocument(
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "documentFile", required = false) org.springframework.web.multipart.MultipartFile documentFile,
            @ModelAttribute VehicleDocument document,
            RedirectAttributes redirectAttributes) {

        if (vehicleId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a vehicle.");
            return "redirect:/maintenance/documents/new";
        }

        try {
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
            document.setVehicle(vehicle);
            document.setDocType(normalizeDocType(document.getDocType()));
            document.setUploadedAt(java.time.LocalDate.now());
            saveUploadedDocumentFile(document, documentFile, vehicleId, document.getDocType());

            maintenanceService.addDocument(document);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Vehicle document successfully recorded for " + vehicle.getModel() + " (" + vehicle.getRegNo() + ").");
            return "redirect:/maintenance/fleet";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save document: " + e.getMessage());
            return "redirect:/maintenance/documents/new";
        }
    }

    @GetMapping("/documents/{id}/edit")
    public String showEditDocumentForm(@PathVariable Long id, Model model) {
        VehicleDocument document = maintenanceService.getDocumentById(id);
        model.addAttribute("document", document);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "maintenance/document-form";
    }

    @PostMapping("/documents/{id}")
    public String updateDocument(
            @PathVariable Long id,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "documentFile", required = false) org.springframework.web.multipart.MultipartFile documentFile,
            @ModelAttribute VehicleDocument document,
            RedirectAttributes redirectAttributes) {

        try {
            VehicleDocument existing = maintenanceService.getDocumentById(id);
            if (vehicleId != null) {
                Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
                existing.setVehicle(vehicle);
            }
            if (document.getDocType() != null && !document.getDocType().isBlank()) {
                existing.setDocType(normalizeDocType(document.getDocType()));
            }
            if (document.getExpiryDate() != null) {
                existing.setExpiryDate(document.getExpiryDate());
            }

            if (documentFile != null && !documentFile.isEmpty()) {
                saveUploadedDocumentFile(existing, documentFile, existing.getVehicle() != null ? existing.getVehicle().getVehicleId() : id, existing.getDocType());
            }

            maintenanceService.updateDocument(id, existing);
            redirectAttributes.addFlashAttribute("successMessage", "Document successfully updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not update document: " + e.getMessage());
        }
        return "redirect:/maintenance/fleet";
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteVehicleDocumentPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return deleteVehicleDocument(id, redirectAttributes);
    }

    @GetMapping("/documents/{id}/delete")
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
