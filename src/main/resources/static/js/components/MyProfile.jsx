import React from 'react';

/**
 * DriveFlow Customer Profile Component (React)
 * 
 * Strict Specification:
 * - Completely removes "My Rental Bookings" section and its data table.
 * - Completely removes the row of sub-navigation pills (My Bookings, Invoices & Bills, Payments & Receipts, Incident Reports).
 * - Strictly contains ONLY:
 *   1. Top user header (Name, Email, action buttons: Report Incident, Edit Profile, Change Password)
 *   2. "Account & Identity Information" details card (NIC, Driving License, Contact, DOB).
 * - No tables, bookings, or billing-related UI elements remain.
 */
export default function MyProfile({ customer = null }) {
  const profileData = customer || {
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    email: 'john.doe@driveflow.com',
    nic: '951234567V',
    drivingLicense: 'B-9876543',
    contactNumber: '+1 (555) 234-5678',
    dob: '1995-04-12'
  };

  const initialLetter = profileData.firstName
    ? profileData.firstName.charAt(0).toUpperCase()
    : 'U';

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '1.5rem', fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif" }}>
      
      {/* 1. Top User Header Card */}
      <div style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '1.75rem 2rem',
        marginBottom: '2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '1.25rem',
        boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #4f46e5, #818cf8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            fontSize: '1.75rem',
            fontWeight: 800,
            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.3)'
          }}>
            {initialLetter}
          </div>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.25rem', letterSpacing: '-0.02em' }}>
              {profileData.name || `${profileData.firstName} ${profileData.lastName}`}
            </h1>
            <p style={{ color: '#64748b', margin: 0, fontSize: '0.95rem' }}>
              {profileData.email}
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <a
            href="/incidents/report"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.5rem 0.9rem',
              background: '#f59e0b',
              color: '#ffffff',
              borderRadius: '8px',
              textDecoration: 'none',
              fontSize: '0.85rem',
              fontWeight: 600,
              boxShadow: '0 2px 4px rgba(245, 158, 11, 0.2)'
            }}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            Report Incident
          </a>
          <a
            href="/profile/edit"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.5rem 0.9rem',
              background: '#4f46e5',
              color: '#ffffff',
              borderRadius: '8px',
              textDecoration: 'none',
              fontSize: '0.85rem',
              fontWeight: 600,
              boxShadow: '0 2px 4px rgba(79, 70, 229, 0.2)'
            }}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
            Edit Profile
          </a>
          <a
            href="/profile/password"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.5rem 0.9rem',
              background: '#f1f5f9',
              color: '#334155',
              border: '1px solid #cbd5e1',
              borderRadius: '8px',
              textDecoration: 'none',
              fontSize: '0.85rem',
              fontWeight: 600
            }}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            Change Password
          </a>
        </div>
      </div>

      {/* 2. Account & Identity Information Details Card */}
      <div style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '1.75rem 2rem',
        boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)'
      }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a', marginBottom: '1.5rem', letterSpacing: '-0.02em' }}>
          Account & Identity Information
        </h2>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '1.75rem',
          background: '#f8fafc',
          padding: '1.5rem',
          borderRadius: '12px',
          border: '1px solid #e2e8f0'
        }}>
          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', display: 'block', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
              National Identity Card (NIC)
            </span>
            <strong style={{ color: '#1e293b', fontSize: '1.05rem' }}>
              {profileData.nic || 'Not specified'}
            </strong>
          </div>

          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', display: 'block', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
              Driving License Number
            </span>
            <code style={{
              background: '#e0e7ff',
              color: '#4338ca',
              padding: '0.25rem 0.6rem',
              borderRadius: '6px',
              fontWeight: 800,
              fontSize: '1rem',
              display: 'inline-block'
            }}>
              {profileData.drivingLicense || 'Not specified'}
            </code>
          </div>

          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', display: 'block', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
              Phone / Contact Number
            </span>
            <span style={{ color: '#1e293b', fontSize: '1.05rem', fontWeight: 600 }}>
              {profileData.contactNumber || 'Not provided'}
            </span>
          </div>

          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', display: 'block', marginBottom: '0.35rem', letterSpacing: '0.04em' }}>
              Date of Birth
            </span>
            <span style={{ color: '#1e293b', fontSize: '1.05rem', fontWeight: 600 }}>
              {profileData.dob || 'Not specified'}
            </span>
          </div>
        </div>
      </div>

    </div>
  );
}

export { MyProfile };
