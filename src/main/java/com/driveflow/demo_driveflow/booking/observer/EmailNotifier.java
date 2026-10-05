package com.driveflow.demo_driveflow.booking.observer;

import com.driveflow.demo_driveflow.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer 1: Handles Email Notifications
 * Following lecture guidelines for the Observer Design Pattern.
 */
@Component
public class EmailNotifier implements BookingObserver {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Autowired(required = false)
    private EmailService emailService;

    public EmailNotifier() {
    }

    public EmailNotifier(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void update(String bookingId, String vehicleRegNumber, String userEmail) {
        System.out.println("Email Notifier: Sending booking confirmation to " + userEmail + " for booking " + bookingId);
        log.info("Email Notifier: Sending booking confirmation to {} for booking {}", userEmail, bookingId);

        if (emailService != null && userEmail != null && !userEmail.isBlank()) {
            try {
                emailService.sendNotification(
                        userEmail,
                        "DriveFlow Booking Confirmation: " + bookingId,
                        "Dear Customer,\n\nYour vehicle booking " + bookingId + " for vehicle [" +
                        vehicleRegNumber + "] has been successfully confirmed!\n\n" +
                        "Thank you for choosing DriveFlow.\nDriveFlow Operations Team"
                );
            } catch (Exception ex) {
                log.warn("EmailNotifier: Failed to dispatch email notification: {}", ex.getMessage());
            }
        }
    }
}
