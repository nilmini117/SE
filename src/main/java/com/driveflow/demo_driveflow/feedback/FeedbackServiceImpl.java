package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingService;
import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private BookingService bookingService;

    @Override
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByDateDesc();
    }

    @Override
    public List<Feedback> getFeedbackByCustomer(Customer customer) {
        if (customer == null) return List.of();
        return feedbackRepository.findByCustomerOrderByDateDesc(customer);
    }

    @Override
    public List<Feedback> getPubliclyVisibleFeedback() {
        return feedbackRepository.findByPublicVisibilityTrueAndApprovalStatusIgnoreCaseOrderByDateDesc("APPROVED");
    }

    @Override
    public Feedback getFeedbackById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found with id: " + id));
    }

    @Override
    @Transactional
    public Feedback submitFeedback(Feedback feedback) {
        if (feedback.getStatus() == null || feedback.getStatus().isBlank()) {
            feedback.setStatus("OPEN");
        }
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }
        if (feedback.getApprovalStatus() == null || feedback.getApprovalStatus().isBlank()) {
            feedback.setApprovalStatus("PENDING");
        }
        if (feedback.getPublicVisibility() == null) {
            feedback.setPublicVisibility(false);
        }
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional
    public Feedback submitCustomerFeedback(Feedback feedback, Customer customer, Long bookingId) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer identity is required to submit feedback.");
        }

        // Submission Lock: Customers can only submit feedback after their vehicle has been returned (Booking status = COMPLETED or RETURNED)
        List<Booking> completedBookings = bookingService.getCompletedBookingsForCustomer(customer.getSystemId());
        if (completedBookings.isEmpty()) {
            throw new IllegalStateException("Feedback submission is locked. Customers can only submit feedback after their vehicle has been returned (Booking status: COMPLETED or RETURNED).");
        }

        // Scope: Link specifically to one category under their last/most recent booking
        Booking targetBooking = null;
        if (bookingId != null) {
            Booking requested = bookingService.getBookingById(bookingId);
            if (requested != null && requested.getCustomer() != null && requested.getCustomer().getSystemId().equals(customer.getSystemId())) {
                String s = requested.getStatus();
                if (s == null || (!"COMPLETED".equalsIgnoreCase(s.trim()) && !"RETURNED".equalsIgnoreCase(s.trim()))) {
                    throw new IllegalStateException("Feedback submission is locked for booking #BK-" + bookingId + ". Customers can only submit feedback after their vehicle has been returned (Booking status: COMPLETED or RETURNED).");
                }
                targetBooking = requested;
            } else {
                targetBooking = completedBookings.stream()
                        .filter(b -> b.getBookingId().equals(bookingId))
                        .findFirst()
                        .orElse(completedBookings.get(0));
            }
        } else {
            targetBooking = completedBookings.get(0);
        }

        feedback.setCustomer(customer);
        feedback.setBooking(targetBooking);
        feedback.setStatus("OPEN");
        feedback.setApprovalStatus("PENDING");
        feedback.setPublicVisibility(false);
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }

        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional
    public Feedback updateFeedback(Long id, Feedback updated) {
        Feedback existing = getFeedbackById(id);

        // Edit Lock: Once staff approves it, disable and lock the edit function
        if (existing.isApproved()) {
            throw new IllegalStateException("Feedback has already been approved by staff and cannot be modified.");
        }

        if (updated.getCategory() != null && !updated.getCategory().isBlank()) {
            existing.setCategory(updated.getCategory());
        }
        if (updated.getMessage() != null && !updated.getMessage().isBlank()) {
            existing.setMessage(updated.getMessage());
        }
        return feedbackRepository.save(existing);
    }

    @Override
    @Transactional
    public Feedback updateCustomerFeedback(Long id, Feedback updated, Customer customer) {
        Feedback existing = getFeedbackById(id);

        if (customer != null && existing.getCustomer() != null &&
                !existing.getCustomer().getSystemId().equals(customer.getSystemId())) {
            throw new IllegalArgumentException("You are not authorized to edit this feedback.");
        }

        // Customer can edit feedback only if it has not yet been approved by staff
        if (existing.isApproved()) {
            throw new IllegalStateException("Feedback has already been approved by staff and cannot be modified.");
        }

        if (updated.getCategory() != null && !updated.getCategory().isBlank()) {
            existing.setCategory(updated.getCategory());
        }
        if (updated.getMessage() != null && !updated.getMessage().isBlank()) {
            existing.setMessage(updated.getMessage());
        }
        return feedbackRepository.save(existing);
    }

    @Override
    @Transactional
    public Feedback togglePublicVisibility(Long id) {
        Feedback feedback = getFeedbackById(id);
        boolean currentlyVisible = Boolean.TRUE.equals(feedback.getPublicVisibility());
        boolean newVisibility = !currentlyVisible;
        feedback.setPublicVisibility(newVisibility);

        // Approving/Publishing toggles public_visibility state allowing view on public vehicle catalog page
        if (newVisibility) {
            feedback.setApprovalStatus("APPROVED");
            feedback.setStatus("RESOLVED");
        }
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional
    public Feedback resolveFeedback(Long id) {
        Feedback feedback = getFeedbackById(id);
        feedback.setStatus("RESOLVED");
        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteCustomerFeedback(Long id, Customer customer) {
        Feedback existing = getFeedbackById(id);
        if (customer != null && existing.getCustomer() != null &&
                !existing.getCustomer().getSystemId().equals(customer.getSystemId())) {
            throw new IllegalArgumentException("You are not authorized to delete this feedback.");
        }
        feedbackRepository.delete(existing);
    }

    @Override
    @Transactional
    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    @Override
    public List<Feedback> getApprovedOrAcceptedFeedback() {
        return feedbackRepository.findAll().stream()
                .filter(f -> f.getApprovalStatus() != null &&
                        ("APPROVED".equalsIgnoreCase(f.getApprovalStatus().trim()) ||
                         "ACCEPTED".equalsIgnoreCase(f.getApprovalStatus().trim())))
                .toList();
    }
}
