package com.driveflow.demo_driveflow.booking.observer;

import com.driveflow.demo_driveflow.email.EmailService;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingObserverPatternTest {

    private BookingManager bookingManager;

    @Mock
    private EmailService emailService;

    @Mock
    private VehicleRepository vehicleRepository;

    private EmailNotifier emailNotifier;
    private VehicleStatusUpdater vehicleStatusUpdater;

    @BeforeEach
    void setUp() {
        bookingManager = new BookingManager();
        emailNotifier = new EmailNotifier(emailService);
        vehicleStatusUpdater = new VehicleStatusUpdater(vehicleRepository);
    }

    @Test
    @DisplayName("Observer Pattern: Observers are successfully attached and detached from BookingSubject")
    void testAttachAndDetachObservers() {
        assertEquals(0, bookingManager.getObservers().size());

        bookingManager.addObserver(emailNotifier);
        bookingManager.addObserver(vehicleStatusUpdater);
        assertEquals(2, bookingManager.getObservers().size());

        bookingManager.removeObserver(emailNotifier);
        assertEquals(1, bookingManager.getObservers().size());
        assertTrue(bookingManager.getObservers().contains(vehicleStatusUpdater));
    }

    @Test
    @DisplayName("Observer Pattern: confirmNewBooking notifies all registered observers loosely")
    void testConfirmNewBooking_NotifiesAllObservers() {
        // Arrange
        Vehicle testVehicle = new Vehicle();
        testVehicle.setRegNo("WP CA-8942");
        testVehicle.setStatus("AVAILABLE");

        when(vehicleRepository.findByRegNoIgnoreCase("WP CA-8942"))
                .thenReturn(Optional.of(testVehicle));

        bookingManager.addObserver(emailNotifier);
        bookingManager.addObserver(vehicleStatusUpdater);

        // Act
        bookingManager.confirmNewBooking("BKG-9942", "WP CA-8942", "customer@example.com");

        // Assert: EmailNotifier sent notification
        verify(emailService, times(1)).sendNotification(
                eq("customer@example.com"),
                contains("BKG-9942"),
                contains("WP CA-8942")
        );

        // Assert: VehicleStatusUpdater marked vehicle as BOOKED
        assertEquals("BOOKED", testVehicle.getStatus());
        verify(vehicleRepository, times(1)).save(testVehicle);
    }

    @Test
    @DisplayName("Observer Pattern Extensibility: New Observer (e.g. SmsNotifier) can be attached without touching BookingManager")
    void testExtensibility_AddNewObserverWithoutModifyingSubject() {
        bookingManager.addObserver(emailNotifier);

        // Create an impromptu 3rd observer (SmsNotifier) without altering BookingManager
        AtomicBoolean smsSent = new AtomicBoolean(false);
        BookingObserver smsNotifier = (bId, regNo, email) -> {
            System.out.println("SMS Notifier: Dispatching SMS alert for " + bId);
            smsSent.set(true);
        };

        bookingManager.addObserver(smsNotifier);
        assertEquals(2, bookingManager.getObservers().size());

        // Act
        bookingManager.confirmNewBooking("BKG-101", "WP CB-1234", "user@driveflow.com");

        // Assert: Both observers fired
        assertTrue(smsSent.get(), "New SmsNotifier should have been triggered without modifying BookingManager");
        verify(emailService, times(1)).sendNotification(anyString(), anyString(), anyString());
    }
}
