import React, { useState, useEffect } from 'react';

/**
 * DriveFlow "Security & Password" React Component
 *
 * Implements:
 * 1. State Management & 60-Second Cooldown Timer:
 *    - otpRequested (boolean), cooldown (number, default 0), otpError (string)
 *    - POST /api/auth/otp/send with user email
 *    - 60-second decrementing timer via useEffect with setInterval
 *    - Dynamic button text: "Send OTP to My Email" -> "Resend OTP in [X]s" -> "Resend OTP"
 * 2. Form Submission & API Integration:
 *    - 6-Digit OTP input restricted to digits only (max length 6)
 *    - Injects OTP into the password change JSON payload
 *    - Inline red typography error rendering on 400 Bad Request without breaking page routing
 * 3. Post-Transaction Cleanup:
 *    - Clears OTP state, resets timer, and routes user upon 200 OK
 */
export default function SecurityAndPassword({
  userEmail = 'customer@driveflow.com',
  isResetFlow = false,
  onSuccessRedirect = '/profile?passwordUpdated=true',
  onSuccess = null
}) {
  // 1. State Management
  const [email, setEmail] = useState(userEmail);
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [otp, setOtp] = useState('');

  const [otpRequested, setOtpRequested] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [otpError, setOtpError] = useState('');
  const [generalError, setGeneralError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 60-Second Cooldown Timer with setInterval
  useEffect(() => {
    let timer = null;
    if (cooldown > 0) {
      timer = setInterval(() => {
        setCooldown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [cooldown]);

  // Dynamic Button Text Handler
  const getButtonText = () => {
    if (isSendingOtp) return 'Sending OTP...';
    if (cooldown > 0) return `Resend OTP in ${cooldown}s`;
    if (otpRequested) return 'Resend OTP';
    return 'Send OTP to My Email';
  };

  // Primary Action: Request / Send 6-Digit OTP
  const handleSendOtp = async () => {
    const targetEmail = (email || '').trim();
    if (!targetEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(targetEmail)) {
      setGeneralError('Please provide a valid registered email address.');
      return;
    }

    setOtpError('');
    setGeneralError('');
    setSuccessMessage('');
    setIsSendingOtp(true);

    try {
      const response = await fetch('/api/auth/otp/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: targetEmail })
      });

      const data = await response.json();

      if (response.ok && (data.status === 200 || data.success)) {
        setOtpRequested(true);
        setCooldown(60);
        setSuccessMessage(`A 6-digit verification code has been dispatched to ${targetEmail} (valid for 10 minutes).`);
      } else {
        setOtpError(data.message || 'Failed to dispatch verification code. Please try again.');
      }
    } catch (err) {
      setOtpError('Network error while requesting OTP code. Please check your connection.');
    } finally {
      setIsSendingOtp(false);
    }
  };

  // Restrict 6-Digit OTP to numeric characters only
  const handleOtpChange = (e) => {
    const numericOnly = e.target.value.replace(/\D/g, '').slice(0, 6);
    setOtp(numericOnly);
    if (otpError) setOtpError('');
  };

  // Form Submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setOtpError('');
    setGeneralError('');
    setSuccessMessage('');

    if (!isResetFlow && !oldPassword) {
      setGeneralError('Current password is required.');
      return;
    }

    if (!newPassword || newPassword.length < 6) {
      setGeneralError('New password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setGeneralError('New password and confirmation password do not match.');
      return;
    }

    const cleanOtp = otp.trim();
    if (!cleanOtp) {
      setOtpError('6-digit OTP verification code is required.');
      return;
    }

    if (cleanOtp.length !== 6 || !/^\d{6}$/.test(cleanOtp)) {
      setOtpError('Verification code must be exactly 6 numeric digits.');
      return;
    }

    setIsSubmitting(true);

    const payload = isResetFlow
      ? { email: email.trim(), newPassword, otp: cleanOtp }
      : { email: email.trim(), oldPassword, newPassword, confirmPassword, otp: cleanOtp };

    const endpoint = isResetFlow ? '/api/auth/password/reset' : '/api/profile/password';

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (!response.ok || data.status === 400 || !data.success) {
        // If 400 Bad Request relates to OTP or error message
        const msg = data.message || 'Password update failed.';
        if (msg.toLowerCase().includes('otp') || msg.toLowerCase().includes('code') || msg.toLowerCase().includes('verification')) {
          setOtpError(msg);
        } else {
          setGeneralError(msg);
        }
        setIsSubmitting(false);
        return;
      }

      // 3. Post-Transaction Cleanup on 200 OK
      setOtp('');
      setCooldown(0);
      setOtpError('');
      setGeneralError('');
      setSuccessMessage(data.message || 'Password updated successfully!');

      setTimeout(() => {
        if (onSuccess) {
          onSuccess(data);
        } else if (onSuccessRedirect) {
          window.location.href = onSuccessRedirect;
        }
      }, 1200);
    } catch (err) {
      setOtpError('Network error while processing security verification. Please try again.');
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="df-security-password-card"
      style={{
        maxWidth: '680px',
        margin: '0 auto',
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '2rem 2.25rem',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)',
        fontFamily: "'Plus Jakarta Sans', system-ui, -apple-system, sans-serif"
      }}
    >
      {/* Header */}
      <div style={{ marginBottom: '1.75rem' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', color: '#4f46e5', fontSize: '0.82rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.4rem' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
          </svg>
          Security &amp; Password
        </div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.4rem', letterSpacing: '-0.02em' }}>
          {isResetFlow ? 'Reset Account Password' : 'Change Account Password'}
        </h2>
        <p style={{ color: '#64748b', fontSize: '0.9rem', margin: 0 }}>
          Keep your DriveFlow account secure. Transactions are protected by a time-bound 6-digit One-Time Password (OTP) sent to your registered email.
        </p>
      </div>

      {/* General Feedback Alerts */}
      {generalError && (
        <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', padding: '0.85rem 1.25rem', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ fontSize: '1.1rem' }}>⚠️</span>
          <span>{generalError}</span>
        </div>
      )}

      {successMessage && (
        <div style={{ background: '#ecfdf5', border: '1px solid #10b981', color: '#065f46', padding: '0.85rem 1.25rem', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ fontSize: '1.1rem' }}>✅</span>
          <span>{successMessage}</span>
        </div>
      )}

      {/* 1. OTP Request Box */}
      <div
        style={{
          background: '#f8fafc',
          border: '1px solid #e2e8f0',
          borderRadius: '12px',
          padding: '1.25rem 1.5rem',
          marginBottom: '1.75rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem'
        }}
      >
        <div style={{ flex: '1 1 260px' }}>
          <strong style={{ color: '#0f172a', fontSize: '0.95rem', display: 'block', marginBottom: '0.2rem' }}>
            Step 1: Request Security OTP
          </strong>
          <span style={{ color: '#64748b', fontSize: '0.82rem', lineHeight: '1.4', display: 'block' }}>
            A 6-digit verification code will be dispatched to <strong>{email}</strong> (valid for 10 minutes).
          </span>
        </div>

        <div>
          <button
            type="button"
            id="sendOtpBtn"
            onClick={handleSendOtp}
            disabled={cooldown > 0 || isSendingOtp}
            style={{
              padding: '0.55rem 1.1rem',
              fontSize: '0.88rem',
              fontWeight: 600,
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              background: cooldown > 0 ? '#f1f5f9' : '#ffffff',
              color: cooldown > 0 ? '#94a3b8' : '#334155',
              cursor: cooldown > 0 || isSendingOtp ? 'not-allowed' : 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              boxShadow: cooldown > 0 ? 'none' : '0 1px 2px rgba(0,0,0,0.05)',
              transition: 'all 0.15s ease-in-out'
            }}
          >
            <span>📧</span>
            {getButtonText()}
          </button>
        </div>
      </div>

      {/* 2. Password & OTP Form */}
      <form onSubmit={handleSubmit} noValidate>
        {isResetFlow && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Registered Email Address <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. user@driveflow.com"
              required
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '0.95rem',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>
        )}

        {/* Step 2: 6-Digit OTP Input Field */}
        <div style={{ marginBottom: '1.5rem' }}>
          <label htmlFor="otpInput" style={{ display: 'block', fontSize: '0.88rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
            Step 2: 6-Digit Verification Code (OTP) <span style={{ color: '#dc2626' }}>*</span>
          </label>
          <input
            id="otpInput"
            name="otp"
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            value={otp}
            onChange={handleOtpChange}
            onKeyDown={(e) => {
              if (
                !/^\d$/.test(e.key) &&
                !['Backspace', 'Tab', 'Delete', 'ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key) &&
                !e.ctrlKey &&
                !e.metaKey
              ) {
                e.preventDefault();
              }
            }}
            maxLength={6}
            placeholder="e.g. 123456"
            style={{
              letterSpacing: '6px',
              fontWeight: 800,
              fontSize: '1.2rem',
              maxWidth: '220px',
              width: '100%',
              padding: '0.65rem 0.85rem',
              borderRadius: '8px',
              border: otpError ? '2px solid #dc2626' : '1px solid #cbd5e1',
              backgroundColor: otpError ? '#fef2f2' : '#ffffff',
              outline: 'none',
              textAlign: 'center',
              boxSizing: 'border-box',
              display: 'block'
            }}
            required
          />

          {/* Dynamic Error in Red Typography Directly Below Input */}
          {otpError && (
            <div
              id="otpError"
              style={{
                color: '#dc2626',
                fontSize: '0.82rem',
                fontWeight: 600,
                marginTop: '0.35rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.35rem'
              }}
            >
              <span>⚠️</span>
              <span>{otpError}</span>
            </div>
          )}

          <small style={{ color: '#64748b', fontSize: '0.8rem', display: 'block', marginTop: '0.35rem' }}>
            Enter the 6-digit numeric security code received in your inbox.
          </small>
        </div>

        {/* Current Password (only in authenticated change flow) */}
        {!isResetFlow && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Current Password <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
              placeholder="••••••••"
              required
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '0.95rem',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>
        )}

        {/* New Password & Confirm Password */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.75rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              New Password <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              minLength={6}
              placeholder="Min 6 characters"
              required
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '0.95rem',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.88rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Confirm New Password <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              minLength={6}
              placeholder="Re-enter new password"
              required
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '0.95rem',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <button
            type="submit"
            disabled={isSubmitting}
            style={{
              flex: '1',
              padding: '0.8rem 1.5rem',
              background: isSubmitting ? '#94a3b8' : '#4f46e5',
              color: '#ffffff',
              borderRadius: '8px',
              border: 'none',
              fontWeight: 700,
              fontSize: '0.95rem',
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              boxShadow: '0 2px 4px rgba(79, 70, 229, 0.25)',
              transition: 'background 0.15s ease'
            }}
          >
            {isSubmitting ? 'Verifying & Updating...' : 'Update Account Password'}
          </button>
          <a
            href="/profile"
            style={{
              padding: '0.8rem 1.25rem',
              background: '#f1f5f9',
              color: '#475569',
              borderRadius: '8px',
              textDecoration: 'none',
              fontWeight: 600,
              fontSize: '0.95rem',
              border: '1px solid #cbd5e1',
              textAlign: 'center'
            }}
          >
            Cancel
          </a>
        </div>
      </form>
    </div>
  );
}

export { SecurityAndPassword };
