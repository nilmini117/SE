package com.driveflow.demo_driveflow.booking.observer;

/**
 * Standalone Demonstration of the Observer Design Pattern for DriveFlow Booking.
 * Implements the exact lecture structure and guidelines:
 * 1. Initialize Subject (BookingManager)
 * 2. Initialize Observers (EmailNotifier, VehicleStatusUpdater)
 * 3. Attach Observers to the Subject
 * 4. Trigger confirmNewBooking() to notify observers loosely.
 */
public class ObserverPatternDemo {

    public static void main(String[] args) {
        // 1. Initialize the Subject
        BookingManager bookingManager = new BookingManager();

        // 2. Initialize the Observers
        EmailNotifier emailNotifier = new EmailNotifier();
        VehicleStatusUpdater vehicleUpdater = new VehicleStatusUpdater();

        // 3. Attach Observers to the Subject
        bookingManager.addObserver(emailNotifier);
        bookingManager.addObserver(vehicleUpdater);

        // 4. Simulate a customer making a booking
        System.out.println("--- Customer confirms booking ---");
        bookingManager.confirmNewBooking("BKG-9942", "WP CA-8942", "customer@example.com");
    }
}
