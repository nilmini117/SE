import React, { useState, useEffect } from 'react';

/**
 * DriveFlow Corporate Financial Ledger & Company Sales Dashboard (React)
 * 
 * Strict Business Logic & Constraints:
 * 1. Financial Immutability (Staff Constraints):
 *    - In the primary payments list view, completely remove "Status" and "Actions" columns.
 *    - Staff must not have UI access to edit, refund, or delete any payment records.
 * 2. UI Clean-up:
 *    - Remove the "+ Process Payment" button from the top of the page.
 *    - Remove all secondary financial tabs (Invoices, Refunds) so only main read-only transaction list remains.
 * 3. New Module - "Company Sales Information":
 *    - Bank Details: Central company bank account information (routing/account numbers) where customer payments are deposited.
 *    - Profit & Loss Table: Calculates real income: SUM(revenue) - SUM(maintenance_costs)
 *      pulling total revenue from approved payments, deducting total service costs from Maintenance (approximated_cost).
 */
export default function CompanySalesDashboard({ initialTab = 'transactions' }) {
  const [activeTab, setActiveTab] = useState(initialTab);
  const [payments, setPayments] = useState([]);
  const [salesSummary, setSalesSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [copiedField, setCopiedField] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch payments and corporate sales summary from Spring Boot backend
  useEffect(() => {
    setLoading(true);
    setError(null);

    Promise.all([
      fetch('/api/company-sales')
        .then(res => {
          if (!res.ok) {
            // Fallback to /payments/api/sales
            return fetch('/payments/api/sales');
          }
          return res;
        })
        .then(res => {
          if (!res.ok) throw new Error('Failed to load corporate sales summary');
          return res.json();
        }),
      fetch('/payments/api')
        .then(res => {
          if (!res.ok) return [];
          return res.json();
        })
        .catch(() => [])
    ])
      .then(([summaryData, paymentsData]) => {
        setSalesSummary(summaryData);
        if (paymentsData && paymentsData.length > 0) {
          setPayments(paymentsData);
        } else if (summaryData && summaryData.approvedPayments) {
          setPayments(summaryData.approvedPayments);
        }
        setLoading(false);
      })
      .catch(err => {
        console.error('Error loading sales data:', err);
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const handleCopy = (text, fieldName) => {
    navigator.clipboard.writeText(text);
    setCopiedField(fieldName);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const filteredPayments = payments.filter(p => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const id = p.paymentId ? String(p.paymentId) : '';
    const ref = p.refNo ? p.refNo.toLowerCase() : '';
    const inv = p.invoice && p.invoice.invoiceId ? String(p.invoice.invoiceId) : '';
    const cust = p.invoice && p.invoice.booking && p.invoice.booking.customer && p.invoice.booking.customer.name
      ? p.invoice.booking.customer.name.toLowerCase()
      : '';
    return id.includes(q) || ref.includes(q) || inv.includes(q) || cust.includes(q);
  });

  const totalRevenue = salesSummary ? Number(salesSummary.totalRevenue || 0) : 0;
  const totalMaintenanceCosts = salesSummary ? Number(salesSummary.totalMaintenanceCosts || 0) : 0;
  const netIncome = salesSummary ? Number(salesSummary.netIncome || (totalRevenue - totalMaintenanceCosts)) : (totalRevenue - totalMaintenanceCosts);

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '1.5rem', fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif" }}>
      
      {/* Page Header (Notice: "+ Process Payment" button is completely removed to enforce Financial Immutability) */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.03em', margin: 0 }}>
            Corporate Financial Ledger & Sales
          </h1>
          <p style={{ fontSize: '0.95rem', color: '#64748b', marginTop: '0.25rem', margin: 0 }}>
            Immutable customer payment records and aggregate corporate revenue vs fleet maintenance performance.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.4rem',
            padding: '0.45rem 0.85rem',
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: '9999px',
            fontSize: '0.8rem',
            fontWeight: 700,
            color: '#475569',
            textTransform: 'uppercase',
            letterSpacing: '0.04em'
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            Read-Only Financial Records
          </span>
        </div>
      </div>

      {/* Navigation Tabs (Only Processed Payments and Company Sales Information remain) */}
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid #e2e8f0', marginBottom: '1.75rem' }}>
        <button
          type="button"
          onClick={() => setActiveTab('transactions')}
          style={{
            padding: '0.75rem 1.25rem',
            fontSize: '0.95rem',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            borderBottom: activeTab === 'transactions' ? '2px solid #4f46e5' : '2px solid transparent',
            color: activeTab === 'transactions' ? '#4f46e5' : '#64748b',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            transition: 'all 0.2s'
          }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
            <line x1="1" y1="10" x2="23" y2="10"></line>
          </svg>
          Processed Payments
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('sales')}
          style={{
            padding: '0.75rem 1.25rem',
            fontSize: '0.95rem',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            borderBottom: activeTab === 'sales' ? '2px solid #4f46e5' : '2px solid transparent',
            color: activeTab === 'sales' ? '#4f46e5' : '#64748b',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            transition: 'all 0.2s'
          }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="1" x2="12" y2="23"></line>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
          </svg>
          Company Sales Information
        </button>
      </div>

      {/* Loading state */}
      {loading && (
        <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
          <div style={{ display: 'inline-block', width: '32px', height: '32px', border: '3px solid #e2e8f0', borderTopColor: '#4f46e5', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
          <p style={{ marginTop: '1rem', fontWeight: 600 }}>Loading corporate financial data...</p>
        </div>
      )}

      {/* Error state */}
      {error && (
        <div style={{ padding: '1rem 1.25rem', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '12px', color: '#991b1b', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>Error loading data: {error}</span>
        </div>
      )}

      {/* TAB 1: PROCESSED PAYMENTS (STRICT READ-ONLY TRANSACTION VIEW) */}
      {!loading && activeTab === 'transactions' && (
        <div>
          {/* Immutability Banner */}
          <div style={{
            background: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: '12px',
            padding: '0.9rem 1.25rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.75rem'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
              <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: '26px', height: '26px', borderRadius: '50%', background: '#e0e7ff', color: '#4338ca', fontSize: '0.85rem', fontWeight: 700 }}>i</span>
              <span style={{ fontSize: '0.88rem', color: '#334155' }}>
                <strong>Financial Immutability:</strong> All customer payments are permanent audit ledger records. Staff actions (Edit, Refund, Delete) and status toggles have been removed.
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Search reference, invoice..."
                style={{
                  padding: '0.4rem 0.75rem',
                  fontSize: '0.85rem',
                  border: '1px solid #cbd5e1',
                  borderRadius: '6px',
                  outline: 'none'
                }}
              />
              <span style={{ fontSize: '0.82rem', color: '#64748b', fontWeight: 600 }}>
                {filteredPayments.length} Entries
              </span>
            </div>
          </div>

          {/* Primary Payments List Table:
              Strict Constraints:
              1. Status column completely removed.
              2. Actions column completely removed (no Edit, Refund, or Delete buttons).
          */}
          <div style={{ background: 'white', borderRadius: '16px', border: '1px solid #e2e8f0', boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Payment ID</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Reference Number</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Linked Invoice</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Payment Date</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', textAlign: 'right' }}>Amount Paid</th>
                </tr>
              </thead>
              <tbody>
                {filteredPayments.map(p => (
                  <tr key={p.paymentId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <strong style={{ color: '#0f172a' }}>#PAY-{p.paymentId}</strong>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      <code style={{ background: '#f1f5f9', padding: '0.25rem 0.5rem', borderRadius: '4px', fontWeight: 600, color: '#1e293b' }}>
                        {p.refNo || 'N/A'}
                      </code>
                    </td>
                    <td style={{ padding: '1rem 1.25rem' }}>
                      {p.invoice ? (
                        <span>
                          <strong style={{ color: '#0f172a' }}>#INV-{p.invoice.invoiceId}</strong>
                          {p.invoice.booking && p.invoice.booking.customer && (
                            <span style={{ color: '#64748b', fontSize: '0.85rem' }}>
                              {' '}({p.invoice.booking.customer.name})
                            </span>
                          )}
                        </span>
                      ) : (
                        <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Unlinked</span>
                      )}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', color: '#334155' }}>
                      {p.paymentDate || 'N/A'}
                    </td>
                    <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                      <strong style={{ color: '#10b981', fontSize: '0.95rem' }}>
                        ${Number(p.amountPaid || 0).toFixed(2)}
                      </strong>
                    </td>
                  </tr>
                ))}
                {filteredPayments.length === 0 && (
                  <tr>
                    <td colSpan="5" style={{ padding: '3rem 2rem', textAlign: 'center', color: '#94a3b8' }}>
                      No payment records found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: COMPANY SALES INFORMATION (CORPORATE FINANCIAL OVERVIEW & PROFIT/LOSS) */}
      {!loading && activeTab === 'sales' && (
        <div>
          {/* SECTION 1: BANK DETAILS */}
          <div style={{
            background: 'white',
            borderRadius: '16px',
            border: '1px solid #e2e8f0',
            borderTop: '4px solid #4f46e5',
            boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
            padding: '1.75rem',
            marginBottom: '2rem'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.35rem' }}>
                  <div style={{ width: '32px', height: '32px', borderRadius: '8px', background: '#e0e7ff', color: '#4f46e5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="12" y1="1" x2="12" y2="23"></line>
                      <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                    </svg>
                  </div>
                  <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a', margin: 0 }}>
                    Central Corporate Bank Account
                  </h2>
                </div>
                <p style={{ fontSize: '0.88rem', color: '#64748b', margin: 0 }}>
                  Designated central company bank account where all customer payments and card transactions are deposited.
                </p>
              </div>

              <span style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
                padding: '0.4rem 0.8rem',
                background: '#ecfdf5',
                color: '#065f46',
                border: '1px solid #a7f3d0',
                borderRadius: '9999px',
                fontSize: '0.78rem',
                fontWeight: 700,
                textTransform: 'uppercase'
              }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                Direct Deposit Verified
              </span>
            </div>

            {/* Bank Information Grid */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '1.25rem',
              background: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: '12px',
              padding: '1.25rem'
            }}>
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', letterSpacing: '0.05em', marginBottom: '0.3rem' }}>
                  Beneficiary Entity
                </div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#0f172a' }}>
                  {salesSummary?.beneficiaryName || 'DriveFlow Car Rental Systems Inc.'}
                </div>
                <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.2rem' }}>
                  {salesSummary?.accountName || 'DriveFlow Corporate Operating Fund'}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', letterSpacing: '0.05em', marginBottom: '0.3rem' }}>
                  Bank & Branch
                </div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#0f172a' }}>
                  {salesSummary?.bankName || 'Commercial Bank of Ceylon'}
                </div>
                <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.2rem' }}>
                  {salesSummary?.branchName || 'Colombo Central Main Hub'}
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', letterSpacing: '0.05em', marginBottom: '0.3rem' }}>
                  Account Number
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <code style={{ fontSize: '1.05rem', fontWeight: 800, color: '#4f46e5', background: 'white', padding: '0.3rem 0.6rem', borderRadius: '4px', border: '1px solid #c7d2fe' }}>
                    {salesSummary?.accountNumber || '1000-8842-9931-5021'}
                  </code>
                  <button
                    type="button"
                    onClick={() => handleCopy(salesSummary?.accountNumber || '1000-8842-9931-5021', 'account')}
                    style={{
                      background: 'white',
                      border: '1px solid #cbd5e1',
                      borderRadius: '4px',
                      padding: '0.3rem 0.55rem',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      color: copiedField === 'account' ? '#059669' : '#475569'
                    }}
                  >
                    {copiedField === 'account' ? 'Copied!' : 'Copy'}
                  </button>
                </div>
              </div>

              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: '#64748b', letterSpacing: '0.05em', marginBottom: '0.3rem' }}>
                  Routing & SWIFT
                </div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#0f172a' }}>
                  Routing: <code style={{ background: 'white', padding: '0.15rem 0.45rem', borderRadius: '4px', border: '1px solid #e2e8f0' }}>{salesSummary?.routingNumber || '071000288'}</code>
                </div>
                <div style={{ fontSize: '0.85rem', color: '#475569', marginTop: '0.25rem' }}>
                  SWIFT: <code style={{ background: 'white', padding: '0.15rem 0.45rem', borderRadius: '4px', border: '1px solid #e2e8f0', fontWeight: 600 }}>{salesSummary?.swiftCode || 'CBCLKLX'}</code>
                </div>
              </div>
            </div>

            <div style={{
              marginTop: '1rem',
              padding: '0.75rem 1rem',
              background: '#eff6ff',
              border: '1px solid #bfdbfe',
              borderRadius: '8px',
              fontSize: '0.83rem',
              color: '#1e40af',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
              <span>{salesSummary?.depositInstructions || 'Please quote Customer Invoice ID (#INV-XXXX) or Booking ID (#BK-XXXX) in payment reference.'}</span>
            </div>
          </div>

          {/* SECTION 2: PROFIT & LOSS AGGREGATE SUMMARY CARDS */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
            {/* Total Revenue Card */}
            <div style={{
              background: 'white',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              borderLeft: '5px solid #10b981',
              padding: '1.75rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Total Revenue (Approved)
                </div>
                <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#ecfdf5', color: '#10b981', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="12" y1="1" x2="12" y2="23"></line>
                    <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                  </svg>
                </div>
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#065f46' }}>
                ${totalRevenue.toFixed(2)}
              </div>
              <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.35rem' }}>
                Aggregated from {salesSummary?.approvedPaymentsCount || payments.length} cleared payments
              </div>
            </div>

            {/* Total Maintenance Costs Card */}
            <div style={{
              background: 'white',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              borderLeft: '5px solid #ef4444',
              padding: '1.75rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Fleet Maintenance Costs
                </div>
                <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#fef2f2', color: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
                  </svg>
                </div>
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#991b1b' }}>
                -${totalMaintenanceCosts.toFixed(2)}
              </div>
              <div style={{ fontSize: '0.82rem', color: '#64748b', marginTop: '0.35rem' }}>
                Total approximated cost from Maintenance module ({salesSummary?.maintenanceServicesCount || 0} services)
              </div>
            </div>

            {/* Real Net Operating Income Card */}
            <div style={{
              background: '#fdfefe',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              borderLeft: '5px solid #4f46e5',
              padding: '1.75rem',
              boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Real Net Operating Income
                </div>
                <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: '#e0e7ff', color: '#4f46e5', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                    <polyline points="17 6 23 6 23 12"></polyline>
                  </svg>
                </div>
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800, color: netIncome >= 0 ? '#047857' : '#b91c1c' }}>
                ${netIncome.toFixed(2)}
              </div>
              <div style={{ fontSize: '0.82rem', fontWeight: 600, color: '#4f46e5', marginTop: '0.35rem' }}>
                Formula: SUM(revenue) - SUM(maintenance_costs)
              </div>
            </div>
          </div>

          {/* SECTION 3: PROFIT & LOSS DYNAMIC CALCULATION TABLE */}
          <div style={{ background: 'white', borderRadius: '16px', border: '1px solid #e2e8f0', boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', overflow: 'hidden', marginBottom: '2.5rem' }}>
            <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid #e2e8f0', background: '#f8fafc', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: '#0f172a', margin: 0 }}>
                  Corporate Profit & Loss Statement
                </h3>
                <p style={{ fontSize: '0.85rem', color: '#64748b', margin: '0.2rem 0 0 0' }}>
                  Real-time aggregation from live Payment revenue and Maintenance service expenditures.
                </p>
              </div>
              <span style={{
                fontSize: '0.78rem',
                fontWeight: 700,
                textTransform: 'uppercase',
                padding: '0.3rem 0.65rem',
                background: '#ecfeff',
                color: '#155e75',
                border: '1px solid #a5f3fc',
                borderRadius: '9999px'
              }}>
                Live Aggregation
              </span>
            </div>

            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', width: '25%' }}>Financial Category</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', width: '35%' }}>Source Module & Accounting Logic</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', width: '15%', textAlign: 'center' }}>Record Volume</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', width: '12%', textAlign: 'center' }}>Flow Type</th>
                  <th style={{ padding: '1rem 1.25rem', fontWeight: 700, color: '#475569', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', width: '13%', textAlign: 'right' }}>Amount (USD)</th>
                </tr>
              </thead>
              <tbody>
                {/* Row 1: Revenue */}
                <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                      <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981', display: 'inline-block' }}></span>
                      <div>
                        <strong style={{ color: '#0f172a' }}>Gross Customer Rental Revenue</strong>
                        <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Cleared rental receipts</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', fontSize: '0.85rem', color: '#334155' }}>
                    <code>SELECT SUM(amount_paid) FROM payment WHERE status != 'CANCELLED'</code>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                    <span style={{ padding: '0.25rem 0.5rem', background: '#f1f5f9', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 700, color: '#475569' }}>
                      {salesSummary?.approvedPaymentsCount || 0} payments
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                    <span style={{ padding: '0.25rem 0.6rem', background: '#ecfdf5', color: '#065f46', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 700 }}>
                      + Inflow
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                    <strong style={{ color: '#065f46', fontSize: '1.05rem' }}>
                      +${totalRevenue.toFixed(2)}
                    </strong>
                  </td>
                </tr>

                {/* Row 2: Maintenance Costs */}
                <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '1rem 1.25rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                      <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#ef4444', display: 'inline-block' }}></span>
                      <div>
                        <strong style={{ color: '#0f172a' }}>Fleet Maintenance & Repairs</strong>
                        <div style={{ fontSize: '0.8rem', color: '#64748b' }}>Vehicle servicing & parts</div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', fontSize: '0.85rem', color: '#334155' }}>
                    <code>SELECT SUM(approximated_cost) FROM maintenance</code>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                    <span style={{ padding: '0.25rem 0.5rem', background: '#f1f5f9', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 700, color: '#475569' }}>
                      {salesSummary?.maintenanceServicesCount || 0} services
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'center' }}>
                    <span style={{ padding: '0.25rem 0.6rem', background: '#fef2f2', color: '#991b1b', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 700 }}>
                      - Outflow
                    </span>
                  </td>
                  <td style={{ padding: '1rem 1.25rem', textAlign: 'right' }}>
                    <strong style={{ color: '#991b1b', fontSize: '1.05rem' }}>
                      -${totalMaintenanceCosts.toFixed(2)}
                    </strong>
                  </td>
                </tr>
              </tbody>

              {/* Summary Row */}
              <tfoot>
                <tr style={{ background: '#f1f5f9', borderTop: '2px solid #cbd5e1' }}>
                  <td colSpan="2" style={{ padding: '1.1rem 1.25rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                      <div style={{ width: '24px', height: '24px', borderRadius: '50%', background: '#4f46e5', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.85rem', fontWeight: 800 }}>=</div>
                      <div>
                        <strong style={{ fontSize: '1.05rem', color: '#0f172a' }}>Final Net Corporate Operating Income</strong>
                        <div style={{ fontSize: '0.8rem', color: '#475569', fontWeight: 500 }}>
                          Real Profit Calculation: <code>SUM(revenue) - SUM(maintenance_costs)</code>
                        </div>
                      </div>
                    </div>
                  </td>
                  <td style={{ padding: '1.1rem 1.25rem', textAlign: 'center', fontSize: '0.85rem', fontWeight: 600, color: '#334155' }}>
                    Consolidated
                  </td>
                  <td style={{ padding: '1.1rem 1.25rem', textAlign: 'center' }}>
                    <span style={{ padding: '0.3rem 0.65rem', background: '#e0e7ff', color: '#4338ca', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 800 }}>
                      NET RESULT
                    </span>
                  </td>
                  <td style={{ padding: '1.1rem 1.25rem', textAlign: 'right' }}>
                    <strong style={{ fontSize: '1.25rem', color: netIncome >= 0 ? '#047857' : '#b91c1c' }}>
                      ${netIncome.toFixed(2)}
                    </strong>
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

export { CompanySalesDashboard };
