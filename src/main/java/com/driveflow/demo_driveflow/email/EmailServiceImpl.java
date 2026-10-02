package com.driveflow.demo_driveflow.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    public static final String WELCOME_PHRASE = "log in succes welcome to drive flow";

    @Override
    public void sendWelcomeEmail(String toEmail, String customerName) {
        String subject = WELCOME_PHRASE;
        String body = String.format(
            "Hello %s,\n\n" +
            "Your DriveFlow account has been created successfully!\n\n" +
            "%s\n\n" +
            "You can now log in to the customer portal and pick your choice from our park.\n\n" +
            "Best Regards,\n" +
            "DriveFlow Team",
            (customerName != null && !customerName.isBlank()) ? customerName : "Customer",
            WELCOME_PHRASE
        );

        log.info("================================================================================");
        log.info("📧 [AUTOMATED WELCOME EMAIL TRIGGERED]");
        log.info("Recipient : {}", toEmail);
        log.info("Subject   : {}", subject);
        log.info("Content   :\n{}", body);
        log.info("================================================================================");
    }

    @Override
    public void sendMaintenanceNotificationEmail(String toEmail, String companyName, String vehicleDetails,
                                                java.time.LocalDate serviceDate, java.math.BigDecimal approximatedCost) {
        String subject = "DriveFlow Fleet Maintenance Schedule: " + vehicleDetails;
        String costFormatted = (approximatedCost != null) ? "$" + approximatedCost.toPlainString() : "$0.00";
        String dateFormatted = (serviceDate != null) ? serviceDate.toString() : java.time.LocalDate.now().toString();

        String body = String.format(
            "Dear %s Team,\n\n" +
            "A vehicle from the DriveFlow park has been scheduled for maintenance and allocated to your service company:\n\n" +
            "  • Vehicle Details    : %s\n" +
            "  • Scheduled Date     : %s\n" +
            "  • Approximated Cost  : %s\n\n" +
            "Please ensure your service bay is prepared for vehicle intake. Global vehicle status has been updated to UNAVAILABLE.\n\n" +
            "Best Regards,\n" +
            "DriveFlow Fleet Operations",
            (companyName != null && !companyName.isBlank()) ? companyName : "Maintenance Partner",
            vehicleDetails,
            dateFormatted,
            costFormatted
        );

        log.info("================================================================================");
        log.info("📧 [AUTOMATED MAINTENANCE NOTIFICATION EMAIL TRIGGERED]");
        log.info("Recipient : {}", toEmail);
        log.info("Company   : {}", companyName);
        log.info("Subject   : {}", subject);
        log.info("Content   :\n{}", body);
        log.info("================================================================================");
    }
}
