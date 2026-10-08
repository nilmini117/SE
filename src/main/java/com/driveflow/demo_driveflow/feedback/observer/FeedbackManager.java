package com.driveflow.demo_driveflow.feedback.observer;

import java.util.ArrayList;
import java.util.List;

public class FeedbackManager implements Subject {
    private List<Observer> observers = new ArrayList<>();
    private String latestFeedback = "";

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(latestFeedback);
        }
    }

    // Method called when a customer submits new feedback on the website
    public void receiveNewFeedback(String feedback) {
        this.latestFeedback = feedback;
        System.out.println("\nFeedback Manager: New feedback saved to database.");
        notifyObservers();
    }
}
