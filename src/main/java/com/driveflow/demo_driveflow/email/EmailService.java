package com.driveflow.demo_driveflow.email;

public interface EmailService {
    /**
     * Automatically triggers a welcome email upon successful customer registration.
     * Contains the required phrase: "log in succes welcome to drive flow"
     *
     * @param toEmail      Recipient email address
     * @param customerName Recipient customer name
     */
    void sendWelcomeEmail(String toEmail, String customerName);
}
