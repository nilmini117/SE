package com.driveflow.demo_driveflow.otp;

import com.driveflow.demo_driveflow.users.CustomerRegistrationDto;

import java.time.Instant;

/**
 * Encapsulates an active OTP record with its purpose, expiration timestamp,
 * and optional payload (such as pending registration data).
 */
public class OtpRecord {

    private final String email;
    private final String otp;
    private final OtpType type;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final CustomerRegistrationDto pendingData;

    public OtpRecord(String email, String otp, OtpType type, Instant createdAt, Instant expiresAt, CustomerRegistrationDto pendingData) {
        this.email = email;
        this.otp = otp;
        this.type = type;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.pendingData = pendingData;
    }

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }

    public OtpType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public CustomerRegistrationDto getPendingData() {
        return pendingData;
    }

    /**
     * Checks if the OTP has exceeded its strict expiration window.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
