package com.driveflow.demo_driveflow.booking;

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

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for Booking operations.
 * Exposes transactional endpoint POST /api/bookings/{id}/return
 * to automate inventory release and unlock the feedback module.
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

    /**
     * Transactional Customer Vehicle Return Action:
     * Automates inventory release (flips vehicle to AVAILABLE) and unlocks the feedback module.
     * POST /api/bookings/{id}/return
     */
    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnVehicle(@PathVariable("id") Long id, Authentication authentication) {
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
        if (!staff) {
            String email = authentication.getName();
            Customer customer = customerRepository.findByEmail(email).orElse(null);
            if (customer == null || booking.getCustomer() == null || !booking.getCustomer().getSystemId().equals(customer.getSystemId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "You are not authorized to return this vehicle reservation."));
            }
        }

        try {
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
            response.put("message", "Vehicle successfully returned! Inventory released to AVAILABLE. Feedback module unlocked.");

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to return vehicle: " + e.getMessage()));
        }
    }
}
