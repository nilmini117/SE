package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.users.Customer;
import java.util.List;

public interface FeedbackService {
    List<Feedback> getAllFeedback();
    List<Feedback> getFeedbackByCustomer(Customer customer);
    Feedback getFeedbackById(Long id);
    Feedback submitFeedback(Feedback feedback);
    Feedback updateFeedback(Long id, Feedback feedback);
    Feedback resolveFeedback(Long id);
    void deleteFeedback(Long id);
}
