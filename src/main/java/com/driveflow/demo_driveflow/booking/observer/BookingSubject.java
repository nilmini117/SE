package com.driveflow.demo_driveflow.booking.observer;

/**
 * The Subject interface that manages the observers.
 * Following lecture guidelines for the Observer Design Pattern.
 */
public interface BookingSubject {
    void addObserver(BookingObserver observer);
    void removeObserver(BookingObserver observer);
    void notifyObservers(String bookingId, String vehicleRegNumber, String userEmail);
}
