package com.driveflow.demo_driveflow.feedback.observer;

public class FeedbackDemo {
    public static void main(String[] args) {
        // 1. Create the Subject
        FeedbackManager feedbackManager = new FeedbackManager();

        // 2. Create the Observers
        EmailAlertObserver emailAlert = new EmailAlertObserver();
        DashboardUpdateObserver dashboardUpdate = new DashboardUpdateObserver();

        // 3. Register Observers to the Subject
        feedbackManager.addObserver(emailAlert);
        feedbackManager.addObserver(dashboardUpdate);

        // 4. Simulate a customer submitting a review
        feedbackManager.receiveNewFeedback("The vehicle was very clean and the staff was friendly.");
        
        // Simulate a bad review
        feedbackManager.receiveNewFeedback("Waited 30 minutes at the counter!");
    }
}
