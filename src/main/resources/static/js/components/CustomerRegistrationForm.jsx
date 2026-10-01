import React, { useState } from 'react';

/**
 * DriveFlow Customer Registration Component (React)
 *
 * Enforces strict client-side field validations:
 * - nic_number: Exactly 12 characters, integers only (blocks letters/specials)
 * - mobile_number: Exactly 10 characters, integers only
 * - password: Minimum 8 characters, combination of letters and numbers
 */
export default function CustomerRegistrationForm({ onSuccessRedirect = '/login?registered=true' }) {
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

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');
  const [serverSuccess, setServerSuccess] = useState('');

  // 1. Validation Logic
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
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Please enter a valid email address.';
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

    // Real-time integer-only enforcement for NIC and Mobile
    if (name === 'nic_number') {
      sanitizedValue = value.replace(/\D/g, '').slice(0, 12);
    } else if (name === 'mobile_number') {
      sanitizedValue = value.replace(/\D/g, '').slice(0, 10);
    }

    setFormData((prev) => ({ ...prev, [name]: sanitizedValue }));

    // Immediate inline validation feedback
    const errorMsg = validateField(name, sanitizedValue);
    setErrors((prev) => ({ ...prev, [name]: errorMsg }));
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    const errorMsg = validateField(name, value);
    setErrors((prev) => ({ ...prev, [name]: errorMsg }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerError('');
    setServerSuccess('');

    // Full form validation check before submission
    const newErrors = {};
    Object.keys(formData).forEach((key) => {
      const err = validateField(key, formData[key]);
      if (err) newErrors[key] = err;
    });

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (!response.ok) {
        if (data.fieldErrors) {
          setErrors(data.fieldErrors);
        }
        setServerError(data.message || 'Registration failed. Please check the entered fields.');
        setIsSubmitting(false);
        return;
      }

      setServerSuccess('Registration successful! log in succes welcome to drive flow');
      setTimeout(() => {
        window.location.href = onSuccessRedirect;
      }, 1500);
    } catch (err) {
      setServerError('Network error while processing registration. Please try again.');
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '640px', margin: '2rem auto', padding: '2rem', background: '#fff', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}>
      <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a', marginBottom: '0.5rem' }}>Create DriveFlow Account</h2>
      <p style={{ fontSize: '0.875rem', color: '#64748b', marginBottom: '1.5rem' }}>Fill in your verified credentials to book vehicles in our park.</p>

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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.firstName ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.lastName ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.email ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.mobile_number ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.nic_number ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
            />
            {errors.nic_number && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.nic_number}</div>}
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
              Driving License Number <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="text"
              name="drivingLicense"
              value={formData.drivingLicense}
              onChange={handleChange}
              onBlur={handleBlur}
              maxLength={7}
              placeholder="e.g. B123456"
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.drivingLicense ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
            />
            {errors.drivingLicense && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.drivingLicense}</div>}
          </div>
        </div>

        {/* Password & Confirm */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.password ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
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
              style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '6px', border: errors.confirmPassword ? '1px solid #dc2626' : '1px solid #cbd5e1', outline: 'none' }}
            />
            {errors.confirmPassword && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginTop: '0.25rem' }}>{errors.confirmPassword}</div>}
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          style={{ width: '100%', padding: '0.85rem', background: isSubmitting ? '#94a3b8' : '#4f46e5', color: '#fff', fontWeight: 700, borderRadius: '8px', border: 'none', cursor: isSubmitting ? 'not-allowed' : 'pointer' }}
        >
          {isSubmitting ? 'Processing Registration...' : 'Complete Customer Registration →'}
        </button>
      </form>
    </div>
  );
}
