package com.driveflow.demo_driveflow.feedback.observer;

public class EmailAlertObserver implements Observer {
    private String message;

    @Override
    public void update(String feedbackMessage) {
        this.message = feedbackMessage;
        display();
    }

    private void display() {
        System.out.println("Email Alert System: Sending email to branch manager about feedback - \"" + message + "\"");
    }
}
