package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.otp.OtpService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for Booking operations.
 * Exposes endpoints:
 * - POST /api/bookings/{id}/return/request-otp : Dispatches 6-digit return authorization OTP
 * - POST /api/bookings/{id}/return             : Verifies OTP, releases inventory, and informs early return refund policy
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired(required = false)
    private OtpService otpService;

    private boolean isStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        boolean hasStaffRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("STAFF"));
        if (hasStaffRole) {
            return true;
        }
        return staffRepository.findByEmail(authentication.getName()).isPresent();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIdx = email.indexOf('@');
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 2) {
            return name.charAt(0) + "*..." + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    /**
     * Request OTP before returning vehicle:
     * Dispatches a 6-digit numeric OTP to customer's registered email address.
     * Informs customer if this return qualifies as an early return with front-desk refund instructions.
     * POST /api/bookings/{id}/return/request-otp
     */
    @PostMapping("/{id}/return/request-otp")
    public ResponseEntity<?> requestReturnOtp(@PathVariable("id") Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required to request return OTP."));
        }

        Booking booking = bookingService.getBookingById(id);
        if (booking == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Booking #BK-" + id + " not found."));
        }

        String status = booking.getStatus();
        if (!"CONFIRMED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status) && !"APPROVED".equalsIgnoreCase(status)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Vehicle return OTP can only be requested for confirmed or active bookings."));
        }

        boolean staff = isStaff(authentication);
        String email = authentication.getName();
        Customer customer = null;
        if (!staff) {
            customer = customerRepository.findByEmail(email).orElse(null);
            if (customer == null || booking.getCustomer() == null || !booking.getCustomer().getSystemId().equals(customer.getSystemId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You are not authorized to return this vehicle reservation."));
            }
        } else {
            customer = booking.getCustomer();
            if (customer != null) {
                email = customer.getEmail();
            }
        }

        if (customer == null || email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Customer email not found for this booking."));
        }

        boolean isEarlyReturn = booking.getEndDate() != null && LocalDate.now().isBefore(booking.getEndDate());
        String customerName = customer.getFirstName() + (customer.getLastName() != null ? " " + customer.getLastName() : "");

        try {
            if (otpService != null) {
                otpService.generateVehicleReturnOtp(email, customerName, id, isEarlyReturn);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bookingId", id);
            response.put("isEarlyReturn", isEarlyReturn);
            response.put("maskedEmail", maskEmail(email));
            response.put("message", "A 6-digit verification code has been sent to your email (" + maskEmail(email) + ").");
            if (isEarlyReturn) {
                response.put("refundNotice", "If you return the vehicle before the scheduled end date, refund money can be collected from the branch front desk after giving the car key to the staff.");
            }
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode())
                    .body(Map.of("success", false, "message", rse.getReason()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to dispatch verification code: " + ex.getMessage()));
        }
    }

    /**
     * Transactional Customer Vehicle Return Action:
     * Validates 6-digit OTP, releases vehicle inventory to AVAILABLE, unlocks feedback,
     * and informs early return refund notice if returned before end date.
     * POST /api/bookings/{id}/return
     */
    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnVehicle(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> payload,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Authentication required to return vehicle."));
        }

        Booking booking = bookingService.getBookingById(id);
        if (booking == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Booking #BK-" + id + " not found."));
        }

        // Authorization: Verify booking belongs to authenticated customer unless caller is staff
        boolean staff = isStaff(authentication);
        String customerEmail = null;
        if (!staff) {
            String email = authentication.getName();
            Customer customer = customerRepository.findByEmail(email).orElse(null);
            if (customer == null || booking.getCustomer() == null || !booking.getCustomer().getSystemId().equals(customer.getSystemId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You are not authorized to return this vehicle reservation."));
            }
            customerEmail = customer.getEmail();
        } else if (booking.getCustomer() != null) {
            customerEmail = booking.getCustomer().getEmail();
        }

        // Enforce OTP validation for non-staff returns when OtpService is present
        if (!staff && otpService != null) {
            String otp = payload != null ? payload.get("otp") : null;
            if (otp == null || otp.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "OTP verification code is required to authorize vehicle return."));
            }
            if (!otpService.verifyVehicleReturnOtp(customerEmail, otp.trim())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Invalid or expired OTP code. Please enter the valid code sent to your email."));
            }
        }

        try {
            boolean isEarlyReturn = booking.getEndDate() != null && LocalDate.now().isBefore(booking.getEndDate());
            Booking returnedBooking = bookingService.returnVehicle(id);
            Vehicle vehicle = returnedBooking.getVehicle();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bookingId", returnedBooking.getBookingId());
            response.put("bookingStatus", returnedBooking.getStatus());
            response.put("vehicleId", vehicle != null ? vehicle.getVehicleId() : null);
            response.put("vehicleModel", vehicle != null ? vehicle.getModel() : null);
            response.put("vehicleStatus", vehicle != null ? vehicle.getStatus() : "AVAILABLE");
            response.put("operationalStatus", vehicle != null ? vehicle.getOperationalStatus() : "AVAILABLE");
            response.put("feedbackUnlocked", true);
            response.put("feedbackUrl", "/feedback/new?bookingId=" + returnedBooking.getBookingId());
            response.put("isEarlyReturn", isEarlyReturn);

            String refundNotice = isEarlyReturn
                    ? "If you return the vehicle before the scheduled end date, refund money can be collected from the branch front desk after giving the car key to the staff."
                    : null;
            if (isEarlyReturn) {
                response.put("refundNotice", refundNotice);
                response.put("message", "Vehicle successfully returned! Notice: " + refundNotice + " Inventory released to AVAILABLE. Feedback module unlocked.");
            } else {
                response.put("message", "Vehicle successfully returned! Inventory released to AVAILABLE. Feedback module unlocked.");
            }

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to return vehicle: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> returnVehicle(Long id, Authentication authentication) {
        return returnVehicle(id, null, authentication);
    }
}
