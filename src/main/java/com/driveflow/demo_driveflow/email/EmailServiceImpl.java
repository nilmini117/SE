package com.driveflow.demo_driveflow.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Async
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    public static final String WELCOME_PHRASE = "log in succes welcome to drive flow";

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:notifications@driveflow.com}")
    private String fromEmail = "notifications@driveflow.com";

    @Override
    @Async
    public void sendNotification(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send notification: recipient address is empty.");
            return;
        }
        if (mailSender == null) {
            log.warn("JavaMailSender bean is not configured. Email logged but not dispatched: {}", body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "notifications@driveflow.com";
        message.setFrom(sender);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("✅ Successfully delivered notification email to {}", toEmail);
        } catch (Exception ex) {
            log.error("❌ Failed to deliver notification email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    @Override
    public void sendRegistrationOtpEmail(String toEmail, String customerName, String otp, int expiryMinutes) {
        String subject = "[DriveFlow] Your Registration Verification Code: " + otp;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Future Driver";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #0f172a; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Security Verification</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #94a3b8;'>Customer Account Registration</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Hello <strong>" + displayName + "</strong>,</p>" +
                "<p>Thank you for initiating registration with DriveFlow Car Rental System. To verify your email address and activate your account, please enter the 6-digit One-Time Password (OTP) below:</p>" +
                "<div style='font-size: 2.2rem; font-weight: 800; letter-spacing: 8px; color: #2563eb; background: #eff6ff; padding: 18px 24px; text-align: center; border-radius: 8px; border: 2px dashed #93c5fd; margin: 22px 0;'>" +
                otp +
                "</div>" +
                "<div style='background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 4px; font-size: 0.85rem; color: #92400e; margin-bottom: 18px;'>" +
                "⏱️ <strong>Strict Expiration:</strong> This OTP is valid for <strong>" + expiryMinutes + " minutes</strong>. Do not share this code with anyone." +
                "</div>" +
                "<p style='font-size: 0.85rem; color: #64748b;'>If you did not request this registration, you can safely ignore this email.</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Security Team</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. Automated Security Dispatch." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Registration OTP");
    }

    @Override
    public void sendPasswordOtpEmail(String toEmail, String customerName, String otp, int expiryMinutes) {
        String subject = "[DriveFlow] Password Change Verification Code: " + otp;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Driver";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #7f1d1d; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Account Security</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #fca5a5;'>Password Change / Reset Request</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Hello <strong>" + displayName + "</strong>,</p>" +
                "<p>A request to change or reset your DriveFlow account password was submitted. Use the 6-digit One-Time Password (OTP) below to authorize this password update:</p>" +
                "<div style='font-size: 2.2rem; font-weight: 800; letter-spacing: 8px; color: #b91c1c; background: #fef2f2; padding: 18px 24px; text-align: center; border-radius: 8px; border: 2px dashed #fca5a5; margin: 22px 0;'>" +
                otp +
                "</div>" +
                "<div style='background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 4px; font-size: 0.85rem; color: #92400e; margin-bottom: 18px;'>" +
                "⏱️ <strong>Strict Expiration:</strong> This OTP will expire in <strong>" + expiryMinutes + " minutes</strong>. The password update transaction is blocked until this code is verified." +
                "</div>" +
                "<p style='font-size: 0.85rem; color: #64748b;'>If you did not initiate this request, please contact DriveFlow support immediately.</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Security Team</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. Automated Security Dispatch." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Password Change OTP");
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String customerName) {
        String subject = "Welcome to DriveFlow - Account Created";
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Valued Customer";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #1e293b; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem; letter-spacing: -0.5px;'>DriveFlow Car Rental System</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #94a3b8;'>Customer Onboarding Notification</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Hello <strong>" + displayName + "</strong>,</p>" +
                "<p>Your DriveFlow account has been created successfully!</p>" +
                "<div style='background: #f1f5f9; padding: 14px 18px; border-left: 4px solid #3b82f6; border-radius: 4px; margin: 18px 0;'>" +
                "<strong style='color: #1e40af;'>" + WELCOME_PHRASE + "</strong>" +
                "</div>" +
                "<p>You can now log in to the customer portal to reserve vehicles from our nationwide fleet park.</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Team</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. All rights reserved." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Welcome Notification");
    }

    @Override
    public void sendBookingPlacedEmail(String toEmail, String customerName, Long bookingId,
                                      String vehicleDetails, LocalDate startDate, LocalDate endDate) {
        String subject = "[DriveFlow] Rental Reservation Placed - #BK-" + bookingId;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String startFormatted = (startDate != null) ? startDate.toString() : LocalDate.now().toString();
        String endFormatted = (endDate != null) ? endDate.toString() : startFormatted;

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #0369a1; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Car Rental System</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #bae6fd;'>Booking Success &bull; Reservation Placed - Awaiting Staff Review</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + "</strong>,</p>" +
                "<div style='background: #ecfdf5; border-left: 4px solid #10b981; padding: 12px 16px; border-radius: 4px; margin: 16px 0; color: #065f46; font-weight: bold;'>" +
                "&#10004; Booking Placed Successfully! Your reservation is awaiting staff review and approval." +
                "</div>" +
                "<p>We have successfully received your vehicle reservation request. Our fleet management team is reviewing vehicle readiness.</p>" +
                "<table style='width: 100%; border-collapse: collapse; margin: 18px 0; font-size: 0.9rem;'>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold; width: 40%;'>Booking ID:</td><td style='padding: 10px; color: #0284c7; font-weight: bold;'>#BK-" + bookingId + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Reserved Vehicle:</td><td style='padding: 10px; font-weight: bold;'>" + vehicleDetails + "</td></tr>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Pickup Date:</td><td style='padding: 10px;'>" + startFormatted + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Return Date:</td><td style='padding: 10px;'>" + endFormatted + "</td></tr>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Initial Status:</td><td style='padding: 10px;'><span style='background: #fef3c7; color: #b45309; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold;'>PENDING</span></td></tr>" +
                "</table>" +
                "<p>Once approved by our staff team, you will receive another email notification with payment instructions to confirm your booking.</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Fleet Operations</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. All rights reserved." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Booking Placed");
    }

    @Override
    public void sendBookingDecisionEmail(String toEmail, String customerName, Long bookingId,
                                         String vehicleDetails, String decisionStatus, String notesOrReason) {
        boolean isApproved = "APPROVED".equalsIgnoreCase(decisionStatus) || "CONFIRMED".equalsIgnoreCase(decisionStatus);
        String subject = isApproved
                ? "[DriveFlow] Booking Approved - #BK-" + bookingId + " - Proceed to Payment"
                : "[DriveFlow] Booking Update: Reservation Cancelled - #BK-" + bookingId;

        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String headerBg = isApproved ? "#15803d" : "#991b1b";
        String statusBg = isApproved ? "#dcfce7" : "#fee2e2";
        String statusColor = isApproved ? "#15803d" : "#b91c1c";
        String statusText = isApproved ? "APPROVED" : "CANCELLED";

        String instructions = isApproved
                ? "<div style='background: #f0fdf4; border-left: 4px solid #22c55e; padding: 14px 16px; border-radius: 4px; margin: 18px 0;'>" +
                  "<strong style='color: #166534;'>Payment Gate Unlocked:</strong> Your booking is approved! Please sign in to the DriveFlow customer portal to view your invoice and complete your payment." +
                  "</div>"
                : "<div style='background: #fef2f2; border-left: 4px solid #ef4444; padding: 14px 16px; border-radius: 4px; margin: 18px 0;'>" +
                  "<strong style='color: #991b1b;'>Reason for Cancellation:</strong> " +
                  (notesOrReason != null && !notesOrReason.isBlank() ? notesOrReason : "Reservation cancelled by staff.") +
                  "</div>";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: " + headerBg + "; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Car Rental System</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #f1f5f9;'>Booking Decision: " + statusText + "</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + "</strong>,</p>" +
                "<p>There is an official update regarding your vehicle reservation:</p>" +
                "<table style='width: 100%; border-collapse: collapse; margin: 18px 0; font-size: 0.9rem;'>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold; width: 40%;'>Booking Reference:</td><td style='padding: 10px; font-weight: bold;'>#BK-" + bookingId + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Vehicle:</td><td style='padding: 10px;'>" + vehicleDetails + "</td></tr>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Updated Decision:</td><td style='padding: 10px;'><span style='background: " + statusBg + "; color: " + statusColor + "; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold;'>" + statusText + "</span></td></tr>" +
                "</table>" +
                instructions +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Customer Support</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. All rights reserved." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Booking Decision (" + statusText + ")");
    }

    @Override
    public void sendPaymentReceiptEmail(String toEmail, String customerName, String paymentRef,
                                       Long invoiceId, Long bookingId, BigDecimal amountPaid, LocalDate paymentDate) {
        String subject = "[DriveFlow] Official Payment Receipt - Ref: " + paymentRef;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String dateFormatted = (paymentDate != null) ? paymentDate.toString() : LocalDate.now().toString();
        // Strictly formatted in Sri Lankan Rupees (Rs.)
        String amountFormatted = (amountPaid != null)
                ? "Rs. " + String.format("%,.2f", amountPaid)
                : "Rs. 0.00";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #0f172a; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Billing & Finance</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #cbd5e1;'>Official Digital Payment Receipt</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + "</strong>,</p>" +
                "<p>Thank you for your payment. Your transaction has cleared successfully and your invoice is now settled:</p>" +
                "<div style='background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 16px; margin: 18px 0;'>" +
                "<div style='font-size: 0.85rem; color: #64748b;'>TOTAL AMOUNT PAID (LKR)</div>" +
                "<div style='font-size: 1.75rem; font-weight: 800; color: #047857; margin: 4px 0 12px 0;'>" + amountFormatted + "</div>" +
                "<table style='width: 100%; border-collapse: collapse; font-size: 0.88rem;'>" +
                "<tr style='border-top: 1px solid #e2e8f0;'><td style='padding: 8px 0; color: #64748b;'>Payment Reference:</td><td style='padding: 8px 0; font-weight: bold; text-align: right;'>" + paymentRef + "</td></tr>" +
                "<tr style='border-top: 1px solid #e2e8f0;'><td style='padding: 8px 0; color: #64748b;'>Invoice Number:</td><td style='padding: 8px 0; font-weight: bold; text-align: right;'>#INV-" + invoiceId + "</td></tr>" +
                "<tr style='border-top: 1px solid #e2e8f0;'><td style='padding: 8px 0; color: #64748b;'>Booking Reference:</td><td style='padding: 8px 0; font-weight: bold; text-align: right;'>#BK-" + bookingId + "</td></tr>" +
                "<tr style='border-top: 1px solid #e2e8f0;'><td style='padding: 8px 0; color: #64748b;'>Payment Date:</td><td style='padding: 8px 0; text-align: right;'>" + dateFormatted + "</td></tr>" +
                "<tr style='border-top: 1px solid #e2e8f0;'><td style='padding: 8px 0; color: #64748b;'>Settlement Status:</td><td style='padding: 8px 0; text-align: right;'><span style='background: #dcfce7; color: #15803d; padding: 2px 8px; border-radius: 4px; font-size: 0.78rem; font-weight: bold;'>PAID</span></td></tr>" +
                "</table>" +
                "</div>" +
                "<p>Your vehicle is secured for your journey. Have a safe and comfortable trip!</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Accounts Department</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. All transactions billed in Sri Lankan Rupees (Rs.)." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Payment Receipt");
    }

    @Override
    public void sendVehicleReturnedThankYouEmail(String toEmail, String customerName, Long bookingId,
                                                String vehicleDetails, String feedbackUrl) {
        sendVehicleReturnedThankYouEmail(toEmail, customerName, bookingId, vehicleDetails, feedbackUrl, false);
    }

    @Override
    public void sendVehicleReturnedThankYouEmail(String toEmail, String customerName, Long bookingId,
                                                String vehicleDetails, String feedbackUrl, boolean isEarlyReturn) {
        String subject = "[DriveFlow] Vehicle Check-In Complete - Thank You for Driving with Us!";
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String link = (feedbackUrl != null && !feedbackUrl.isBlank()) ? feedbackUrl : "/feedback";

        String earlyReturnBanner = isEarlyReturn
                ? "<div style='background: #ecfdf5; border-left: 4px solid #10b981; padding: 14px 16px; border-radius: 6px; margin: 18px 0;'>" +
                  "<strong style='color: #065f46; font-size: 0.95rem;'>Refund Policy Notice:</strong>" +
                  "<p style='margin: 6px 0 0 0; color: #047857; font-size: 0.9rem; line-height: 1.45;'>" +
                  "Since you have returned the vehicle before your scheduled end date, your refund money can be collected from the branch front desk after giving the car key to the staff." +
                  "</p></div>"
                : "";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #4338ca; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Fleet Return</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #c7d2fe;'>Vehicle Check-In & Rental Closure</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + "</strong>,</p>" +
                "<p>Thank you for choosing DriveFlow for your travel! We are pleased to confirm that your vehicle has been successfully returned and checked in:</p>" +
                "<table style='width: 100%; border-collapse: collapse; margin: 18px 0; font-size: 0.9rem;'>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold; width: 40%;'>Booking Reference:</td><td style='padding: 10px; font-weight: bold;'>#BK-" + bookingId + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Returned Vehicle:</td><td style='padding: 10px;'>" + vehicleDetails + "</td></tr>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Status:</td><td style='padding: 10px;'><span style='background: #e0e7ff; color: #4338ca; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold;'>RETURNED</span></td></tr>" +
                "</table>" +
                earlyReturnBanner +
                "<div style='background: #faf5ff; border: 1px solid #e9d5ff; border-radius: 8px; padding: 18px; margin: 20px 0; text-align: center;'>" +
                "<h3 style='margin: 0 0 8px 0; color: #6b21a8; font-size: 1.05rem;'>How was your ride?</h3>" +
                "<p style='margin: 0 0 16px 0; font-size: 0.88rem; color: #7e22ce;'>The Feedback module for this rental is now unlocked. We would love to hear about your experience.</p>" +
                "<a href='" + link + "' style='display: inline-block; background: #6366f1; color: #ffffff; padding: 10px 24px; border-radius: 6px; font-weight: bold; text-decoration: none; font-size: 0.92rem; box-shadow: 0 2px 4px rgba(99, 102, 241, 0.3);'>Rate Your Experience & Leave Feedback &rarr;</a>" +
                "</div>" +
                "<p style='margin-bottom: 0;'>We look forward to welcoming you back on the road soon!<br><strong>The DriveFlow Team</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. Thank you for driving with us." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Vehicle Returned Thank You");
    }

    @Override
    public void sendReturnVehicleOtpEmail(String toEmail, String customerName, Long bookingId,
                                         String otp, int expiryMinutes, boolean isEarlyReturn) {
        String subject = "[DriveFlow] Authorization Code for Vehicle Return - Booking #BK-" + bookingId;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Driver";

        String earlyReturnHtml = isEarlyReturn
                ? "<div style='background: #ecfdf5; border-left: 4px solid #10b981; padding: 14px 16px; border-radius: 6px; margin: 18px 0;'>" +
                  "<strong style='color: #065f46; font-size: 0.95rem;'>Early Return Refund Policy:</strong>" +
                  "<p style='margin: 6px 0 0 0; color: #047857; font-size: 0.9rem; line-height: 1.45;'>" +
                  "If you return the vehicle before the scheduled end date, refund money can be collected from the branch front desk after giving the car key to the staff." +
                  "</p></div>"
                : "";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #2563eb; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Security Verification</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #dbeafe;'>Vehicle Return Authorization Code</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + "</strong>,</p>" +
                "<p>You have initiated the vehicle return process for reservation <strong>#BK-" + bookingId + "</strong>. Please enter the following 6-digit authorization code to verify and complete your return:</p>" +
                "<div style='background: #eff6ff; border: 1px dashed #3b82f6; border-radius: 8px; padding: 18px; margin: 20px 0; text-align: center;'>" +
                "<span style='font-family: monospace; font-size: 2.2rem; font-weight: bold; letter-spacing: 8px; color: #1d4ed8;'>" + otp + "</span>" +
                "<div style='margin-top: 8px; font-size: 0.8rem; color: #64748b;'>Expires in " + expiryMinutes + " minutes. Do not share this code with anyone.</div>" +
                "</div>" +
                earlyReturnHtml +
                "<p style='font-size: 0.88rem; color: #64748b;'>If you did not initiate this vehicle return request, please contact our support desk immediately.</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Fleet Operations</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. All rights reserved." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Vehicle Return OTP");
    }

    @Override
    public void sendMaintenanceNotificationEmail(String toEmail, String companyName, String vehicleDetails,
                                                 LocalDate serviceDate, BigDecimal approximatedCost) {
        String subject = "[DriveFlow] Vehicle Maintenance Allocation: " + vehicleDetails;
        String displayName = (companyName != null && !companyName.isBlank()) ? companyName : "Maintenance Partner";
        String dateFormatted = (serviceDate != null) ? serviceDate.toString() : LocalDate.now().toString();
        String costFormatted = (approximatedCost != null) ? "Rs. " + approximatedCost.toPlainString() : "Rs. 0.00";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #0f172a; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Fleet Operations</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #cbd5e1;'>Work Order & Maintenance Bay Allocation</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + " Team</strong>,</p>" +
                "<p>A vehicle from the DriveFlow park has been scheduled for maintenance and allocated to your service center:</p>" +
                "<table style='width: 100%; border-collapse: collapse; margin: 18px 0; font-size: 0.9rem;'>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold; width: 40%;'>Vehicle Registration:</td><td style='padding: 10px; color: #1d4ed8; font-weight: bold;'>" + vehicleDetails + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Requested Date:</td><td style='padding: 10px;'>" + dateFormatted + "</td></tr>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Approximated Cost:</td><td style='padding: 10px; color: #047857; font-weight: bold;'>" + costFormatted + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Vehicle Status:</td><td style='padding: 10px;'><span style='background: #fee2e2; color: #b91c1c; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold;'>UNAVAILABLE</span></td></tr>" +
                "</table>" +
                "<p>Please ensure your service bay is prepared for vehicle intake. The vehicle's fleet availability has automatically been locked.</p>" +
                "<p style='margin-bottom: 0;'>Best Regards,<br><strong>DriveFlow Fleet Management</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Fleet Operations. Automated Dispatch System." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Maintenance Allocation");
    }

    @Override
    public void sendBookingConfirmationEmail(String toEmail, String customerName, String bookingReference,
                                            LocalDate pickupDate, String vehicleDetails) {
        String subject = "[DriveFlow] Booking Confirmation - " + bookingReference;
        String displayName = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String dateFormatted = (pickupDate != null) ? pickupDate.toString() : LocalDate.now().toString();
        String vehicleInfo = (vehicleDetails != null && !vehicleDetails.isBlank()) ? vehicleDetails : "Reserved Vehicle";

        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; padding: 20px; color: #1e293b;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border: 1px solid #e2e8f0;'>" +
                "<div style='background: #1e3a8a; color: #ffffff; padding: 20px 24px;'>" +
                "<h2 style='margin: 0; font-size: 1.3rem;'>DriveFlow Car Rental System</h2>" +
                "<p style='margin: 4px 0 0 0; font-size: 0.85rem; color: #bfdbfe;'>Reservation Status: CONFIRMED</p>" +
                "</div>" +
                "<div style='padding: 24px;'>" +
                "<p style='font-size: 1rem; margin-top: 0;'>Dear <strong>" + displayName + "</strong>,</p>" +
                "<p>Great news! Your vehicle reservation has been approved and marked as <strong>CONFIRMED</strong> by our staff.</p>" +
                "<table style='width: 100%; border-collapse: collapse; margin: 18px 0; font-size: 0.9rem;'>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold; width: 40%;'>Booking Reference:</td><td style='padding: 10px; color: #1e40af; font-weight: bold;'>" + bookingReference + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Reserved Vehicle:</td><td style='padding: 10px;'>" + vehicleInfo + "</td></tr>" +
                "<tr style='background: #f8fafc; border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Scheduled Pickup Date:</td><td style='padding: 10px; font-weight: bold; color: #047857;'>" + dateFormatted + "</td></tr>" +
                "<tr style='border-bottom: 1px solid #e2e8f0;'><td style='padding: 10px; font-weight: bold;'>Payment Gate:</td><td style='padding: 10px;'><span style='background: #dcfce7; color: #15803d; padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold;'>UNLOCKED</span></td></tr>" +
                "</table>" +
                "<p>The payment gate for your booking is now unlocked. You can sign in to the DriveFlow customer portal to view your invoice and complete your payment.</p>" +
                "<p style='margin-bottom: 0;'>Thank you for choosing DriveFlow!<br><strong>DriveFlow Customer Support Team</strong></p>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 12px 24px; font-size: 0.75rem; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                "© " + LocalDate.now().getYear() + " DriveFlow Car Rental System. Need assistance? Contact us via support portal." +
                "</div></div></body></html>";

        dispatchHtmlEmail(toEmail, subject, html, "Customer Booking Confirmation");
    }

    private void dispatchHtmlEmail(String toEmail, String subject, String htmlContent, String triggerType) {
        log.info("================================================================================");
        log.info("📧 [AUTOMATED EMAIL DISPATCH TRIGGERED: {}]", triggerType);
        log.info("Recipient : {}", toEmail);
        log.info("Subject   : {}", subject);
        log.info("================================================================================");

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send email: recipient address is empty.");
            return;
        }

        if (mailSender == null) {
            log.warn("JavaMailSender bean is not configured. Email logged but not dispatched to SMTP.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "notifications@driveflow.com";
            helper.setFrom(sender, "DriveFlow Car Rental System");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true sets Content-Type to text/html

            mailSender.send(message);
            log.info("✅ Successfully sent HTML email ({}) to {}", triggerType, toEmail);
        } catch (Exception ex) {
            log.error("❌ Failed to deliver email ({}) to {}: {}", triggerType, toEmail, ex.getMessage(), ex);
        }
    }
}
