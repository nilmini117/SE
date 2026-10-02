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

    /**
     * Automatically triggers an email notification to the assigned maintenance company
     * upon saving a vehicle maintenance schedule.
     *
     * @param toEmail          Recipient maintenance company email address
     * @param companyName      Maintenance company name
     * @param vehicleDetails   Vehicle model and registration number
     * @param serviceDate      Scheduled service date
     * @param approximatedCost Approximated service cost
     */
    void sendMaintenanceNotificationEmail(String toEmail, String companyName, String vehicleDetails,
                                         java.time.LocalDate serviceDate, java.math.BigDecimal approximatedCost);
}
