import React, { useState } from 'react';

/**
 * DriveFlow Staff Schedule Maintenance Form Component (React)
 * 
 * Strict Business Logic:
 * 1. Mandate staff to select one of the external maintenance companies from a dropdown.
 * 2. Mandate staff to input a numerical approximated_cost.
 * 3. Status Trigger: Automatically update vehicle status to UNAVAILABLE upon scheduling.
 * 4. Notification: Automatically dispatch email alert to the assigned company.
 */
export default function ScheduleMaintenanceForm({ vehicles = [], companies = [], onSubmitSchedule }) {
  const [formData, setFormData] = useState({
    vehicleId: '',
    companyId: '',
    serviceDate: new Date().toISOString().split('T')[0],
    approximatedCost: ''
  });

  const [errors, setErrors] = useState({});
  const [statusTriggered, setStatusTriggered] = useState(false);
  const [emailDispatchedTo, setEmailDispatchedTo] = useState('');

  const validate = () => {
    const err = {};
    if (!formData.vehicleId) {
      err.vehicleId = 'Mandatory: Select a fleet vehicle for service.';
    }
    if (!formData.companyId) {
      err.companyId = 'Mandatory: Select an outsourced maintenance company from the dropdown.';
    }
    if (!formData.approximatedCost || isNaN(formData.approximatedCost) || Number(formData.approximatedCost) < 0) {
      err.approximatedCost = 'Mandatory: Input a valid non-negative numerical approximated cost.';
    }
    setErrors(err);
    return Object.keys(err).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    const selectedCompany = companies.find(c => String(c.companyId) === String(formData.companyId));
    const targetEmail = selectedCompany ? selectedCompany.email : 'partner@service.com';

    // Status Trigger Simulation: Automatically switches vehicle to UNAVAILABLE
    setStatusTriggered(true);
    setEmailDispatchedTo(targetEmail);

    if (onSubmitSchedule) {
      onSubmitSchedule({
        ...formData,
        newVehicleStatus: 'UNAVAILABLE',
        notifiedEmail: targetEmail
      });
    }
  };

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' }}>
      {/* Automation Trigger Success Banner */}
      {statusTriggered && (
        <div style={{
          background: '#ecfdf5',
          border: '1px solid #a7f3d0',
          borderRadius: '12px',
          padding: '1.25rem',
          marginBottom: '1.5rem',
          boxShadow: '0 4px 6px -1px rgba(16, 185, 129, 0.1)'
        }}>
          <h4 style={{ color: '#065f46', margin: '0 0 0.35rem 0', fontSize: '1.05rem', fontWeight: 800 }}>
            ⚡ Maintenance Schedule Saved & Automations Triggered!
          </h4>
          <ul style={{ color: '#047857', fontSize: '0.85rem', margin: 0, paddingLeft: '1.2rem', lineHeight: '1.5' }}>
            <li>Vehicle global status has been switched to <strong>UNAVAILABLE</strong> (blocking any new customer bookings).</li>
            <li>Automated service allocation notification email sent to <strong>{emailDispatchedTo}</strong>.</li>
          </ul>
        </div>
      )}

      <div style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '2rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
      }}>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.5rem 0' }}>
          Schedule Fleet Maintenance
        </h2>
        <p style={{ color: '#64748b', fontSize: '0.88rem', margin: '0 0 1.5rem 0' }}>
          Allocate an external repair company and record approximated costs.
        </p>

        <form onSubmit={handleSubmit}>
          {/* 1. Vehicle Selection */}
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
              Fleet Vehicle <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <select
              value={formData.vehicleId}
              onChange={e => setFormData({ ...formData, vehicleId: e.target.value })}
              style={{ width: '100%', padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.88rem' }}
            >
              <option value="">-- Select Vehicle from Park --</option>
              {vehicles.map(v => (
                <option key={v.vehicleId || v.regNo} value={v.vehicleId || v.regNo}>
                  {v.model} ({v.regNo}) &bull; Current Status: {v.status}
                </option>
              ))}
            </select>
            {errors.vehicleId && <span style={{ color: '#dc2626', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>{errors.vehicleId}</span>}
          </div>

          {/* 2. Mandatory External Maintenance Company Dropdown */}
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
              External Maintenance Company <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <select
              value={formData.companyId}
              onChange={e => setFormData({ ...formData, companyId: e.target.value })}
              style={{ width: '100%', padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.88rem' }}
            >
              <option value="">-- Mandated: Select Outsourced Maintenance Partner --</option>
              {companies.map(c => (
                <option key={c.companyId} value={c.companyId}>
                  {c.companyName} &bull; {c.email} ({c.speciality || 'General Maintenance'})
                </option>
              ))}
            </select>
            <small style={{ color: '#4f46e5', fontSize: '0.78rem', display: 'block', marginTop: '0.25rem' }}>
              📧 Automatic dispatch email will be sent to this partner's registered address.
            </small>
            {errors.companyId && <span style={{ color: '#dc2626', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>{errors.companyId}</span>}
          </div>

          {/* 3. Service Date & Mandatory Numerical Approximated Cost */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                Service Date <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <input
                type="date"
                value={formData.serviceDate}
                onChange={e => setFormData({ ...formData, serviceDate: e.target.value })}
                style={{ width: '100%', padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.88rem' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                Approximated Cost ($) <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={formData.approximatedCost}
                onChange={e => setFormData({ ...formData, approximatedCost: e.target.value })}
                placeholder="e.g. 18500.00"
                style={{ width: '100%', padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.88rem' }}
              />
              {errors.approximatedCost && <span style={{ color: '#dc2626', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>{errors.approximatedCost}</span>}
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            style={{
              width: '100%',
              background: '#4f46e5',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              padding: '0.85rem',
              fontWeight: 700,
              fontSize: '1rem',
              cursor: 'pointer',
              boxShadow: '0 4px 6px -1px rgba(79, 70, 229, 0.25)'
            }}
          >
            Confirm Schedule & Switch to UNAVAILABLE
          </button>
        </form>
      </div>
    </div>
  );
}
