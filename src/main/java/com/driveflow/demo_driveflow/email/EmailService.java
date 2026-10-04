package com.driveflow.demo_driveflow.email;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EmailService {

    /**
     * Sends a 6-digit numeric OTP email to verify a new user's email during registration.
     *
     * @param toEmail       Recipient email address
     * @param customerName  Applicant name
     * @param otp           6-digit numeric OTP code
     * @param expiryMinutes Strict expiration window in minutes
     */
    void sendRegistrationOtpEmail(String toEmail, String customerName, String otp, int expiryMinutes);

    /**
     * Sends a 6-digit numeric OTP email to authorize a password change or password reset request.
     *
     * @param toEmail       Registered user email address
     * @param customerName  User name
     * @param otp           6-digit numeric OTP code
     * @param expiryMinutes Strict expiration window in minutes
     */
    void sendPasswordOtpEmail(String toEmail, String customerName, String otp, int expiryMinutes);

    /**
     * [Lifecycle 1: Post-Registration Welcome]
     * Automatically triggers a welcome email immediately after successful OTP validation and account creation.
     * Contains the required phrase: "log in succes welcome to drive flow"
     *
     * @param toEmail      Recipient email address
     * @param customerName Recipient customer name
     */
    void sendWelcomeEmail(String toEmail, String customerName);

    /**
     * [Lifecycle 2: Booking Placed]
     * Automatically triggers an email when a customer submits a new rental reservation (Status: PENDING).
     * Must include the Booking ID, vehicle details, and requested dates.
     *
     * @param toEmail        Customer's registered email address
     * @param customerName   Customer's full name
     * @param bookingId      Booking primary ID
     * @param vehicleDetails Reserved vehicle model and registration details
     * @param startDate      Requested start / pickup date
     * @param endDate        Requested end / return date
     */
    void sendBookingPlacedEmail(String toEmail, String customerName, Long bookingId,
                                String vehicleDetails, LocalDate startDate, LocalDate endDate);

    /**
     * [Lifecycle 3: Booking Decision (Approved / Cancelled)]
     * Automatically triggers an email when a staff member updates a booking status to:
     * - APPROVED / CONFIRMED: Instructs customer to proceed to invoice payment.
     * - CANCELLED: Provides a brief explanation or cancellation reason.
     *
     * @param toEmail        Customer's registered email address
     * @param customerName   Customer's full name
     * @param bookingId      Booking primary ID
     * @param vehicleDetails Vehicle model and registration
     * @param decisionStatus Updated status (APPROVED, CONFIRMED, or CANCELLED)
     * @param notesOrReason  Staff message or cancellation reason
     */
    void sendBookingDecisionEmail(String toEmail, String customerName, Long bookingId,
                                  String vehicleDetails, String decisionStatus, String notesOrReason);

    /**
     * [Lifecycle 4: Payment Successful (Digital Receipt)]
     * Automatically triggers immediately after a payment transaction clears.
     * Acts as a digital receipt, displaying the paid amount strictly in Sri Lankan Rupees (Rs.).
     *
     * @param toEmail      Customer's registered email address
     * @param customerName Customer's full name
     * @param paymentRef   Payment transaction reference (e.g. PAY-A1B2C3D4)
     * @param invoiceId    Invoice ID
     * @param bookingId    Booking ID
     * @param amountPaid   Amount settled (strictly formatted in Rs.)
     * @param paymentDate  Date of payment settlement
     */
    void sendPaymentReceiptEmail(String toEmail, String customerName, String paymentRef,
                                 Long invoiceId, Long bookingId, BigDecimal amountPaid, LocalDate paymentDate);

    /**
     * [Lifecycle 5: Vehicle Returned (Thank You & Feedback CTA)]
     * Automatically triggers when the customer clicks "Confirm Return" and booking status transitions to RETURNED.
     * Thanks them for driving with DriveFlow and includes a call-to-action link directing them to the newly unlocked Feedback module.
     *
     * @param toEmail        Customer's registered email address
     * @param customerName   Customer's full name
     * @param bookingId      Booking primary ID
     * @param vehicleDetails Returned vehicle model details
     * @param feedbackUrl    Direct call-to-action link to the feedback module
     */
    void sendVehicleReturnedThankYouEmail(String toEmail, String customerName, Long bookingId,
                                         String vehicleDetails, String feedbackUrl);

    /**
     * Automatically triggers an HTML-formatted email notification to the assigned maintenance company
     * upon saving a vehicle maintenance schedule.
     *
     * @param toEmail          Recipient maintenance company email address
     * @param companyName      Maintenance company name
     * @param vehicleDetails   Vehicle model and registration number
     * @param serviceDate      Scheduled service date
     * @param approximatedCost Approximated service cost
     */
    void sendMaintenanceNotificationEmail(String toEmail, String companyName, String vehicleDetails,
                                          LocalDate serviceDate, BigDecimal approximatedCost);

    /**
     * Automatically triggers an HTML-formatted confirmation email to the customer's registered email address
     * when a customer's booking status changes to CONFIRMED by staff, detailing their booking reference and pickup date.
     *
     * @param toEmail          Customer's registered email address
     * @param customerName     Customer's full name
     * @param bookingReference Booking reference code (e.g. #BK-101)
     * @param pickupDate       Scheduled vehicle pickup date
     * @param vehicleDetails   Reserved vehicle model details
     */
    void sendBookingConfirmationEmail(String toEmail, String customerName, String bookingReference,
                                      LocalDate pickupDate, String vehicleDetails);

    /**
     * Sends a plain-text notification email using SimpleMailMessage.
     *
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param body    Email body text
     */
    void sendNotification(String toEmail, String subject, String body);
}
