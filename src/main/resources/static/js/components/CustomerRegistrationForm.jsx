import React, { useState, useEffect } from 'react';

/**
 * DriveFlow Customer Registration Component (React)
 *
 * Implements:
 * 1. State Management & Cooldown Timer:
 *    - otpRequested (boolean), cooldown (number, default 0), otpError (string)
 *    - POST /api/auth/otp/send with user email
 *    - On 200 OK: immediately disable button and set cooldown to 60
 *    - 60-second decrementing timer via useEffect with setInterval
 *    - Dynamic button text: "Send OTP to My Email" -> "Resend OTP in [X]s" -> "Resend OTP"
 * 2. Form Submission & API Integration:
 *    - 6-Digit OTP input restricted to numeric characters only (max length 6)
 *    - Rejects letters and special characters
 *    - Injects OTP state into JSON registration payload alongside form data
 *    - Catch block: renders otpError in red typography directly below OTP input on 400 Bad Request
 * 3. Post-Transaction Cleanup:
 *    - On 201 Created, clears OTP state, resets timer, and routes user to success view
 */
export default function CustomerRegistrationForm({
  onSuccessRedirect = '/login?registered=true',
  onSuccess = null
}) {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    mobile_number: '',
    nic_number: '',
    drivingLicense: '',
    dob: '',
    password: '',
    confirmPassword: '',
  });

  // 1. State Management
  const [otpRequested, setOtpRequested] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [otpError, setOtpError] = useState('');
  const [otp, setOtp] = useState('');

  const [errors, setErrors] = useState({});
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');
  const [serverSuccess, setServerSuccess] = useState('');

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

  // Validation Logic
  const validateField = (name, value) => {
    switch (name) {
      case 'nic_number':
        if (!value.trim()) return 'NIC number is required.';
        if (!/^\d+$/.test(value)) return 'NIC number must contain only integers (no letters or special characters).';
        if (value.length !== 12) return `NIC number must be exactly 12 characters long (currently ${value.length}).`;
        return '';

      case 'mobile_number':
        if (!value.trim()) return 'Mobile number is required.';
        if (!/^\d+$/.test(value)) return 'Mobile number must contain only integers.';
        if (value.length !== 10) return `Mobile number must be exactly 10 characters long (currently ${value.length}).`;
        return '';

      case 'password':
        if (!value) return 'Password is required.';
        if (value.length < 8) return `Password must be at least 8 characters long (currently ${value.length}).`;
        if (!/[a-zA-Z]/.test(value) || !/[0-9]/.test(value)) {
          return 'Password must contain a combination of both letters and numbers.';
        }
        return '';

      case 'confirmPassword':
        if (value !== formData.password) return 'Passwords do not match.';
        return '';

      case 'firstName':
        return !value.trim() ? 'First name is required.' : '';

      case 'lastName':
        return !value.trim() ? 'Last name is required.' : '';

      case 'email':
        if (!value.trim()) return 'Email address is required.';
        if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value.trim())) {
          return 'Please enter a valid email address';
        }
        return '';

      case 'drivingLicense':
        if (!value || !value.trim()) return 'Driving license number is required.';
        const dlVal = value.trim();
        if (!/^[A-Za-z]\d{6}$/.test(dlVal)) {
          if (dlVal.length !== 7) {
            return `Driving license must be exactly 7 characters long (currently ${dlVal.length}).`;
          }
          if (!/^[A-Za-z]/.test(dlVal)) {
            return 'Driving license must start with an English letter (A-Z or a-z).';
          }
          return 'The remaining six characters of the driving license must be numeric digits (0-9).';
        }
        return '';

      default:
        return '';
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    let sanitizedValue = value;

    if (name === 'nic_number') {
      sanitizedValue = value.replace(/\D/g, '').slice(0, 12);
    } else if (name === 'mobile_number') {
      sanitizedValue = value.replace(/\D/g, '').slice(0, 10);
    }

    setFormData((prev) => ({ ...prev, [name]: sanitizedValue }));
    const errorMsg = validateField(name, sanitizedValue);
    setErrors((prev) => ({ ...prev, [name]: errorMsg }));
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    const errorMsg = validateField(name, value);
    setErrors((prev) => ({ ...prev, [name]: errorMsg }));
  };

  // Primary Action: Request / Send 6-Digit OTP to Email
  const handleSendOtp = async () => {
    const targetEmail = (formData.email || '').trim();
    if (!targetEmail || !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(targetEmail)) {
      setErrors((prev) => ({ ...prev, email: 'Please enter a valid email address before requesting an OTP code.' }));
      setServerError('Please enter a valid email address before requesting an OTP code.');
      return;
    }

    setOtpError('');
    setServerError('');
    setServerSuccess('');
    setIsSendingOtp(true);

    try {
      const response = await fetch('/api/auth/otp/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: targetEmail })
      });

      const data = await response.json().catch(() => ({}));

      if (response.ok && (data.status === 200 || data.success)) {
        // On a successful 200 OK response, immediately disable the button and set cooldown state to 60
        setOtpRequested(true);
        setCooldown(60);
        setServerSuccess(`A 6-digit verification code has been dispatched to ${targetEmail} (valid for 10 minutes).`);
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

  // 2. Form Submission & API Integration
  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setServerSuccess('');
    setOtpError('');

    // Validate all form fields
    const newErrors = {};
    Object.keys(formData).forEach((key) => {
      const err = validateField(key, formData[key]);
      if (err) newErrors[key] = err;
    });

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      setServerError('Please fix the validation errors before submitting.');
      return;
    }

    const cleanOtp = (otp || '').trim();
    if (!cleanOtp) {
      setOtpError('6-digit OTP verification code is required.');
      return;
    }

    if (cleanOtp.length !== 6 || !/^\d{6}$/.test(cleanOtp)) {
      setOtpError('Verification code must be exactly 6 numeric digits.');
      return;
    }

    setIsSubmitting(true);

    // Injects the OTP state into the JSON payload alongside standard form data
    const payload = {
      ...formData,
      email: formData.email.trim(),
      otp: cleanOtp
    };

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json().catch(() => ({}));

      // Catch 400 Bad Request or error responses
      if (!response.ok || response.status === 400 || data.status === 400 || !data.success) {
        const errorMsg = data.message || 'Registration failed. Please check your details.';
        if (data.fieldErrors) {
          setErrors(data.fieldErrors);
        }
        // If the backend returns 400 Bad Request indicating the OTP is invalid or expired,
        // render the otpError string in red typography directly below the OTP input field
        if (errorMsg.toLowerCase().includes('otp') || errorMsg.toLowerCase().includes('code') || errorMsg.toLowerCase().includes('verification') || response.status === 400) {
          setOtpError(errorMsg);
        } else {
          setServerError(errorMsg);
        }
        setIsSubmitting(false);
        return;
      }

      // 3. Post-Transaction Cleanup:
      // Upon receiving 201 Created (or 200 OK), clear OTP input state, reset timer, and route user
      setOtp('');
      setCooldown(0);
      setOtpRequested(false);
      setOtpError('');
      setServerSuccess('Registration successful! log in succes welcome to drive flow');

      setTimeout(() => {
        if (onSuccess) {
          onSuccess(data);
        } else if (onSuccessRedirect) {
          window.location.href = onSuccessRedirect;
        }
      }, 1500);
    } catch (err) {
      // Catch block on form submission API call
      setOtpError(err.message || 'Network error while submitting registration. Please try again.');
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '640px', margin: '2rem auto', padding: '2.25rem', background: '#fff', borderRadius: '16px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.06)', fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif" }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
        <div style={{ width: '38px', height: '38px', borderRadius: '8px', background: '#4f46e5', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 800, fontSize: '1rem' }}>
          DF
        </div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a', margin: 0, letterSpacing: '-0.02em' }}>Create DriveFlow Account</h2>
      </div>
      <p style={{ fontSize: '0.88rem', color: '#64748b', marginBottom: '1.5rem' }}>
        Fill in your verified credentials and verify your email via a 6-digit OTP code to join DriveFlow.
      </p>

      {serverError && (
        <div style={{ padding: '0.75rem 1rem', background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', borderRadius: '8px', marginBottom: '1.25rem', fontSize: '0.875rem' }}>
          ⚠️ {serverError}
        </div>
      )}

      {serverSuccess && (
        <div style={{ padding: '0.75rem 1rem', background: '#ecfdf5', border: '1px solid #a7f3d0', color: '#065f46', borderRadius: '8px', marginBottom: '1.25rem', fontSize: '0.875rem' }}>
          ✅ {serverSuccess}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        {/* First & Last Name */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              First Name <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="text"
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="e.g. Jane"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.firstName ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.firstName && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.firstName}</div>}
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Last Name <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="text"
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="e.g. Smith"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.lastName ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.lastName && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.lastName}</div>}
          </div>
        </div>

        {/* Email & Mobile */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Email Address <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="jane@example.com"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.email ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.email && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.email}</div>}
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Mobile Number <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="tel"
              name="mobile_number"
              value={formData.mobile_number}
              onChange={handleChange}
              onBlur={handleBlur}
              maxLength={10}
              placeholder="e.g. 0771234567"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.mobile_number ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.mobile_number && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.mobile_number}</div>}
          </div>
        </div>

        {/* NIC & License */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              NIC Number <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="text"
              name="nic_number"
              value={formData.nic_number}
              onChange={handleChange}
              onBlur={handleBlur}
              maxLength={12}
              placeholder="e.g. 199512345678"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.nic_number ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.nic_number && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.nic_number}</div>}
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Driving License <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="text"
              name="drivingLicense"
              value={formData.drivingLicense}
              onChange={handleChange}
              onBlur={handleBlur}
              maxLength={7}
              placeholder="e.g. B123456"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.drivingLicense ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.drivingLicense && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.drivingLicense}</div>}
          </div>
        </div>

        {/* Date of Birth */}
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
            Date of Birth
          </label>
          <input
            type="date"
            name="dob"
            value={formData.dob}
            onChange={handleChange}
            style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
          />
        </div>

        {/* Password & Confirm */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Password (min 8 chars, letters &amp; numbers) <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="At least 8 chars"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.password ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.password && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.password}</div>}
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Confirm Password <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Re-enter password"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.confirmPassword ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none', boxSizing: 'border-box' }}
            />
            {errors.confirmPassword && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.confirmPassword}</div>}
          </div>
        </div>

        {/* 1. OTP Request & Cooldown Timer Section */}
        <div
          style={{
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: '12px',
            padding: '1.25rem',
            marginBottom: '1.25rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '1rem'
          }}
        >
          <div style={{ flex: '1 1 260px' }}>
            <strong style={{ color: '#0f172a', fontSize: '0.92rem', display: 'block', marginBottom: '0.2rem' }}>
              Security OTP Verification <span style={{ color: '#dc2626' }}>*</span>
            </strong>
            <span style={{ color: '#64748b', fontSize: '0.82rem', lineHeight: '1.4', display: 'block' }}>
              A 6-digit verification code will be dispatched to {formData.email ? <strong>{formData.email}</strong> : 'your email'}.
            </span>
          </div>

          <div>
            <button
              type="button"
              id="sendRegistrationOtpBtn"
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

        {/* 2. 6-Digit Verification Code (OTP) Input Field */}
        <div style={{ marginBottom: '1.5rem' }}>
          <label htmlFor="regOtpInput" style={{ display: 'block', fontSize: '0.88rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
            6-Digit Verification Code (OTP) <span style={{ color: '#dc2626' }}>*</span>
          </label>
          <input
            id="regOtpInput"
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
              role="alert"
              style={{
                color: '#dc2626',
                fontSize: '0.84rem',
                fontWeight: 600,
                marginTop: '0.4rem',
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

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isSubmitting}
          style={{
            width: '100%',
            padding: '0.85rem',
            background: isSubmitting ? '#94a3b8' : '#4f46e5',
            color: '#fff',
            fontWeight: 700,
            borderRadius: '8px',
            border: 'none',
            cursor: isSubmitting ? 'not-allowed' : 'pointer',
            fontSize: '1rem',
            boxShadow: '0 2px 4px rgba(79, 70, 229, 0.25)',
            transition: 'background 0.15s ease'
          }}
        >
          {isSubmitting ? 'Submitting Registration...' : 'Complete Customer Registration →'}
        </button>
      </form>
    </div>
  );
}

export { CustomerRegistrationForm };
