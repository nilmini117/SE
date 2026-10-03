package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackApiController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) return false;
        boolean hasStaffRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"));
        if (hasStaffRole) return true;
        return staffRepository.findByEmail(authentication.getName()).isPresent();
    }

    /**
     * Public feedback reviews endpoint for public vehicle catalog page.
     */
    @GetMapping("/public")
    public List<Map<String, Object>> getPublicFeedback() {
        return feedbackService.getPubliclyVisibleFeedback().stream().map(f -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("category", f.getCategory());
            map.put("message", f.getMessage());
            map.put("date", f.getDate());
            map.put("customerName", f.getCustomer() != null ? f.getCustomer().getName() : "Verified Renter");
            if (f.getBooking() != null && f.getBooking().getVehicle() != null) {
                map.put("vehicleModel", f.getBooking().getVehicle().getModel());
                map.put("vehicleBrand", f.getBooking().getVehicle().getBrand());
            }
            return map;
        }).toList();
    }

    /**
     * PUT Endpoint to alter feedback text.
     * Strict requirement: A staff-role JWT/session attempting to alter feedback text is rejected with 403 Forbidden.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFeedbackText(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload,
            Authentication authentication) {
        if (authentication != null && isStaff(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "error", "Forbidden", "message", "Staff members have strictly read-only access to feedback text."));
        }

        Feedback existing = feedbackService.getFeedbackById(id);
        if (existing.isApproved()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", 400, "error", "Bad Request", "message", "Approved feedback is locked from editing."));
        }

        if (authentication != null) {
            String email = authentication.getName();
            if (existing.getCustomer() == null || !existing.getCustomer().getEmail().equalsIgnoreCase(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", 403, "error", "Forbidden", "message", "You are not authorized to edit this feedback."));
            }
        }

        if (payload != null) {
            if (payload.containsKey("category")) {
                existing.setCategory((String) payload.get("category"));
            }
            if (payload.containsKey("message")) {
                existing.setMessage((String) payload.get("message"));
            }
        }
        Feedback saved = feedbackService.save(existing);
        return ResponseEntity.ok(saved);
    }

    /**
     * Staff toggle public visibility.
     */
    @PostMapping("/{id}/toggle-visibility")
    public ResponseEntity<?> toggleVisibility(@PathVariable Long id, Authentication authentication) {
        if (!isStaff(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "error", "Forbidden", "message", "Only staff can toggle visibility."));
        }
        Feedback updated = feedbackService.togglePublicVisibility(id);
        return ResponseEntity.ok(updated);
    }
}
