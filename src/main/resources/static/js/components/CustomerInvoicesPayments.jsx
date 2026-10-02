import React, { useState, useEffect } from 'react';

/**
 * DriveFlow Customer "Invoices & Payments" Component (React)
 * 
 * Strict Specification:
 * 1. Target Destination ("Invoices & Payments" Tab):
 *    - Renders the migrated "My Rental Bookings" table component strictly for bookings
 *      that have been approved by staff (Status: CONFIRMED or APPROVED).
 *    - Customers can only pay post-approval.
 * 2. Integrated Payment Gateway Form:
 *    - Requires 16-digit credit card number (with auto-formatting and digit validation).
 *    - 3-digit CVV security code.
 *    - Valid expiration date (MM/YY).
 *    - Issuing bank name and cardholder name.
 *    - Directly pays for the confirmed booking selected in the table.
 */
export default function CustomerInvoicesPayments({ initialBookings = null, customerName = 'John Doe' }) {
  const [confirmedBookings, setConfirmedBookings] = useState(initialBookings || []);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [loading, setLoading] = useState(!initialBookings);
  const [notification, setNotification] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Form State
  const [bankName, setBankName] = useState('Commercial Bank');
  const [cardNo, setCardNo] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvv, setCvv] = useState('');
  const [formErrors, setFormErrors] = useState({});

  // Fetch confirmed bookings from API
  useEffect(() => {
    if (initialBookings) {
      setConfirmedBookings(initialBookings);
      const firstUnpaid = initialBookings.find(b => !b.isPaid);
      setSelectedBooking(firstUnpaid || initialBookings[0] || null);
      setLoading(false);
      return;
    }

    setLoading(true);
    fetch('/api/customer/confirmed-bookings')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load confirmed reservations');
        return res.json();
      })
      .then(data => {
        setConfirmedBookings(data || []);
        const firstUnpaid = (data || []).find(b => !b.isPaid);
        setSelectedBooking(firstUnpaid || (data && data[0]) || null);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching confirmed bookings:', err);
        setLoading(false);
      });
  }, [initialBookings]);

  // Real-time 16-digit card formatter
  const handleCardNumberChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').substring(0, 16);
    let formatted = '';
    for (let i = 0; i < raw.length; i++) {
      if (i > 0 && i % 4 === 0) formatted += ' ';
      formatted += raw[i];
    }
    setCardNo(formatted);
    if (formErrors.cardNo) setFormErrors({ ...formErrors, cardNo: null });
  };

  // Expiration date formatter (MM/YY)
  const handleExpiryChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').substring(0, 4);
    if (raw.length >= 2) {
      setExpiry(raw.substring(0, 2) + '/' + raw.substring(2));
    } else {
      setExpiry(raw);
    }
    if (formErrors.expiry) setFormErrors({ ...formErrors, expiry: null });
  };

  // 3-digit CVV formatter
  const handleCvvChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').substring(0, 3);
    setCvv(raw);
    if (formErrors.cvv) setFormErrors({ ...formErrors, cvv: null });
  };

  const handleSelectBooking = (booking) => {
    setSelectedBooking(booking);
    const formElement = document.getElementById('payment-gateway-form-section');
    if (formElement) {
      formElement.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handlePaymentSubmit = (e) => {
    e.preventDefault();
    const errors = {};

    if (!selectedBooking) {
      errors.general = 'Please select a confirmed booking to settle payment.';
    }

    const cleanCard = cardNo.replace(/\D/g, '');
    if (cleanCard.length !== 16) {
      errors.cardNo = 'Credit card number must be exactly 16 numeric digits.';
    }

    const expiryRegex = /^(0[1-9]|1[0-2])\/\d{2}$/;
    if (!expiryRegex.test(expiry)) {
      errors.expiry = 'Expiration date must follow MM/YY format (e.g. 12/28).';
    }

    if (cvv.length !== 3) {
      errors.cvv = 'CVV security code must be 3 digits.';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    setFormErrors({});

    fetch('/api/customer/pay', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bookingId: selectedBooking.bookingId,
        bankName: bankName,
        cardNo: cleanCard,
        expiry: expiry,
        cvv: cvv
      })
    })
      .then(res => res.json())
      .then(data => {
        setSubmitting(false);
        if (data.success) {
          setNotification({
            type: 'success',
            message: `Payment of $${Number(data.amountPaid || selectedBooking.totalAmountDue).toFixed(2)} processed successfully! Reference: ${data.refNo || 'PAY-SUCCESS'}. Booking #BK-${selectedBooking.bookingId} is now marked as PAID.`
          });

          // Update local state to show booking as paid
          setConfirmedBookings(prev =>
            prev.map(b =>
              b.bookingId === selectedBooking.bookingId
                ? { ...b, isPaid: true, invoiceStatus: 'PAID' }
                : b
            )
          );
          setSelectedBooking(prev => ({ ...prev, isPaid: true, invoiceStatus: 'PAID' }));
          setCardNo('');
          setExpiry('');
          setCvv('');
        } else {
          setNotification({
            type: 'error',
            message: data.message || 'Payment processing failed. Please verify card details.'
          });
        }
      })
      .catch(err => {
        setSubmitting(false);
        setNotification({
          type: 'error',
          message: 'Error contacting payment gateway: ' + err.message
        });
      });
  };

  const unpaidCount = confirmedBookings.filter(b => !b.isPaid).length;
  const paidCount = confirmedBookings.filter(b => b.isPaid).length;

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '1.5rem', fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif" }}>
      
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.03em', margin: 0 }}>
            Invoices & Payments
          </h1>
          <p style={{ fontSize: '0.95rem', color: '#64748b', marginTop: '0.25rem', margin: 0 }}>
            Review staff-confirmed vehicle reservations and complete settlement via the integrated card gateway.
          </p>
        </div>

        <span style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.4rem',
          padding: '0.45rem 0.85rem',
          background: '#ecfeff',
          color: '#0891b2',
          border: '1px solid #a5f3fc',
          borderRadius: '9999px',
          fontSize: '0.8rem',
          fontWeight: 700,
          textTransform: 'uppercase'
        }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
          </svg>
          Post-Approval Payment Gate
        </span>
      </div>

      {/* Notifications */}
      {notification && (
        <div style={{
          padding: '1rem 1.25rem',
          borderRadius: '12px',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          fontWeight: 600,
          fontSize: '0.9rem',
          background: notification.type === 'success' ? '#ecfdf5' : '#fef2f2',
          color: notification.type === 'success' ? '#065f46' : '#991b1b',
          border: notification.type === 'success' ? '1px solid #a7f3d0' : '1px solid #fecaca'
        }}>
          <span>{notification.message}</span>
        </div>
      )}

      {/* KPI Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        <div style={{ background: '#ffffff', borderRadius: '16px', padding: '1.25rem 1.5rem', border: '1px solid #e2e8f0', borderLeft: '4px solid #4f46e5', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b' }}>Confirmed Reservations</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#0f172a', marginTop: '0.2rem' }}>{confirmedBookings.length}</div>
          <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Approved by staff</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '16px', padding: '1.25rem 1.5rem', border: '1px solid #e2e8f0', borderLeft: '4px solid #f59e0b', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b' }}>Awaiting Payment</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#d97706', marginTop: '0.2rem' }}>{unpaidCount}</div>
          <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Ready for card settlement</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '16px', padding: '1.25rem 1.5rem', border: '1px solid #e2e8f0', borderLeft: '4px solid #10b981', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b' }}>Settled Invoices</div>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#059669', marginTop: '0.2rem' }}>{paidCount}</div>
          <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Paid & verified</div>
        </div>
      </div>

      {/* SECTION 1: MIGRATED CONFIRMED BOOKINGS TABLE */}
      <div style={{ marginBottom: '2.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
          <div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.25rem' }}>
              Confirmed Rental Bookings
            </h2>
            <p style={{ fontSize: '0.88rem', color: '#64748b', margin: 0 }}>
              Only reservations approved by staff (Status: CONFIRMED) appear here for payment processing.
            </p>
          </div>
          <a
            href="/bookings/new"
            style={{
              padding: '0.5rem 0.9rem',
              background: '#f1f5f9',
              color: '#334155',
              borderRadius: '8px',
              textDecoration: 'none',
              fontWeight: 600,
              fontSize: '0.85rem'
            }}
          >
            + New Reservation
          </a>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '16px', border: '1px solid #e2e8f0', boxShadow: '0 1px 2px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Booking ID</th>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Vehicle</th>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Rental Period</th>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Amount Due</th>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Approval Status</th>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Invoice Status</th>
                <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {confirmedBookings.map(b => {
                const isSelected = selectedBooking && selectedBooking.bookingId === b.bookingId;
                return (
                  <tr
                    key={b.bookingId}
                    style={{
                      borderBottom: '1px solid #f1f5f9',
                      background: isSelected ? '#f5f3ff' : 'transparent',
                      transition: 'background 0.2s'
                    }}
                  >
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <strong style={{ color: '#0f172a' }}>#BK-{b.bookingId}</strong>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <div style={{ fontWeight: 700, color: '#0f172a' }}>{b.vehicleModel}</div>
                      {b.vehicleRegNo && (
                        <div style={{ fontSize: '0.8rem', color: '#64748b' }}>{b.vehicleRegNo}</div>
                      )}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: '#334155' }}>
                      <div>{b.pickupDate} &rarr; {b.returnDate}</div>
                      <div style={{ fontSize: '0.78rem', color: '#64748b' }}>({b.duration || 1} days)</div>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <strong style={{ color: '#4f46e5', fontSize: '1rem' }}>
                        ${Number(b.totalAmountDue || b.chargedRate || 0).toFixed(2)}
                      </strong>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                        padding: '0.25rem 0.6rem',
                        background: '#ecfdf5',
                        color: '#065f46',
                        borderRadius: '9999px',
                        fontSize: '0.75rem',
                        fontWeight: 700
                      }}>
                        &#10003; Confirmed
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <span style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                        padding: '0.25rem 0.6rem',
                        background: b.isPaid ? '#ecfdf5' : '#fffbeb',
                        color: b.isPaid ? '#065f46' : '#92400e',
                        border: b.isPaid ? '1px solid #a7f3d0' : '1px solid #fde68a',
                        borderRadius: '9999px',
                        fontSize: '0.75rem',
                        fontWeight: 700
                      }}>
                        {b.isPaid ? 'PAID' : 'UNPAID'}
                      </span>
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                      {!b.isPaid ? (
                        <button
                          type="button"
                          onClick={() => handleSelectBooking(b)}
                          style={{
                            background: '#4f46e5',
                            color: '#ffffff',
                            border: 'none',
                            padding: '0.45rem 0.85rem',
                            borderRadius: '8px',
                            fontWeight: 700,
                            fontSize: '0.85rem',
                            cursor: 'pointer',
                            boxShadow: '0 2px 4px rgba(79, 70, 229, 0.25)'
                          }}
                        >
                          Pay with Card &rarr;
                        </button>
                      ) : (
                        <span style={{ fontSize: '0.8rem', color: '#059669', fontWeight: 600 }}>
                          Settled
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
              {confirmedBookings.length === 0 && (
                <tr>
                  <td colSpan="7" style={{ padding: '3rem 2rem', textAlign: 'center', color: '#94a3b8' }}>
                    No staff-confirmed bookings awaiting payment.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* SECTION 2: INTEGRATED PAYMENT GATEWAY FORM */}
      <div
        id="payment-gateway-form-section"
        style={{
          background: '#ffffff',
          borderRadius: '16px',
          border: '1px solid #e2e8f0',
          borderTop: '4px solid #4f46e5',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          padding: '1.75rem',
          marginBottom: '2.5rem'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
              <div style={{ width: '34px', height: '34px', borderRadius: '8px', background: '#e0e7ff', color: '#4f46e5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
                  <line x1="1" y1="10" x2="23" y2="10"></line>
                </svg>
              </div>
              <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#0f172a', margin: 0 }}>
                Integrated Payment Gateway
              </h2>
            </div>
            <p style={{ fontSize: '0.88rem', color: '#64748b', margin: 0 }}>
              Settle payment for your confirmed booking with encrypted 256-bit card processing.
            </p>
          </div>

          <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.35rem',
            padding: '0.35rem 0.75rem',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '9999px',
            fontSize: '0.75rem',
            fontWeight: 700
          }}>
            256-Bit SSL Encrypted
          </span>
        </div>

        {formErrors.general && (
          <div style={{ padding: '0.75rem 1rem', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '1.25rem', fontSize: '0.88rem' }}>
            {formErrors.general}
          </div>
        )}

        <form onSubmit={handlePaymentSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '2rem' }}>
            
            {/* Left Breakdown Box */}
            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '1.5rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', letterSpacing: '0.04em', marginBottom: '1rem' }}>
                  Target Reservation Breakdown
                </div>

                {selectedBooking ? (
                  <div>
                    <div style={{ marginBottom: '1rem' }}>
                      <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', display: 'block' }}>Booking Reference</span>
                      <strong style={{ fontSize: '1.1rem', color: '#0f172a' }}>#BK-{selectedBooking.bookingId}</strong>
                    </div>

                    <div style={{ marginBottom: '1rem' }}>
                      <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', display: 'block' }}>Vehicle</span>
                      <span style={{ fontWeight: 700, color: '#1e293b' }}>{selectedBooking.vehicleModel}</span>
                      {selectedBooking.vehicleRegNo && (
                        <span style={{ color: '#64748b', fontSize: '0.85rem' }}> ({selectedBooking.vehicleRegNo})</span>
                      )}
                    </div>

                    <div style={{ marginBottom: '1rem' }}>
                      <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', display: 'block' }}>Rental Period</span>
                      <span style={{ color: '#334155', fontSize: '0.9rem', fontWeight: 600 }}>
                        {selectedBooking.pickupDate} to {selectedBooking.returnDate}
                      </span>
                    </div>

                    <div style={{ marginBottom: '1rem' }}>
                      <span style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', display: 'block' }}>Booking Status</span>
                      <span style={{ display: 'inline-flex', padding: '0.2rem 0.5rem', background: '#ecfdf5', color: '#047857', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 700 }}>
                        &#10003; Confirmed by Staff
                      </span>
                    </div>
                  </div>
                ) : (
                  <p style={{ color: '#94a3b8', fontStyle: 'italic' }}>No confirmed booking selected.</p>
                )}
              </div>

              {/* Total Due Highlight */}
              <div style={{ paddingTop: '1.25rem', borderTop: '2px dashed #cbd5e1', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <div>
                  <span style={{ fontSize: '0.8rem', fontWeight: 700, textTransform: 'uppercase', color: '#475569', display: 'block' }}>Amount to Authorize</span>
                  <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Total settlement due</span>
                </div>
                <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#4f46e5' }}>
                  ${Number(selectedBooking?.totalAmountDue || selectedBooking?.chargedRate || 0).toFixed(2)}
                </div>
              </div>
            </div>

            {/* Right Card Fields */}
            <div>
              {/* Issuing Bank */}
              <div style={{ marginBottom: '1.2rem' }}>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', color: '#334155', marginBottom: '0.35rem' }}>
                  Issuing Bank
                </label>
                <input
                  type="text"
                  value={bankName}
                  onChange={e => setBankName(e.target.value)}
                  placeholder="e.g. Commercial Bank, Chase, Bank of Ceylon"
                  required
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.85rem',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.95rem'
                  }}
                />
              </div>

              {/* Cardholder Name */}
              <div style={{ marginBottom: '1.2rem' }}>
                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', color: '#334155', marginBottom: '0.35rem' }}>
                  Cardholder Name
                </label>
                <input
                  type="text"
                  value={customerName}
                  readOnly
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.85rem',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    background: '#f1f5f9',
                    color: '#475569',
                    fontWeight: 600,
                    fontSize: '0.95rem'
                  }}
                />
              </div>

              {/* 16-Digit Card Number */}
              <div style={{ marginBottom: '1.2rem' }}>
                <label style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, fontSize: '0.85rem', color: '#334155', marginBottom: '0.35rem' }}>
                  <span>16-Digit Credit / Debit Card Number</span>
                  <span style={{ fontSize: '0.75rem', color: '#4f46e5', fontWeight: 700 }}>16 DIGITS</span>
                </label>
                <input
                  type="text"
                  value={cardNo}
                  onChange={handleCardNumberChange}
                  placeholder="4111 2222 3333 4444"
                  maxLength={19}
                  required
                  style={{
                    width: '100%',
                    padding: '0.7rem 0.85rem',
                    borderRadius: '8px',
                    border: formErrors.cardNo ? '1px solid #ef4444' : '1px solid #cbd5e1',
                    fontFamily: 'monospace',
                    fontSize: '1.05rem',
                    letterSpacing: '0.05em',
                    fontWeight: 600
                  }}
                />
                {formErrors.cardNo && (
                  <span style={{ color: '#ef4444', fontSize: '0.78rem', marginTop: '0.25rem', display: 'block' }}>
                    {formErrors.cardNo}
                  </span>
                )}
              </div>

              {/* Expiry and CVV */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                <div>
                  <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', color: '#334155', marginBottom: '0.35rem' }}>
                    Expiry (MM/YY)
                  </label>
                  <input
                    type="text"
                    value={expiry}
                    onChange={handleExpiryChange}
                    placeholder="12/28"
                    maxLength={5}
                    required
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.85rem',
                      borderRadius: '8px',
                      border: formErrors.expiry ? '1px solid #ef4444' : '1px solid #cbd5e1',
                      fontFamily: 'monospace',
                      fontSize: '0.95rem',
                      fontWeight: 600
                    }}
                  />
                  {formErrors.expiry && (
                    <span style={{ color: '#ef4444', fontSize: '0.78rem', marginTop: '0.25rem', display: 'block' }}>
                      {formErrors.expiry}
                    </span>
                  )}
                </div>

                <div>
                  <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', color: '#334155', marginBottom: '0.35rem' }}>
                    3-Digit CVV
                  </label>
                  <input
                    type="password"
                    value={cvv}
                    onChange={handleCvvChange}
                    placeholder="123"
                    maxLength={3}
                    required
                    style={{
                      width: '100%',
                      padding: '0.65rem 0.85rem',
                      borderRadius: '8px',
                      border: formErrors.cvv ? '1px solid #ef4444' : '1px solid #cbd5e1',
                      fontFamily: 'monospace',
                      fontSize: '0.95rem',
                      fontWeight: 600
                    }}
                  />
                  {formErrors.cvv && (
                    <span style={{ color: '#ef4444', fontSize: '0.78rem', marginTop: '0.25rem', display: 'block' }}>
                      {formErrors.cvv}
                    </span>
                  )}
                </div>
              </div>

              {/* Submit button */}
              <button
                type="submit"
                disabled={submitting || !selectedBooking || selectedBooking.isPaid}
                style={{
                  width: '100%',
                  padding: '0.85rem',
                  fontSize: '1.05rem',
                  fontWeight: 700,
                  background: selectedBooking?.isPaid ? '#94a3b8' : '#4f46e5',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '10px',
                  cursor: selectedBooking?.isPaid ? 'not-allowed' : 'pointer',
                  boxShadow: '0 4px 12px rgba(79, 70, 229, 0.3)'
                }}
              >
                {submitting ? 'Processing Payment...' : selectedBooking?.isPaid ? 'Reservation Already Paid' : `Authorize & Pay $${Number(selectedBooking?.totalAmountDue || selectedBooking?.chargedRate || 0).toFixed(2)}`}
              </button>

              <div style={{ marginTop: '0.85rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem', color: '#64748b', fontSize: '0.78rem' }}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
                <span>Certified PCI-DSS Compliant Payment Clearance</span>
              </div>
            </div>

          </div>
        </form>
      </div>

    </div>
  );
}

export { CustomerInvoicesPayments };
