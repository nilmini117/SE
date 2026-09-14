package com.driveflow.demo_driveflow.feedback;

import java.util.List;

public interface FeedbackService {
    List<Feedback> getAllFeedback();
    Feedback getFeedbackById(Long id);
    Feedback submitFeedback(Feedback feedback);
    Feedback resolveFeedback(Long id);
    void deleteFeedback(Long id);
}
