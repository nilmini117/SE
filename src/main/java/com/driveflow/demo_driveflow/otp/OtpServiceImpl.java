package com.driveflow.demo_driveflow.otp;

import com.driveflow.demo_driveflow.email.EmailService;
import com.driveflow.demo_driveflow.users.CustomerRegistrationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    public static final int OTP_EXPIRY_MINUTES = 10;
    private static final Duration EXPIRATION_WINDOW = Duration.ofMinutes(OTP_EXPIRY_MINUTES);

    /**
     * Max wrong guesses before the email is locked out and its codes are
     * invalidated.
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    /** Minimum gap between two OTP emails to the same address (anti email-spam). */
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    private static final class AttemptState {
        int failures;
        Instant lockedUntil;
    }

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpRecord> otpCache = new ConcurrentHashMap<>();
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();

    private boolean isLockedOut(String normalizedEmail) {
        AttemptState state = attempts.get(normalizedEmail);
        if (state == null || state.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(state.lockedUntil)) {
            attempts.remove(normalizedEmail);
            return false;
        }
        return true;
    }

    private void registerFailure(String normalizedEmail) {
        boolean[] locked = { false };
        attempts.compute(normalizedEmail, (k, state) -> {
            if (state == null) {
                state = new AttemptState();
            }
            state.failures++;
            if (state.failures >= MAX_FAILED_ATTEMPTS) {
                state.failures = 0;
                state.lockedUntil = Instant.now().plus(LOCKOUT_DURATION);
                locked[0] = true;
            }
            return state;
        });
        if (locked[0]) {
            log.warn("Too many wrong OTP attempts for {}. Locked for {} minutes and codes invalidated.",
                    normalizedEmail, LOCKOUT_DURATION.toMinutes());
            clearOtp(normalizedEmail);
        }
    }

    private void resetAttempts(String normalizedEmail) {
        attempts.remove(normalizedEmail);
    }

    private boolean codesMatch(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.trim().getBytes(StandardCharsets.UTF_8),
                provided.trim().getBytes(StandardCharsets.UTF_8));
    }

    private void enforceResendCooldown(String normalizedEmail) {
        Instant now = Instant.now();
        Instant previous = lastSent.get(normalizedEmail);
        if (previous != null && now.isBefore(previous.plus(RESEND_COOLDOWN))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait a few seconds before requesting another code.");
        }
        lastSent.put(normalizedEmail, now);
    }

    @Autowired(required = false)
    private EmailService emailService;

    public OtpServiceImpl() {
    }

    public OtpServiceImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    private String buildCacheKey(String email, OtpType type) {
        if (email == null)
            return "unknown:" + type.name();
        return email.trim().toLowerCase() + ":" + type.name();
    }

    private String generateSecureNumericCode() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    @Override
    public String generateRegistrationOtp(String email, CustomerRegistrationDto dto) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required to generate registration OTP.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        enforceResendCooldown(normalizedEmail);
        String otp = generateSecureNumericCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(EXPIRATION_WINDOW);

        OtpRecord record = new OtpRecord(normalizedEmail, otp, OtpType.REGISTRATION, now, expiresAt, dto);
        otpCache.put(buildCacheKey(normalizedEmail, OtpType.REGISTRATION), record);
        otpCache.put(normalizedEmail, record);

        log.info("🔐 Generated 6-digit registration OTP for {} (expires in {} minutes).", normalizedEmail,
                OTP_EXPIRY_MINUTES);
        log.info("📧 [REGISTRATION OTP DISPATCH] Recipient: {} | Code: {}", normalizedEmail, otp);

        if (emailService != null) {
            String name = (dto != null && dto.getFirstName() != null) ? dto.getFirstName() : "Driver";
            emailService.sendRegistrationOtpEmail(normalizedEmail, name, otp, OTP_EXPIRY_MINUTES);
            emailService.sendNotification(normalizedEmail, "Your DriveFlow Security Code",
                    "Your 6-digit OTP is: " + otp);
        }

        return otp;
    }

    @Override
    public String generateOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required to generate OTP.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        enforceResendCooldown(normalizedEmail);
        String otp = generateSecureNumericCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(EXPIRATION_WINDOW);

        OtpRecord record = new OtpRecord(normalizedEmail, otp, OtpType.GENERAL, now, expiresAt, null);
        otpCache.put(normalizedEmail, record);
        otpCache.put(buildCacheKey(normalizedEmail, OtpType.GENERAL), record);

        log.info("🔐 Generated 6-digit OTP for {} (expires in {} minutes).", normalizedEmail, OTP_EXPIRY_MINUTES);
        log.info("📧 [SECURITY OTP DISPATCH] Recipient: {} | Code: {}", normalizedEmail, otp);

        if (emailService != null) {
            emailService.sendNotification(normalizedEmail, "Your DriveFlow Security Code",
                    "Your 6-digit OTP is: " + otp);
        }

        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null || otp.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (isLockedOut(normalizedEmail)) {
            log.warn("OTP verification blocked: {} is temporarily locked.", normalizedEmail);
            return false;
        }
        OtpRecord record = otpCache.get(normalizedEmail);
        String foundKey = normalizedEmail;

        if (record == null) {
            record = otpCache.get(buildCacheKey(normalizedEmail, OtpType.GENERAL));
            foundKey = buildCacheKey(normalizedEmail, OtpType.GENERAL);
        }
        if (record == null) {
            record = otpCache.get(buildCacheKey(normalizedEmail, OtpType.REGISTRATION));
            foundKey = buildCacheKey(normalizedEmail, OtpType.REGISTRATION);
        }
        if (record == null) {
            record = otpCache.get(buildCacheKey(normalizedEmail, OtpType.PASSWORD_CHANGE));
            foundKey = buildCacheKey(normalizedEmail, OtpType.PASSWORD_CHANGE);
        }
        if (record == null) {
            record = otpCache.get(buildCacheKey(normalizedEmail, OtpType.PASSWORD_RESET));
            foundKey = buildCacheKey(normalizedEmail, OtpType.PASSWORD_RESET);
        }

        if (record == null) {
            log.warn("OTP verification failed: no active OTP record found for {}", normalizedEmail);
            return false;
        }

        if (record.isExpired()) {
            log.warn("OTP for {} has EXPIRED (generated at {}, expired at {})",
                    normalizedEmail, record.getCreatedAt(), record.getExpiresAt());
            otpCache.remove(foundKey);
            otpCache.remove(normalizedEmail);
            return false;
        }

        boolean matches = codesMatch(record.getOtp(), otp);
        if (matches) {
            log.info("✅ OTP verified successfully for {}", normalizedEmail);
            resetAttempts(normalizedEmail);
        } else {
            log.warn("❌ OTP mismatch for {}", normalizedEmail);
            registerFailure(normalizedEmail);
        }

        return matches;
    }

    @Override
    public void clearOtp(String email) {
        if (email == null)
            return;
        String normalizedEmail = email.trim().toLowerCase();
        otpCache.remove(normalizedEmail);
        otpCache.remove(buildCacheKey(normalizedEmail, OtpType.GENERAL));
        otpCache.remove(buildCacheKey(normalizedEmail, OtpType.REGISTRATION));
        otpCache.remove(buildCacheKey(normalizedEmail, OtpType.PASSWORD_CHANGE));
        otpCache.remove(buildCacheKey(normalizedEmail, OtpType.PASSWORD_RESET));
    }

    @Override
    public boolean verifyRegistrationOtp(String email, String otp) {
        if (email == null || otp == null || otp.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (isLockedOut(normalizedEmail)) {
            log.warn("OTP verification blocked: {} is temporarily locked.", normalizedEmail);
            return false;
        }
        String cacheKey = buildCacheKey(normalizedEmail, OtpType.REGISTRATION);
        OtpRecord record = otpCache.get(cacheKey);
        if (record == null) {
            record = otpCache.get(normalizedEmail);
        }

        if (record == null) {
            log.warn("Registration OTP verification failed: no active OTP record found for {}", normalizedEmail);
            return false;
        }

        if (record.isExpired()) {
            log.warn("Registration OTP for {} has EXPIRED (generated at {}, expired at {})",
                    normalizedEmail, record.getCreatedAt(), record.getExpiresAt());
            otpCache.remove(cacheKey);
            otpCache.remove(normalizedEmail);
            return false;
        }

        boolean matches = codesMatch(record.getOtp(), otp);
        if (matches) {
            log.info("✅ Registration OTP verified successfully for {}", normalizedEmail);
            resetAttempts(normalizedEmail);
        } else {
            log.warn("❌ Registration OTP mismatch for {}", normalizedEmail);
            registerFailure(normalizedEmail);
        }

        return matches;
    }

    @Override
    public CustomerRegistrationDto getPendingRegistration(String email) {
        if (email == null)
            return null;
        String normalizedEmail = email.trim().toLowerCase();
        OtpRecord record = otpCache.get(buildCacheKey(normalizedEmail, OtpType.REGISTRATION));
        return (record != null) ? record.getPendingData() : null;
    }

    @Override
    public String generatePasswordOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required to generate password OTP.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        enforceResendCooldown(normalizedEmail);
        String otp = generateSecureNumericCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(EXPIRATION_WINDOW);

        OtpRecord record = new OtpRecord(normalizedEmail, otp, OtpType.PASSWORD_CHANGE, now, expiresAt, null);
        otpCache.put(buildCacheKey(normalizedEmail, OtpType.PASSWORD_CHANGE), record);
        otpCache.put(normalizedEmail, record);

        log.info("🔐 Generated 6-digit password OTP for {} (expires in {} minutes).", normalizedEmail,
                OTP_EXPIRY_MINUTES);

        if (emailService != null) {
            emailService.sendPasswordOtpEmail(normalizedEmail, "DriveFlow User", otp, OTP_EXPIRY_MINUTES);
            emailService.sendNotification(normalizedEmail, "Your DriveFlow Security Code",
                    "Your 6-digit OTP is: " + otp);
        }

        return otp;
    }

    @Override
    public boolean verifyPasswordOtp(String email, String otp) {
        if (email == null || otp == null || otp.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (isLockedOut(normalizedEmail)) {
            log.warn("OTP verification blocked: {} is temporarily locked.", normalizedEmail);
            return false;
        }
        String changeKey = buildCacheKey(normalizedEmail, OtpType.PASSWORD_CHANGE);
        String resetKey = buildCacheKey(normalizedEmail, OtpType.PASSWORD_RESET);

        OtpRecord record = otpCache.get(changeKey);
        String activeKey = changeKey;
        if (record == null) {
            record = otpCache.get(resetKey);
            activeKey = resetKey;
        }
        if (record == null) {
            record = otpCache.get(normalizedEmail);
            activeKey = normalizedEmail;
        }

        if (record == null) {
            log.warn("Password OTP verification failed: no active OTP record found for {}", normalizedEmail);
            return false;
        }

        if (record.isExpired()) {
            log.warn("Password OTP for {} has EXPIRED", normalizedEmail);
            otpCache.remove(activeKey);
            otpCache.remove(normalizedEmail);
            return false;
        }

        boolean matches = codesMatch(record.getOtp(), otp);
        if (matches) {
            log.info("✅ Password OTP verified successfully for {}", normalizedEmail);
            resetAttempts(normalizedEmail);
            otpCache.remove(activeKey);
            otpCache.remove(normalizedEmail);
            return true;
        }

        log.warn("❌ Password OTP mismatch for {}", normalizedEmail);

        registerFailure(normalizedEmail);
        return false;
    }

    @Override
    public boolean isOtpValid(String email, String otp, OtpType type) {
        if (email == null || otp == null || type == null)
            return false;
        String normalizedEmail = email.trim().toLowerCase();
        if (isLockedOut(normalizedEmail)) {
            log.warn("OTP verification blocked: {} is temporarily locked.", normalizedEmail);
            return false;
        }
        String key = buildCacheKey(normalizedEmail, type);
        OtpRecord record = otpCache.get(key);
        if (record == null || record.isExpired()) {
            if (record != null)
                otpCache.remove(key);
            return false;
        }
        boolean matches = codesMatch(record.getOtp(), otp);
        if (matches) {
            resetAttempts(normalizedEmail);
        } else {
            registerFailure(normalizedEmail);
        }
        return matches;
    }

    @Override
    public void clearOtp(String email, OtpType type) {
        if (email == null || type == null)
            return;
        otpCache.remove(buildCacheKey(email.trim().toLowerCase(), type));
    }
}
