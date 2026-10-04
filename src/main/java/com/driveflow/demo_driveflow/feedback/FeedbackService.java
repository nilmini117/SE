package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.users.Customer;
import java.util.List;

public interface FeedbackService {
    List<Feedback> getAllFeedback();
    List<Feedback> getFeedbackByCustomer(Customer customer);
    List<Feedback> getPubliclyVisibleFeedback();
    Feedback getFeedbackById(Long id);
    Feedback submitFeedback(Feedback feedback);
    Feedback submitCustomerFeedback(Feedback feedback, Customer customer, Long bookingId);
    Feedback updateFeedback(Long id, Feedback feedback);
    Feedback updateCustomerFeedback(Long id, Feedback feedback, Customer customer);
    Feedback togglePublicVisibility(Long id);
    Feedback resolveFeedback(Long id);
    void deleteFeedback(Long id);
    void deleteCustomerFeedback(Long id, Customer customer);
    Feedback save(Feedback feedback);
    List<Feedback> getApprovedOrAcceptedFeedback();
}
