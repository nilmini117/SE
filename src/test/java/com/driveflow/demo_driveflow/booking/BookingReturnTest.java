package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.feedback.Feedback;
import com.driveflow.demo_driveflow.feedback.FeedbackRepository;
import com.driveflow.demo_driveflow.feedback.FeedbackServiceImpl;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Backend Unit Tests for Customer "Return Vehicle" action:
 * Proves that returning a booking successfully flips the associated vehicle's status back to AVAILABLE,
 * updates booking state to RETURNED, and unlocks the feedback module.
 */
@ExtendWith(MockitoExtension.class)
public class BookingReturnTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Customer testCustomer;
    private Vehicle testVehicle;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setSystemId(42L);
        testCustomer.setFirstName("Sunil");
        testCustomer.setLastName("Perera");
        testCustomer.setEmail("sunil.perera@example.com");

        testVehicle = new Vehicle();
        testVehicle.setVehicleId(101L);
        testVehicle.setModel("Toyota Corolla Axio");
        testVehicle.setRegNo("WP CAB-8899");
        testVehicle.setStatus("BOOKED");

        testBooking = new Booking();
        testBooking.setBookingId(701L);
        testBooking.setCustomer(testCustomer);
        testBooking.setVehicle(testVehicle);
        testBooking.setBookingDate(LocalDate.now().minusDays(3));
        testBooking.setEndDate(LocalDate.now());
        testBooking.setChargedRate(BigDecimal.valueOf(15000.00));
        testBooking.setStatus("CONFIRMED");
    }

    @Test
    @DisplayName("Unit Test: Returning a CONFIRMED booking flips vehicle status back to AVAILABLE and booking to RETURNED")
    void testReturnVehicle_FlipsVehicleStatusToAvailable() {
        // Arrange
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Booking returnedBooking = bookingService.returnVehicle(701L);

        // Assert - Booking state
        assertNotNull(returnedBooking, "Returned booking must not be null");
        assertEquals("RETURNED", returnedBooking.getStatus(), "Booking status must be updated to RETURNED");
        assertTrue(returnedBooking.getStaffMessage().contains("Vehicle successfully returned by customer"));

        // Assert - Vehicle state (Inventory Release)
        Vehicle vehicle = returnedBooking.getVehicle();
        assertNotNull(vehicle, "Associated vehicle must not be null");
        assertEquals("AVAILABLE", vehicle.getStatus(), "Vehicle status must immediately flip back to AVAILABLE");
        assertEquals("AVAILABLE", vehicle.getOperationalStatus(), "Vehicle operationalStatus must be AVAILABLE");

        // Verify database interactions
        verify(vehicleRepository, times(1)).save(testVehicle);
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    @DisplayName("Unit Test: Returning an ACTIVE booking also flips vehicle status back to AVAILABLE and booking to RETURNED")
    void testReturnVehicle_SucceedsForActiveBooking() {
        // Arrange
        testBooking.setStatus("ACTIVE");
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Booking result = bookingService.returnVehicle(701L);

        // Assert
        assertEquals("RETURNED", result.getStatus());
        assertEquals("AVAILABLE", testVehicle.getStatus());
        assertEquals("AVAILABLE", testVehicle.getOperationalStatus());
        verify(vehicleRepository, times(1)).save(testVehicle);
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    @DisplayName("Visibility & State Constraint: Return vehicle is rejected if booking is in PENDING state")
    void testReturnVehicle_ThrowsException_WhenBookingIsPending() {
        // Arrange
        testBooking.setStatus("PENDING");
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bookingService.returnVehicle(701L)
        );

        assertTrue(ex.getMessage().contains("Only bookings in CONFIRMED or ACTIVE status can be returned"));
        assertEquals("BOOKED", testVehicle.getStatus(), "Vehicle status must NOT change");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Visibility & State Constraint: Return vehicle is rejected if booking is in CANCELLED state")
    void testReturnVehicle_ThrowsException_WhenBookingIsCancelled() {
        // Arrange
        testBooking.setStatus("CANCELLED");
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bookingService.returnVehicle(701L)
        );

        assertTrue(ex.getMessage().contains("Only bookings in CONFIRMED or ACTIVE status can be returned"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("REST API Controller Test: POST /api/bookings/{id}/return returns 200 OK with AVAILABLE vehicle and unlocked feedback")
    void testBookingApiController_ReturnVehicleEndpoint() {
        // Arrange BookingApiController with mocks
        BookingApiController controller = new BookingApiController();

        // Inject dependencies using reflection
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "bookingService", bookingService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "customerRepository", customerRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "staffRepository", staffRepository);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("sunil.perera@example.com");
        when(customerRepository.findByEmail("sunil.perera@example.com")).thenReturn(Optional.of(testCustomer));
        when(staffRepository.findByEmail("sunil.perera@example.com")).thenReturn(Optional.empty());

        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ResponseEntity<?> response = controller.returnVehicle(701L, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals(true, body.get("success"));
        assertEquals(701L, body.get("bookingId"));
        assertEquals("RETURNED", body.get("bookingStatus"));
        assertEquals("AVAILABLE", body.get("vehicleStatus"));
        assertEquals("AVAILABLE", body.get("operationalStatus"));
        assertEquals(true, body.get("feedbackUnlocked"));
        assertEquals("/feedback/new?bookingId=701", body.get("feedbackUrl"));
    }

    @Test
    @DisplayName("Feedback Lifecycle Integration: Vehicle return unlocks customer feedback submission for the specific booking")
    void testReturnVehicle_UnlocksFeedbackModule() {
        // Step 1: Return the vehicle
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking returnedBooking = bookingService.returnVehicle(701L);
        assertEquals("RETURNED", returnedBooking.getStatus());

        // Step 2: Test FeedbackServiceImpl with the newly RETURNED booking
        FeedbackServiceImpl feedbackService = new FeedbackServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(feedbackService, "feedbackRepository", feedbackRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(feedbackService, "bookingService", bookingService);

        when(bookingRepository.findCompletedBookingsByCustomerId(42L)).thenReturn(List.of(returnedBooking));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act: Submit customer feedback for this returned booking
        Feedback newFeedback = new Feedback();
        newFeedback.setCategory("VEHICLE");
        newFeedback.setMessage("The Toyota Corolla was exceptionally clean and smooth on the highway!");

        Feedback submitted = feedbackService.submitCustomerFeedback(newFeedback, testCustomer, 701L);

        // Assert: Feedback was successfully submitted and bound to the returned booking
        assertNotNull(submitted);
        assertEquals("OPEN", submitted.getStatus());
        assertEquals("PENDING", submitted.getApprovalStatus());
        assertEquals(returnedBooking, submitted.getBooking());
        assertEquals(testCustomer, submitted.getCustomer());
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    @DisplayName("Backend Alignment: BookingStatus Enum includes RETURNED and all valid lifecycle states")
    void testBookingStatusEnum_ContainsAllValidStatesIncludingReturned() {
        // Assert all required enum states are present
        assertNotNull(BookingStatus.valueOf("RETURNED"));
        assertNotNull(BookingStatus.valueOf("PENDING"));
        assertNotNull(BookingStatus.valueOf("APPROVED"));
        assertNotNull(BookingStatus.valueOf("CONFIRMED"));
        assertNotNull(BookingStatus.valueOf("ACTIVE"));
        assertNotNull(BookingStatus.valueOf("COMPLETED"));
        assertNotNull(BookingStatus.valueOf("CANCELLED"));

        // Validation helpers
        assertTrue(BookingStatus.isValid("RETURNED"));
        assertTrue(BookingStatus.isValid("returned"));
        assertTrue(BookingStatus.isValid("CONFIRMED"));
        assertTrue(BookingStatus.isValid("PENDING"));
        assertFalse(BookingStatus.isValid("UNKNOWN_STATUS"));
        assertFalse(BookingStatus.isValid(null));

        // String to Enum resolution
        assertEquals(BookingStatus.RETURNED, BookingStatus.fromString("RETURNED"));
        assertEquals(BookingStatus.RETURNED, BookingStatus.fromString("returned"));
        assertEquals("RETURNED", BookingStatus.STATUS_RETURNED);
    }

    @Test
    @DisplayName("Backend Alignment: Booking entity serializes and deserializes BookingStatus correctly")
    void testBookingEntity_SupportsBookingStatusEnum() {
        Booking b = new Booking();
        b.setStatus(BookingStatus.RETURNED);
        assertEquals("RETURNED", b.getStatus());
        assertEquals(BookingStatus.RETURNED, b.getBookingStatus());

        b.setBookingStatus(BookingStatus.CONFIRMED);
        assertEquals("CONFIRMED", b.getStatus());
        assertEquals(BookingStatus.CONFIRMED, b.getBookingStatus());

        b.setStatus("RETURNED");
        assertEquals(BookingStatus.RETURNED, b.getBookingStatus());
    }

    @Test
    @DisplayName("Backend Service: Staff updateBookingStatus to RETURNED succeeds and releases vehicle to AVAILABLE")
    void testUpdateBookingStatus_WithReturned_ReleasesVehicleToAvailable() {
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(testBooking));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking updated = bookingService.updateBookingStatus(701L, "RETURNED");

        assertEquals("RETURNED", updated.getStatus());
        assertEquals("AVAILABLE", testVehicle.getStatus());
        assertEquals("AVAILABLE", testVehicle.getOperationalStatus());
        verify(vehicleRepository, times(1)).save(testVehicle);
        verify(bookingRepository, times(1)).save(testBooking);
    }
}
