package com.driveflow.demo_driveflow.otp;

import com.driveflow.demo_driveflow.users.CustomerRegistrationDto;

/**
 * Service managing secure One-Time Password (OTP) generation, verification,
 * and strict expiration lifecycle.
 */
public interface OtpService {

    /**
     * Generates a secure 6-digit numeric OTP for registration, caches the pending registration data,
     * dispatches an asynchronous OTP email to the user, and sets a strict 10-minute expiry window.
     *
     * @param email Recipient email address
     * @param dto   Pending registration form payload
     * @return Generated 6-digit OTP
     */
    String generateRegistrationOtp(String email, CustomerRegistrationDto dto);

    /**
     * Verifies the submitted registration OTP.
     * Rejects if the OTP does not match, does not exist, or has expired.
     *
     * @param email Recipient email address
     * @param otp   Submitted 6-digit OTP code
     * @return true if OTP is valid and within the expiration window
     */
    boolean verifyRegistrationOtp(String email, String otp);

    /**
     * Retrieves the cached pending registration data associated with a verified email.
     *
     * @param email Recipient email address
     * @return Pending CustomerRegistrationDto, or null if absent
     */
    CustomerRegistrationDto getPendingRegistration(String email);

    /**
     * Generates a secure 6-digit numeric OTP for password change or reset,
     * dispatches an asynchronous OTP email to the registered email, and sets a strict 10-minute expiry.
     *
     * @param email Registered user email address
     * @return Generated 6-digit OTP
     */
    String generatePasswordOtp(String email);

    /**
     * Verifies the submitted password change/reset OTP.
     * Rejects if the OTP does not match, does not exist, or has expired.
     *
     * @param email Registered user email address
     * @param otp   Submitted 6-digit OTP code
     * @return true if OTP is valid and within the expiration window
     */
    boolean verifyPasswordOtp(String email, String otp);

    /**
     * Generates a secure, random 6-digit numeric OTP, temporarily caches it mapped
     * to the user's email address with a strict 10-minute expiry, and dispatches the
     * notification email immediately via EmailService.
     *
     * @param email Recipient email address
     * @return Generated 6-digit numeric OTP string
     */
    String generateOtp(String email);

    /**
     * Verifies the submitted OTP against the cache for the given email address.
     * Validates that the OTP matches and has not expired.
     *
     * @param email Recipient email address
     * @param otp   Submitted 6-digit OTP code
     * @return true if OTP matches and is within the 10-minute validity window
     */
    boolean verifyOtp(String email, String otp);

    /**
     * Explicitly clears any cached OTPs for the specified email address.
     *
     * @param email Recipient email address
     */
    void clearOtp(String email);

    /**
     * General OTP verification for any OtpType.
     *
     * @param email Recipient email address
     * @param otp   Submitted OTP code
     * @param type  Purpose/Type of OTP
     * @return true if valid and not expired
     */
    boolean isOtpValid(String email, String otp, OtpType type);

    /**
     * Explicitly clears the cached OTP for an email and purpose.
     */
    void clearOtp(String email, OtpType type);

    /**
     * Generates a 6-digit numeric OTP for vehicle return authorization,
     * caches it mapped to OtpType.VEHICLE_RETURN, and dispatches the authorization email.
     *
     * @param email         Recipient email address
     * @param customerName  Customer display name
     * @param bookingId     Booking ID being returned
     * @param isEarlyReturn Whether this is an early return prior to scheduled end date
     * @return 6-digit numeric OTP
     */
    String generateVehicleReturnOtp(String email, String customerName, Long bookingId, boolean isEarlyReturn);

    /**
     * Verifies the submitted OTP for vehicle return and invalidates it upon success.
     *
     * @param email Recipient email address
     * @param otp   Submitted 6-digit OTP code
     * @return true if valid and not expired
     */
    boolean verifyVehicleReturnOtp(String email, String otp);
}
