package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.Staff;
import com.driveflow.demo_driveflow.users.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class FeedbackSecurityTest {

    private MockMvc feedbackMockMvc;
    private MockMvc apiFeedbackMockMvc;

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private BookingService bookingService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private FeedbackController feedbackController;

    @InjectMocks
    private FeedbackApiController feedbackApiController;

    private Authentication staffAuth;
    private Authentication customerAuth;

    @BeforeEach
    void setUp() {
        feedbackMockMvc = MockMvcBuilders.standaloneSetup(feedbackController).build();
        apiFeedbackMockMvc = MockMvcBuilders.standaloneSetup(feedbackApiController).build();

        staffAuth = new UsernamePasswordAuthenticationToken(
                "staff@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );

        customerAuth = new UsernamePasswordAuthenticationToken(
                "customer@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Test
    @DisplayName("Staff attempting PUT to /feedback/{id} to alter feedback text must be rejected with 403 Forbidden")
    void staffAttemptingPutFeedback_mustBeRejectedWith403Forbidden() throws Exception {
        String payload = """
            {
                "message": "Staff illegally altering customer review text"
            }
            """;

        feedbackMockMvc.perform(put("/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(staffAuth))
                .andExpect(status().isForbidden());

        verify(feedbackService, never()).updateCustomerFeedback(any(), any(), any());
    }

    @Test
    @DisplayName("Staff attempting PUT to /api/feedback/{id} to alter feedback text must be rejected with 403 Forbidden")
    void staffAttemptingPutApiFeedback_mustBeRejectedWith403Forbidden() throws Exception {
        String payload = """
            {
                "message": "Staff illegally altering review via REST API"
            }
            """;

        apiFeedbackMockMvc.perform(put("/api/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(staffAuth))
                .andExpect(status().isForbidden());

        verify(feedbackService, never()).updateCustomerFeedback(any(), any(), any());
    }

    @Test
    @DisplayName("Staff recognized by StaffRepository attempting PUT to /feedback/{id} is rejected with 403 Forbidden")
    void staffRecognizedByRepository_attemptingPut_mustBeRejectedWith403Forbidden() throws Exception {
        Authentication authWithoutRole = new UsernamePasswordAuthenticationToken(
                "manager@driveflow.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(staffRepository.findByEmail("manager@driveflow.com"))
                .thenReturn(Optional.of(new Staff()));

        feedbackMockMvc.perform(put("/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Manager modifying review\"}")
                        .principal(authWithoutRole))
                .andExpect(status().isForbidden());

        verify(feedbackService, never()).updateCustomerFeedback(any(), any(), any());
    }

    @Test
    @DisplayName("Customer attempting to edit already approved feedback must be rejected with 400 Bad Request")
    void customerAttemptingToEditApprovedFeedback_mustBeRejected() throws Exception {
        Customer customer = new Customer();
        customer.setSystemId(100L);
        customer.setEmail("customer@driveflow.com");

        Feedback existing = new Feedback();
        existing.setFeedbackId(1L);
        existing.setCustomer(customer);
        existing.setApprovalStatus("APPROVED");
        existing.setPublicVisibility(true);

        when(feedbackService.getFeedbackById(1L)).thenReturn(existing);

        String payload = """
            {
                "message": "Customer trying to change approved review"
            }
            """;

        feedbackMockMvc.perform(put("/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .principal(customerAuth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Approved feedback is locked and cannot be edited."));
    }

    @Test
    @DisplayName("Customer without a completed booking cannot open feedback form (Submission Locked)")
    void customerWithoutCompletedBooking_submissionFormIsLocked() throws Exception {
        Customer customer = new Customer();
        customer.setSystemId(100L);
        customer.setEmail("customer@driveflow.com");

        when(customerRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        when(bookingService.getLastCompletedBookingForCustomer(100L)).thenReturn(null);

        feedbackMockMvc.perform(get("/feedback/new").principal(customerAuth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Staff is forbidden from accessing the feedback submission form")
    void staffCannotAccessFeedbackSubmissionForm() throws Exception {
        feedbackMockMvc.perform(get("/feedback/new").principal(staffAuth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Customer can delete their own feedback at any time")
    void customerCanDeleteFeedbackAtAnyTime() throws Exception {
        Customer customer = new Customer();
        customer.setSystemId(100L);
        customer.setEmail("customer@driveflow.com");

        when(customerRepository.findByEmail("customer@driveflow.com")).thenReturn(Optional.of(customer));
        doNothing().when(feedbackService).deleteCustomerFeedback(1L, customer);

        feedbackMockMvc.perform(post("/feedback/1/delete").principal(customerAuth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(feedbackService, times(1)).deleteCustomerFeedback(1L, customer);
    }

    @Test
    @DisplayName("Staff can toggle public visibility to approve and publish feedback to the catalog")
    void staffCanTogglePublicVisibility() throws Exception {
        Feedback approvedFeedback = new Feedback();
        approvedFeedback.setFeedbackId(1L);
        approvedFeedback.setApprovalStatus("APPROVED");
        approvedFeedback.setPublicVisibility(true);

        when(feedbackService.togglePublicVisibility(1L)).thenReturn(approvedFeedback);

        feedbackMockMvc.perform(post("/feedback/1/toggle-visibility").principal(staffAuth))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(feedbackService, times(1)).togglePublicVisibility(1L);
    }
}
