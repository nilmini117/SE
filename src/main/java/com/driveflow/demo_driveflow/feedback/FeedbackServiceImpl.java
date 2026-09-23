package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Override
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    @Override
    public List<Feedback> getFeedbackByCustomer(Customer customer) {
        return feedbackRepository.findByCustomer(customer);
    }

    @Override
    public Feedback getFeedbackById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found: " + id));
    }

    @Override
    public Feedback submitFeedback(Feedback feedback) {
        if (feedback.getStatus() == null || feedback.getStatus().isBlank()) {
            feedback.setStatus("OPEN");
        }
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }
        return feedbackRepository.save(feedback);
    }

    @Override
    public Feedback updateFeedback(Long id, Feedback updated) {
        Feedback existing = getFeedbackById(id);
        existing.setCategory(updated.getCategory());
        existing.setMessage(updated.getMessage());
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        return feedbackRepository.save(existing);
    }

    @Override
    public Feedback resolveFeedback(Long id) {
        Feedback feedback = getFeedbackById(id);
        feedback.setStatus("RESOLVED");
        return feedbackRepository.save(feedback);
    }

    @Override
    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }
}
