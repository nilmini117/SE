package com.driveflow.demo_driveflow.booking.observer;

/**
 * The Observer interface that all listeners must implement.
 * Following lecture guidelines for the Observer Design Pattern.
 */
public interface BookingObserver {
    void update(String bookingId, String vehicleRegNumber, String userEmail);
}
