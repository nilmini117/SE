package com.driveflow.demo_driveflow.feedback.observer;

public class DashboardUpdateObserver implements Observer {
    private String message;

    @Override
    public void update(String feedbackMessage) {
        this.message = feedbackMessage;
        display();
    }

    private void display() {
        System.out.println("Admin Dashboard UI: Updating live feedback feed with - \"" + message + "\"");
    }
}
