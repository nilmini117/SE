package com.driveflow.demo_driveflow.booking.observer;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Subject: Manages the observer list and fires notifications upon booking confirmation.
 * Following lecture guidelines for the Observer Design Pattern.
 */
@Component
public class BookingManager implements BookingSubject {

    private static final Logger log = LoggerFactory.getLogger(BookingManager.class);

    private final List<BookingObserver> observers = new ArrayList<>();

    @Autowired(required = false)
    private EmailNotifier emailNotifier;

    @Autowired(required = false)
    private VehicleStatusUpdater vehicleStatusUpdater;

    public BookingManager() {
    }

    public BookingManager(List<BookingObserver> initialObservers) {
        if (initialObservers != null) {
            this.observers.addAll(initialObservers);
        }
    }

    @PostConstruct
    public void init() {
        if (emailNotifier != null && !observers.contains(emailNotifier)) {
            addObserver(emailNotifier);
        }
        if (vehicleStatusUpdater != null && !observers.contains(vehicleStatusUpdater)) {
            addObserver(vehicleStatusUpdater);
        }
    }

    @Override
    public void addObserver(BookingObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            log.info("BookingObserver registered: {}", observer.getClass().getSimpleName());
        }
    }

    @Override
    public void removeObserver(BookingObserver observer) {
        observers.remove(observer);
        log.info("BookingObserver removed: {}", observer.getClass().getSimpleName());
    }

    @Override
    public void notifyObservers(String bookingId, String vehicleRegNumber, String userEmail) {
        for (BookingObserver observer : observers) {
            try {
                observer.update(bookingId, vehicleRegNumber, userEmail);
            } catch (Exception ex) {
                log.error("Error notifying observer {}: {}", observer.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
    }

    // The main method called when a booking is confirmed
    public void confirmNewBooking(String bookingId, String vehicleRegNumber, String userEmail) {
        System.out.println("Booking Manager: Booking " + bookingId + " successfully saved to database.");
        log.info("Booking Manager: Booking {} successfully saved to database.", bookingId);

        // Automatically trigger all observers
        notifyObservers(bookingId, vehicleRegNumber, userEmail);
    }

    public List<BookingObserver> getObservers() {
        return new ArrayList<>(observers);
    }
}
