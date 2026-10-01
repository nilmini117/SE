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
}
