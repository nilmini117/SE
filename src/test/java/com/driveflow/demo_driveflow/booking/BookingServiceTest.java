package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.booking.exception.ActiveBookingLimitExceededException;
import com.driveflow.demo_driveflow.booking.exception.BookingCancellationNotAllowedException;
import com.driveflow.demo_driveflow.booking.exception.BookingImmutabilityException;
import com.driveflow.demo_driveflow.booking.exception.BranchSelectionRequiredException;
import com.driveflow.demo_driveflow.booking.pricing.PaymentBreakdownItem;
import com.driveflow.demo_driveflow.booking.pricing.PricingBreakdown;
import com.driveflow.demo_driveflow.booking.pricing.PricingEngineService;
import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PricingEngineService pricingEngineService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Customer customer;
    private Branch branch;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setSystemId(101L);
        customer.setFirstName("Kasun");
        customer.setLastName("Silva");
        customer.setEmail("kasun.silva@driveflow.com");
        customer.setDrivingLicense("B123456");

        branch = new Branch();
        branch.setBranchId(1L);
        branch.setBranchName("Colombo Central");
        branch.setCity("Colombo");
        branch.setStreet("Galle Road");

        vehicle = new Vehicle();
        vehicle.setVehicleId(501L);
        vehicle.setModel("Toyota Prius");
        vehicle.setRegNo("WP CA-1020");
        vehicle.setColor("Pearl White");
        vehicle.setStatus("AVAILABLE");
        vehicle.setBranch(branch);
    }

    @Test
    @DisplayName("Should throw ActiveBookingLimitExceededException when user attempts to book a second car while one is already active")
    void testCreateBooking_ThrowsLimitExceededException_WhenUserAttemptsToBookSecondCarWhileOneIsActive() {
        // Arrange: Customer already has 1 currently active booking (PENDING, CONFIRMED, or APPROVED)
        Booking secondBookingAttempt = new Booking();
        secondBookingAttempt.setCustomer(customer);
        secondBookingAttempt.setPickupBranch(branch);
        secondBookingAttempt.setVehicle(vehicle);
        secondBookingAttempt.setBookingDate(LocalDate.now());
        secondBookingAttempt.setEndDate(LocalDate.now().plusDays(3));

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        // Concurrency limit simulation: Database returns 1 active booking for this customer
        when(bookingRepository.countActiveBookingsByCustomerId(101L)).thenReturn(1L);

        // Act & Assert: Transaction MUST be blocked and throw ActiveBookingLimitExceededException
        ActiveBookingLimitExceededException exception = assertThrows(
                ActiveBookingLimitExceededException.class,
                () -> bookingService.createBooking(secondBookingAttempt),
                "Should throw ActiveBookingLimitExceededException when active bookings > 0"
        );

        assertTrue(exception.getMessage().contains("Active booking limit exceeded"),
                "Exception message must mention active booking limit exceeded");
        assertTrue(exception.getMessage().contains("A single customer can only book exactly one vehicle at a time"),
                "Exception message must specify single vehicle restriction");

        // Verify that the second booking is NEVER saved to the database
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should successfully create booking when customer has 0 active bookings and valid pickup branch")
    void testCreateBooking_Succeeds_WhenCustomerHasZeroActiveBookings() {
        // Arrange: Customer has 0 active bookings
        Booking newBooking = new Booking();
        newBooking.setCustomer(customer);
        newBooking.setPickupBranch(branch);
        newBooking.setVehicle(vehicle);
        newBooking.setBookingDate(LocalDate.now());
        newBooking.setEndDate(LocalDate.now().plusDays(2));

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(bookingRepository.countActiveBookingsByCustomerId(101L)).thenReturn(0L);
        when(vehicleRepository.findById(501L)).thenReturn(Optional.of(vehicle));
        when(bookingRepository.countOverlappingActiveBookings(eq(501L), any(), any())).thenReturn(0L);

        PricingBreakdown mockPricing = PricingBreakdown.builder()
                .baseDailyRate(new BigDecimal("3000.00"))
                .durationDays(2)
                .baseCost(new BigDecimal("6000.00"))
                .discountRate(new BigDecimal("15.00"))
                .discountAmount(new BigDecimal("900.00"))
                .finalTotalCost(new BigDecimal("5100.00"))
                .promotionApplied(true)
                .paymentBreakdownArray(new ArrayList<>(List.of(
                        new PaymentBreakdownItem("BASE_RENTAL", "Base Rental Rate", new BigDecimal("6000.00"), "CHARGE"),
                        new PaymentBreakdownItem("PROMOTION_DISCOUNT", "Summer Discount (-15%)", new BigDecimal("-900.00"), "CREDIT"),
                        new PaymentBreakdownItem("TOTAL_DUE", "Final Calculated Cost", new BigDecimal("5100.00"), "TOTAL")
                )))
                .build();

        when(pricingEngineService.calculatePricing(eq(501L), eq(1L), any(), any(), isNull()))
                .thenReturn(mockPricing);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setBookingId(999L);
            return b;
        });

        // Act
        Booking saved = bookingService.createBooking(newBooking);

        // Assert
        assertNotNull(saved);
        assertEquals("PENDING", saved.getStatus(), "Initial booking status must strictly be PENDING (payment gate locked)");
        assertEquals(new BigDecimal("5100.00"), saved.getChargedRate(), "Charged rate should reflect pricing engine calculated total");
        assertEquals(1, saved.getQuantity(), "Quantity must strictly be 1 vehicle per reservation");
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BranchSelectionRequiredException when pickup branch is null")
    void testCreateBooking_ThrowsException_WhenPickupBranchMissing() {
        Booking invalidBooking = new Booking();
        invalidBooking.setCustomer(customer);
        invalidBooking.setPickupBranch(null); // Missing branch
        invalidBooking.setVehicle(vehicle);

        BranchSelectionRequiredException ex = assertThrows(
                BranchSelectionRequiredException.class,
                () -> bookingService.createBooking(invalidBooking)
        );

        assertTrue(ex.getMessage().contains("Branch Selection Required"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BookingImmutabilityException when customer attempts to edit an existing booking record")
    void testUpdateBooking_ThrowsBookingImmutabilityException() {
        Booking updatePayload = new Booking();
        updatePayload.setBookingDate(LocalDate.now().plusDays(10));

        BookingImmutabilityException ex = assertThrows(
                BookingImmutabilityException.class,
                () -> bookingService.updateBooking(100L, updatePayload)
        );

        assertTrue(ex.getMessage().contains("immutable"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancellation Flow: Should allow cancellation when booking status is PENDING prior to staff approval")
    void testCancelBooking_Succeeds_WhenStatusIsPending() {
        Booking pendingBooking = new Booking();
        pendingBooking.setBookingId(200L);
        pendingBooking.setStatus("PENDING");
        pendingBooking.setCustomer(customer);
        pendingBooking.setVehicle(vehicle);

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(200L);

        assertEquals("CANCELLED", pendingBooking.getStatus(), "Status should transition to CANCELLED");
        verify(bookingRepository, times(1)).save(pendingBooking);
    }

    @Test
    @DisplayName("Cancellation Flow: Should block and throw BookingCancellationNotAllowedException when booking is APPROVED or CONFIRMED")
    void testCancelBooking_ThrowsException_WhenStatusIsApprovedOrConfirmed() {
        Booking confirmedBooking = new Booking();
        confirmedBooking.setBookingId(300L);
        confirmedBooking.setStatus("CONFIRMED"); // Or APPROVED
        confirmedBooking.setCustomer(customer);
        confirmedBooking.setVehicle(vehicle);

        when(bookingRepository.findById(300L)).thenReturn(Optional.of(confirmedBooking));

        BookingCancellationNotAllowedException ex = assertThrows(
                BookingCancellationNotAllowedException.class,
                () -> bookingService.cancelBooking(300L)
        );

        assertTrue(ex.getMessage().contains("Cancellation prohibited"));
        assertTrue(ex.getMessage().contains("CONFIRMED"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Staff Decline Constraint: Should throw IllegalStateException when staff tries to decline an approved booking")
    void testDeclineBooking_ThrowsIllegalStateException_WhenBookingIsApprovedOrConfirmed() {
        Booking confirmedBooking = new Booking();
        confirmedBooking.setBookingId(400L);
        confirmedBooking.setStatus("CONFIRMED");
        confirmedBooking.setCustomer(customer);
        confirmedBooking.setVehicle(vehicle);

        when(bookingRepository.findById(400L)).thenReturn(Optional.of(confirmedBooking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.declineBooking(400L, "Staff decline attempt on confirmed")
        );

        assertTrue(ex.getMessage().contains("cannot decline an approved booking"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Staff Decline Constraint: Should throw IllegalStateException when staff tries to decline a paid booking")
    void testDeclineBooking_ThrowsIllegalStateException_WhenInvoiceIsPaid() {
        Booking pendingBooking = new Booking();
        pendingBooking.setBookingId(401L);
        pendingBooking.setStatus("PENDING");
        pendingBooking.setCustomer(customer);
        pendingBooking.setVehicle(vehicle);

        Invoice paidInvoice = new Invoice();
        paidInvoice.setInvoiceId(701L);
        paidInvoice.setStatus("PAID");

        when(bookingRepository.findById(401L)).thenReturn(Optional.of(pendingBooking));
        when(invoiceRepository.findByBooking(pendingBooking)).thenReturn(Optional.of(paidInvoice));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.declineBooking(401L, "Decline paid booking")
        );

        assertTrue(ex.getMessage().contains("cannot decline a paid booking"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Staff Decline: Should succeed when booking is pending and unpaid")
    void testDeclineBooking_Succeeds_WhenBookingIsPendingAndUnpaid() {
        Booking pendingBooking = new Booking();
        pendingBooking.setBookingId(402L);
        pendingBooking.setStatus("PENDING");
        pendingBooking.setCustomer(customer);
        pendingBooking.setVehicle(vehicle);

        when(bookingRepository.findById(402L)).thenReturn(Optional.of(pendingBooking));
        when(invoiceRepository.findByBooking(pendingBooking)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.declineBooking(402L, "Vehicle maintenance required");

        assertEquals("CANCELLED", pendingBooking.getStatus());
        assertEquals("Vehicle maintenance required", pendingBooking.getStaffMessage());
        verify(bookingRepository, times(1)).save(pendingBooking);
    }

    @Test
    @DisplayName("Staff Status Edit Constraint: Should throw IllegalStateException when staff tries to edit status of a paid booking")
    void testUpdateBookingStatus_ThrowsIllegalStateException_WhenInvoiceIsPaid() {
        Booking booking = new Booking();
        booking.setBookingId(500L);
        booking.setStatus("CONFIRMED");
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);

        Invoice paidInvoice = new Invoice();
        paidInvoice.setInvoiceId(801L);
        paidInvoice.setStatus("PAID");

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBooking(booking)).thenReturn(Optional.of(paidInvoice));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.updateBookingStatus(500L, "CANCELLED")
        );

        assertTrue(ex.getMessage().contains("cannot be edited: Customer has already paid"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Staff Status Edit: Should allow staff to edit status when booking is NOT paid")
    void testUpdateBookingStatus_Succeeds_WhenInvoiceIsNotPaid() {
        Booking booking = new Booking();
        booking.setBookingId(501L);
        booking.setStatus("CONFIRMED");
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);

        Invoice unpaidInvoice = new Invoice();
        unpaidInvoice.setInvoiceId(802L);
        unpaidInvoice.setStatus("UNPAID");

        when(bookingRepository.findById(501L)).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBooking(booking)).thenReturn(Optional.of(unpaidInvoice));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking updated = bookingService.updateBookingStatus(501L, "COMPLETED");

        assertEquals("COMPLETED", updated.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }
}
