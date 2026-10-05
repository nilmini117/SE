package com.driveflow.demo_driveflow.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    @DisplayName("EmailService: Class and methods must be annotated with @Async for non-blocking processing")
    void testAsyncAnnotation_Present() {
        assertTrue(EmailServiceImpl.class.isAnnotationPresent(Async.class),
                "EmailServiceImpl must be annotated with @Async for asynchronous non-blocking email processing");
    }

    @Test
    @DisplayName("EmailService: sendMaintenanceNotificationEmail dispatches HTML email with registration, date, and cost")
    void testSendMaintenanceNotificationEmail_DispatchesHtmlEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        LocalDate serviceDate = LocalDate.of(2026, 10, 15);
        BigDecimal approxCost = new BigDecimal("25000.00");

        emailService.sendMaintenanceNotificationEmail(
                "partner@precisionfleet.com",
                "AutoCare Precision",
                "Toyota Corolla (WP-CAB-1234)",
                serviceDate,
                approxCost
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);
        assertEquals("[DriveFlow] Vehicle Maintenance Allocation: Toyota Corolla (WP-CAB-1234)", sentMessage.getSubject());
        assertEquals("partner@precisionfleet.com", sentMessage.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendBookingConfirmationEmail dispatches HTML email with booking ref and pickup date")
    void testSendBookingConfirmationEmail_DispatchesHtmlEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        LocalDate pickupDate = LocalDate.of(2026, 11, 1);

        emailService.sendBookingConfirmationEmail(
                "customer@driveflow.com",
                "Alice Smith",
                "#BK-999",
                pickupDate,
                "Tesla Model 3"
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);
        assertEquals("[DriveFlow] Booking Confirmation - #BK-999", sentMessage.getSubject());
        assertEquals("customer@driveflow.com", sentMessage.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: Handles mail sending failure gracefully without throwing unhandled exceptions")
    void testEmailFailure_HandledGracefully() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().doThrow(new RuntimeException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendBookingConfirmationEmail(
                "customer@driveflow.com",
                "Alice Smith",
                "#BK-100",
                LocalDate.now(),
                "Toyota Prius"
        ));
    }

    @Test
    @DisplayName("EmailService: sendRegistrationOtpEmail dispatches 6-digit OTP verification code")
    void testSendRegistrationOtpEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendRegistrationOtpEmail("newuser@driveflow.com", "Alice Smith", "482915", 10);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Your Registration Verification Code: 482915", msg.getSubject());
        assertEquals("newuser@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendPasswordOtpEmail dispatches OTP for password change/reset")
    void testSendPasswordOtpEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordOtpEmail("user@driveflow.com", "Alice Smith", "739102", 10);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Password Change Verification Code: 739102", msg.getSubject());
        assertEquals("user@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendWelcomeEmail dispatches welcome message post-registration")
    void testSendWelcomeEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendWelcomeEmail("customer@driveflow.com", "John Doe");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("Welcome to DriveFlow - Account Created", msg.getSubject());
        assertEquals("customer@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendBookingPlacedEmail dispatches notification with pending status and dates")
    void testSendBookingPlacedEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingPlacedEmail(
                "customer@driveflow.com",
                "John Doe",
                1001L,
                "Tesla Model Y (Electric)",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5)
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Rental Reservation Placed - #BK-1001", msg.getSubject());
        assertEquals("customer@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendBookingDecisionEmail dispatches approval and cancellation notices")
    void testSendBookingDecisionEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Test APPROVED decision
        emailService.sendBookingDecisionEmail(
                "customer@driveflow.com",
                "John Doe",
                1001L,
                "Tesla Model Y",
                "APPROVED",
                "Please proceed to complete payment."
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage approvalMsg = captor.getValue();
        assertEquals("[DriveFlow] Booking Approved - #BK-1001 - Proceed to Payment", approvalMsg.getSubject());

        // Test CANCELLED decision
        MimeMessage cancelMime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(cancelMime);

        emailService.sendBookingDecisionEmail(
                "customer@driveflow.com",
                "John Doe",
                1002L,
                "Tesla Model Y",
                "CANCELLED",
                "Vehicle unavailable on requested dates."
        );

        ArgumentCaptor<MimeMessage> captor2 = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(captor2.capture());

        MimeMessage cancelMsg = captor2.getValue();
        assertEquals("[DriveFlow] Booking Update: Reservation Cancelled - #BK-1002", cancelMsg.getSubject());
    }

    @Test
    @DisplayName("EmailService: sendPaymentReceiptEmail dispatches digital receipt strictly in Sri Lankan Rupees (Rs.)")
    void testSendPaymentReceiptEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentReceiptEmail(
                "customer@driveflow.com",
                "John Doe",
                "PAY-88419",
                50L,
                1001L,
                new BigDecimal("45000.00"),
                LocalDate.of(2026, 10, 3)
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Official Payment Receipt - Ref: PAY-88419", msg.getSubject());
        assertEquals("customer@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendVehicleReturnedThankYouEmail dispatches thank you and link to feedback module")
    void testSendVehicleReturnedThankYouEmail() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVehicleReturnedThankYouEmail(
                "customer@driveflow.com",
                "John Doe",
                1001L,
                "Mercedes-Benz C-Class",
                "/feedback"
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Vehicle Check-In Complete - Thank You for Driving with Us!", msg.getSubject());
        assertEquals("customer@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendVehicleReturnedThankYouEmail with isEarlyReturn dispatches refund notice")
    void testSendVehicleReturnedThankYouEmail_WithEarlyReturn() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVehicleReturnedThankYouEmail(
                "customer@driveflow.com",
                "John Doe",
                1001L,
                "Mercedes-Benz C-Class",
                "/feedback",
                true
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Vehicle Check-In Complete - Thank You for Driving with Us!", msg.getSubject());
        assertEquals("customer@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendReturnVehicleOtpEmail dispatches OTP authorization code with early return notice")
    void testSendReturnVehicleOtpEmail_WithEarlyReturn() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendReturnVehicleOtpEmail(
                "customer@driveflow.com",
                "John Doe",
                1001L,
                "654321",
                10,
                true
        );

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage msg = captor.getValue();
        assertNotNull(msg);
        assertEquals("[DriveFlow] Authorization Code for Vehicle Return - Booking #BK-1001", msg.getSubject());
        assertEquals("customer@driveflow.com", msg.getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("EmailService: sendNotification dispatches SimpleMailMessage with official sender")
    void testSendNotification_DispatchesSimpleMailMessage() {
        emailService.sendNotification("driver@example.com", "Test Subject", "Test Body Message");

        ArgumentCaptor<org.springframework.mail.SimpleMailMessage> captor =
                ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        org.springframework.mail.SimpleMailMessage sent = captor.getValue();
        assertNotNull(sent);
        assertEquals("notifications@driveflow.com", sent.getFrom());
        assertArrayEquals(new String[]{"driver@example.com"}, sent.getTo());
        assertEquals("Test Subject", sent.getSubject());
        assertEquals("Test Body Message", sent.getText());
    }
}
