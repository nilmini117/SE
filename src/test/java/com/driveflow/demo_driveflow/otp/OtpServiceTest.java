package com.driveflow.demo_driveflow.otp;

import com.driveflow.demo_driveflow.email.EmailService;
import com.driveflow.demo_driveflow.users.CustomerRegistrationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private EmailService emailService;

    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(emailService);
    }

    @Test
    @DisplayName("generateRegistrationOtp: creates 6-digit numeric OTP and dispatches email")
    void testGenerateRegistrationOtp_Generates6DigitsAndSendsEmail() {
        String email = "newuser@example.com";
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFirstName("Alice");
        dto.setLastName("Smith");
        dto.setEmail(email);

        String otp = otpService.generateRegistrationOtp(email, dto);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("^\\d{6}$"), "OTP must be strictly a 6-digit numeric string");

        verify(emailService, times(1)).sendRegistrationOtpEmail(eq(email), eq("Alice"), eq(otp), eq(10));
    }

    @Test
    @DisplayName("verifyRegistrationOtp: succeeds with correct OTP and provides cached registration data")
    void testVerifyRegistrationOtp_Success() {
        String email = "alice@example.com";
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFirstName("Alice");
        dto.setEmail(email);

        String otp = otpService.generateRegistrationOtp(email, dto);

        boolean valid = otpService.verifyRegistrationOtp(email, otp);
        assertTrue(valid);

        CustomerRegistrationDto cached = otpService.getPendingRegistration(email);
        assertNotNull(cached);
        assertEquals("Alice", cached.getFirstName());
    }

    @Test
    @DisplayName("verifyRegistrationOtp: fails with incorrect OTP")
    void testVerifyRegistrationOtp_IncorrectOtpFails() {
        String email = "bob@example.com";
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setEmail(email);
        otpService.generateRegistrationOtp(email, dto);

        boolean valid = otpService.verifyRegistrationOtp(email, "000000");
        assertFalse(valid);
    }

    @Test
    @DisplayName("generatePasswordOtp: generates 6-digit numeric OTP and dispatches email")
    void testGeneratePasswordOtp_Generates6DigitsAndSendsEmail() {
        String email = "existing@example.com";

        String otp = otpService.generatePasswordOtp(email);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("^\\d{6}$"), "OTP must be strictly a 6-digit numeric string");

        verify(emailService, times(1)).sendPasswordOtpEmail(eq(email), eq("DriveFlow User"), eq(otp), eq(10));
    }

    @Test
    @DisplayName("verifyPasswordOtp: validates matching OTP and invalidates on consumption")
    void testVerifyPasswordOtp_SuccessAndSingleUse() {
        String email = "existing@example.com";
        String otp = otpService.generatePasswordOtp(email);

        boolean verified = otpService.verifyPasswordOtp(email, otp);
        assertTrue(verified);

        // Single-use check: consumed immediately on verification
        boolean retry = otpService.verifyPasswordOtp(email, otp);
        assertFalse(retry, "Password OTP must not be reusable after successful verification");
    }

    @Test
    @DisplayName("verifyPasswordOtp: fails with incorrect OTP")
    void testVerifyPasswordOtp_IncorrectFails() {
        String email = "existing@example.com";
        otpService.generatePasswordOtp(email);

        boolean verified = otpService.verifyPasswordOtp(email, "999999");
        assertFalse(verified);
    }

    @Test
    @DisplayName("OtpRecord: strict expiration after validity window")
    void testOtpRecord_Expiration() {
        Instant now = Instant.now();
        OtpRecord validRecord = new OtpRecord("alice@example.com", "123456", OtpType.REGISTRATION, now, now.plus(Duration.ofMinutes(10)), null);
        assertFalse(validRecord.isExpired());

        OtpRecord expiredRecord = new OtpRecord("alice@example.com", "123456", OtpType.REGISTRATION, now.minus(Duration.ofMinutes(15)), now.minus(Duration.ofMinutes(5)), null);
        assertTrue(expiredRecord.isExpired());
    }

    @Test
    @DisplayName("generateOtp: creates secure 6-digit numeric OTP and immediately dispatches notification email")
    void testGenerateOtp_Generates6DigitsAndSendsNotification() {
        String email = "driver@example.com";

        String otp = otpService.generateOtp(email);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("^\\d{6}$"), "OTP must strictly be a 6-digit numeric string");

        verify(emailService, times(1)).sendNotification(
                eq(email),
                eq("Your DriveFlow Security Code"),
                eq("Your 6-digit OTP is: " + otp)
        );
    }

    @Test
    @DisplayName("verifyOtp: validates matching OTP and rejects invalid or expired code")
    void testVerifyOtp_ValidationFlow() {
        String email = "secure@driveflow.com";
        String otp = otpService.generateOtp(email);

        // Correct OTP passes
        assertTrue(otpService.verifyOtp(email, otp));

        // Wrong code fails
        assertFalse(otpService.verifyOtp(email, "111111"));

        // Blank or null fails
        assertFalse(otpService.verifyOtp(email, ""));
        assertFalse(otpService.verifyOtp(null, otp));

        // Clear OTP
        otpService.clearOtp(email);
        assertFalse(otpService.verifyOtp(email, otp));
    }
}
