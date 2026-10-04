package com.driveflow.demo_driveflow;

import com.driveflow.demo_driveflow.feedback.Feedback;
import com.driveflow.demo_driveflow.feedback.FeedbackService;
import com.driveflow.demo_driveflow.maintenance.MaintenanceCompany;
import com.driveflow.demo_driveflow.maintenance.MaintenanceCompanyService;
import com.driveflow.demo_driveflow.promotion.Promotion;
import com.driveflow.demo_driveflow.promotion.PromotionRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired(required = false)
    private VehicleService vehicleService;

    @Autowired(required = false)
    private PromotionRepository promotionRepository;

    @Autowired(required = false)
    private MaintenanceCompanyService maintenanceCompanyService;

    @Autowired(required = false)
    private FeedbackService feedbackService;

    @GetMapping("/")
    public String home(Model model) {
        if (vehicleService != null) {
            try {
                // Strictly filter to verified fleet only (isRegistered = true)
                List<Vehicle> availableVehicles = vehicleService.getDashboardVehicles();
                if (availableVehicles == null || availableVehicles.isEmpty()) {
                    availableVehicles = vehicleService.searchVehicles("", "AVAILABLE").stream()
                            .filter(v -> v.getIsRegistered() != null && v.getIsRegistered())
                            .toList();
                }
                model.addAttribute("vehicles", availableVehicles);
                model.addAttribute("brands", vehicleService.getAllBrands());
            } catch (Exception e) {
                log.warn("Could not load vehicles for the home page", e);
            }
        }

        // Active Promotions Feed
        if (promotionRepository != null) {
            try {
                List<Promotion> activePromotions = promotionRepository.findActivePromotions(LocalDate.now());
                if (activePromotions.isEmpty()) {
                    activePromotions = promotionRepository.findByStatusIgnoreCase("ACTIVE");
                }
                model.addAttribute("activePromotions", activePromotions);
            } catch (Exception e) {
                log.warn("Could not load promotions for the home page", e);
            }
        }

        // Partnered Maintenance Companies
        if (maintenanceCompanyService != null) {
            try {
                List<MaintenanceCompany> maintenanceCompanies = maintenanceCompanyService.getAllCompanies();
                model.addAttribute("maintenanceCompanies", maintenanceCompanies);
            } catch (Exception e) {
                log.warn("Could not load maintenance companies for the home page", e);
            }
        }

        // Approved & Accepted Feedback tied to each vehicle
        if (feedbackService != null) {
            try {
                List<Feedback> approvedList = feedbackService.getApprovedOrAcceptedFeedback();
                Map<Long, List<Feedback>> vehicleFeedbackMap = new HashMap<>();
                for (Feedback f : approvedList) {
                    if (f.getBooking() != null && f.getBooking().getVehicle() != null) {
                        Long vId = f.getBooking().getVehicle().getVehicleId();
                        vehicleFeedbackMap.computeIfAbsent(vId, k -> new ArrayList<>()).add(f);
                    }
                }
                model.addAttribute("vehicleFeedbackMap", vehicleFeedbackMap);
                model.addAttribute("allApprovedFeedback", approvedList);
            } catch (Exception e) {
                log.warn("Could not load approved feedback for the home page", e);
            }
        }

        return "index";
    }

    @GetMapping("/api/promotions")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getActivePromotionsApi() {
        if (promotionRepository == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Promotion> list = promotionRepository.findActivePromotions(LocalDate.now());
        if (list.isEmpty()) {
            list = promotionRepository.findByStatusIgnoreCase("ACTIVE");
        }
        List<Map<String, Object>> result = list.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("promotionId", p.getPromotionId());
            map.put("title", p.getTitle());
            map.put("couponId", p.getCouponId());
            map.put("couponCode", p.getCouponCode());
            map.put("discountRate", p.getDiscountRate());
            map.put("startDate", p.getStartDate());
            map.put("endDate", p.getEndDate());
            map.put("status", p.getStatus());
            map.put("vehicleCategory", p.getVehicleCategory());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/maintenance-companies")
    @ResponseBody
    public ResponseEntity<List<MaintenanceCompany>> getMaintenanceCompaniesApi() {
        if (maintenanceCompanyService == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(maintenanceCompanyService.getAllCompanies());
    }

    @GetMapping("/api/maintenance-partners")
    @ResponseBody
    public ResponseEntity<List<MaintenanceCompany>> getMaintenancePartnersApi() {
        return getMaintenanceCompaniesApi();
    }

    @GetMapping("/api/feedback/approved")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getApprovedFeedbackApi() {
        if (feedbackService == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Feedback> list = feedbackService.getApprovedOrAcceptedFeedback();
        List<Map<String, Object>> result = list.stream().map(f -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("feedbackId", f.getFeedbackId());
            map.put("customerName", f.getCustomer() != null ? f.getCustomer().getName() : "Verified Customer");
            map.put("category", f.getCategory());
            map.put("message", f.getMessage());
            map.put("date", f.getDate());
            map.put("approvalStatus", f.getApprovalStatus());
            if (f.getBooking() != null && f.getBooking().getVehicle() != null) {
                map.put("vehicleId", f.getBooking().getVehicle().getVehicleId());
                map.put("vehicleModel", f.getBooking().getVehicle().getModel());
            }
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/booking")
    public String bookingPage(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return "redirect:/bookings/new" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
    }

    @GetMapping("/incident")
    public String incidentPage() {
        return "redirect:/incidents/report";
    }
}
