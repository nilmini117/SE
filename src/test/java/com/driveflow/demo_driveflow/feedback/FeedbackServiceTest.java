package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private Customer sampleCustomer;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setSystemId(10L);
        sampleCustomer.setFirstName("Jane");
        sampleCustomer.setLastName("Doe");
        sampleCustomer.setEmail("jane@driveflow.com");

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(5L);
        vehicle.setBrand("Toyota");
        vehicle.setModel("Corolla");

        completedBooking = new Booking();
        completedBooking.setBookingId(20L);
        completedBooking.setStatus("COMPLETED");
        completedBooking.setCustomer(sampleCustomer);
        completedBooking.setVehicle(vehicle);
    }

    @Test
    @DisplayName("Submission Lock: Submitting feedback without a completed booking throws IllegalStateException")
    void submitCustomerFeedback_withoutCompletedBooking_throwsException() {
        when(bookingService.getCompletedBookingsForCustomer(10L)).thenReturn(Collections.emptyList());

        Feedback feedback = new Feedback();
        feedback.setCategory("SERVICE");
        feedback.setMessage("Loved the car");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                feedbackService.submitCustomerFeedback(feedback, sampleCustomer, null)
        );

        assertTrue(ex.getMessage().contains("Feedback submission is locked"));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Submitting feedback with a completed booking links booking, sets PENDING approval and private visibility")
    void submitCustomerFeedback_withCompletedBooking_savesSuccessfully() {
        when(bookingService.getCompletedBookingsForCustomer(10L)).thenReturn(List.of(completedBooking));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Feedback feedback = new Feedback();
        feedback.setCategory("VEHICLE");
        feedback.setMessage("Great vehicle performance.");

        Feedback saved = feedbackService.submitCustomerFeedback(feedback, sampleCustomer, null);

        assertNotNull(saved);
        assertEquals("PENDING", saved.getApprovalStatus());
        assertFalse(saved.isPublicVisibility());
        assertEquals("VEHICLE", saved.getCategory());
        assertEquals(completedBooking, saved.getBooking());
        assertEquals(sampleCustomer, saved.getCustomer());
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    @DisplayName("Customer can edit unapproved feedback")
    void updateCustomerFeedback_whenUnapproved_updatesMessageAndCategory() {
        Feedback existing = new Feedback();
        existing.setFeedbackId(1L);
        existing.setCustomer(sampleCustomer);
        existing.setApprovalStatus("PENDING");
        existing.setPublicVisibility(false);
        existing.setCategory("SERVICE");
        existing.setMessage("Initial draft");

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Feedback updateData = new Feedback();
        updateData.setMessage("Updated message");
        updateData.setCategory("PRICING");

        Feedback updated = feedbackService.updateCustomerFeedback(1L, updateData, sampleCustomer);

        assertEquals("Updated message", updated.getMessage());
        assertEquals("PRICING", updated.getCategory());
        verify(feedbackRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Edit Lock: Editing feedback already approved by staff throws IllegalStateException")
    void updateCustomerFeedback_whenApproved_throwsIllegalStateException() {
        Feedback approved = new Feedback();
        approved.setFeedbackId(1L);
        approved.setCustomer(sampleCustomer);
        approved.setApprovalStatus("APPROVED");
        approved.setPublicVisibility(true);

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(approved));

        Feedback updateData = new Feedback();
        updateData.setMessage("Trying to edit approved text");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                feedbackService.updateCustomerFeedback(1L, updateData, sampleCustomer)
        );

        assertTrue(ex.getMessage().contains("Feedback has already been approved by staff and cannot be modified"));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Customer can delete their feedback at any time")
    void deleteCustomerFeedback_succeeds() {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(1L);
        feedback.setCustomer(sampleCustomer);

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        feedbackService.deleteCustomerFeedback(1L, sampleCustomer);

        verify(feedbackRepository, times(1)).delete(feedback);
    }

    @Test
    @DisplayName("Staff can toggle public visibility between true and false, setting approvalStatus to APPROVED")
    void togglePublicVisibility_togglesBooleanAndSetsApproved() {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(1L);
        feedback.setApprovalStatus("PENDING");
        feedback.setPublicVisibility(false);

        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First toggle: publish
        Feedback published = feedbackService.togglePublicVisibility(1L);
        assertTrue(published.isPublicVisibility());
        assertEquals("APPROVED", published.getApprovalStatus());

        // Second toggle: hide
        Feedback hidden = feedbackService.togglePublicVisibility(1L);
        assertFalse(hidden.isPublicVisibility());
        assertEquals("APPROVED", hidden.getApprovalStatus());
    }

    @Test
    @DisplayName("getPubliclyVisibleFeedback returns published catalog reviews")
    void getPubliclyVisibleFeedback_returnsOnlyPublicReviews() {
        Feedback visible = new Feedback();
        visible.setFeedbackId(1L);
        visible.setPublicVisibility(true);
        visible.setApprovalStatus("APPROVED");

        when(feedbackRepository.findByPublicVisibilityTrueAndApprovalStatusIgnoreCaseOrderByDateDesc("APPROVED"))
                .thenReturn(List.of(visible));

        List<Feedback> results = feedbackService.getPubliclyVisibleFeedback();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isPublicVisibility());
    }
}
