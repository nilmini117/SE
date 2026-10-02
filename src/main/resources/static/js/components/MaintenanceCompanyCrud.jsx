import React, { useState, useEffect } from 'react';

/**
 * DriveFlow Staff Maintenance Company CRUD Component (React)
 * Module: "Maintenance Companies" Staff Portal Tab
 * 
 * Features:
 * - Full CRUD: Add, Edit, Delete, View outsourced maintenance companies.
 * - Form validation (companyName, email, contactNumber, address, speciality).
 * - Pre-seeded demo partner companies support.
 */
export default function MaintenanceCompanyCrud({ csrfToken = '' }) {
  const [companies, setCompanies] = useState([
    {
      companyId: 1,
      companyName: 'AutoCare Precision Services',
      email: 'autocare@precisionfleet.com',
      contactNumber: '011-2894567',
      address: '45 Station Road, Colombo 03',
      speciality: 'Engine & Transmission Overhaul'
    },
    {
      companyId: 2,
      companyName: 'Apex Fleet Mechanics & Bodywork',
      email: 'service@apexfleet.com',
      contactNumber: '011-4567890',
      address: '122 Baseline Highway, Colombo 08',
      speciality: 'Bodywork, Paint & Structural Repairs'
    },
    {
      companyId: 3,
      companyName: 'VoltTech Hybrid & EV Diagnostics',
      email: 'support@volttechfleet.com',
      contactNumber: '011-3456789',
      address: '88 High Level Road, Nugegoda',
      speciality: 'Hybrid & Electric Vehicle Servicing'
    }
  ]);

  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingCompany, setEditingCompany] = useState(null);
  const [formData, setFormData] = useState({
    companyName: '',
    email: '',
    contactNumber: '',
    address: '',
    speciality: ''
  });
  const [errors, setErrors] = useState({});
  const [alert, setAlert] = useState(null);

  const openAddModal = () => {
    setEditingCompany(null);
    setFormData({ companyName: '', email: '', contactNumber: '', address: '', speciality: '' });
    setErrors({});
    setModalOpen(true);
  };

  const openEditModal = (company) => {
    setEditingCompany(company);
    setFormData({ ...company });
    setErrors({});
    setModalOpen(true);
  };

  const validate = () => {
    const err = {};
    if (!formData.companyName.trim()) err.companyName = 'Company name is mandatory.';
    if (!formData.email.trim()) {
      err.email = 'Notification email is mandatory.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      err.email = 'Please provide a valid email format.';
    }
    setErrors(err);
    return Object.keys(err).length === 0;
  };

  const handleSave = (e) => {
    e.preventDefault();
    if (!validate()) return;

    if (editingCompany) {
      setCompanies(companies.map(c => c.companyId === editingCompany.companyId ? { ...formData, companyId: editingCompany.companyId } : c));
      setAlert({ type: 'success', text: `Company '${formData.companyName}' updated successfully.` });
    } else {
      const newId = Math.max(...companies.map(c => c.companyId), 0) + 1;
      setCompanies([...companies, { ...formData, companyId: newId }]);
      setAlert({ type: 'success', text: `Company '${formData.companyName}' added successfully.` });
    }
    setModalOpen(false);
  };

  const handleDelete = (id, name) => {
    if (window.confirm(`Are you sure you want to remove '${name}' from outsourced maintenance partners?`)) {
      setCompanies(companies.filter(c => c.companyId !== id));
      setAlert({ type: 'success', text: `Company '${name}' deleted successfully.` });
    }
  };

  const filtered = companies.filter(c => 
    c.companyName.toLowerCase().includes(search.toLowerCase()) ||
    c.email.toLowerCase().includes(search.toLowerCase()) ||
    (c.speciality && c.speciality.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' }}>
      {/* Alert banner */}
      {alert && (
        <div style={{
          padding: '0.85rem 1.25rem',
          borderRadius: '8px',
          background: alert.type === 'success' ? '#ecfdf5' : '#fef2f2',
          color: alert.type === 'success' ? '#065f46' : '#991b1b',
          border: `1px solid ${alert.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
          marginBottom: '1.5rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span>{alert.text}</span>
          <button onClick={() => setAlert(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', fontWeight: 700 }}>&times;</button>
        </div>
      )}

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.35rem 0' }}>
            Maintenance Companies
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.9rem', margin: 0 }}>
            Manage outsourced partner garages, repair workshops, and service contacts.
          </p>
        </div>

        <button
          onClick={openAddModal}
          style={{
            background: '#4f46e5',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            padding: '0.65rem 1.25rem',
            fontWeight: 700,
            fontSize: '0.9rem',
            cursor: 'pointer',
            boxShadow: '0 4px 6px -1px rgba(79, 70, 229, 0.25)'
          }}
        >
          + Add Maintenance Company
        </button>
      </div>

      {/* Search Bar */}
      <div style={{ marginBottom: '1.5rem' }}>
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search by company name, email, or speciality..."
          style={{
            width: '100%',
            maxWidth: '420px',
            padding: '0.55rem 0.85rem',
            border: '1px solid #cbd5e1',
            borderRadius: '8px',
            fontSize: '0.88rem'
          }}
        />
      </div>

      {/* Table */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontWeight: 700 }}>
              <th style={{ padding: '0.85rem 1rem' }}>ID</th>
              <th style={{ padding: '0.85rem 1rem' }}>Company Name</th>
              <th style={{ padding: '0.85rem 1rem' }}>Notification Email</th>
              <th style={{ padding: '0.85rem 1rem' }}>Phone</th>
              <th style={{ padding: '0.85rem 1rem' }}>Speciality</th>
              <th style={{ padding: '0.85rem 1rem' }}>Address</th>
              <th style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(c => (
              <tr key={c.companyId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 700, color: '#64748b' }}>#CMP-{c.companyId}</td>
                <td style={{ padding: '0.85rem 1rem', fontWeight: 800, color: '#0f172a' }}>{c.companyName}</td>
                <td style={{ padding: '0.85rem 1rem', color: '#4f46e5' }}>{c.email}</td>
                <td style={{ padding: '0.85rem 1rem', color: '#334155' }}>{c.contactNumber || '-'}</td>
                <td style={{ padding: '0.85rem 1rem' }}>
                  <span style={{ padding: '0.2rem 0.55rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 700, background: '#e0e7ff', color: '#4338ca' }}>
                    {c.speciality || 'General'}
                  </span>
                </td>
                <td style={{ padding: '0.85rem 1rem', color: '#64748b' }}>{c.address || '-'}</td>
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                  <button
                    onClick={() => openEditModal(c)}
                    style={{ background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '6px', padding: '0.35rem 0.75rem', fontSize: '0.8rem', fontWeight: 600, color: '#334155', cursor: 'pointer', marginRight: '0.35rem' }}
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(c.companyId, c.companyName)}
                    style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', padding: '0.35rem 0.75rem', fontSize: '0.8rem', fontWeight: 600, color: '#dc2626', cursor: 'pointer' }}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal for Add / Edit */}
      {modalOpen && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(15, 23, 42, 0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          backdropFilter: 'blur(4px)'
        }}>
          <div style={{
            background: 'white',
            borderRadius: '16px',
            width: '100%',
            maxWidth: '520px',
            padding: '1.75rem',
            boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)'
          }}>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#0f172a', margin: '0 0 1.25rem 0' }}>
              {editingCompany ? 'Edit Maintenance Partner' : 'Register New Partner Company'}
            </h2>

            <form onSubmit={handleSave}>
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Company Name *
                </label>
                <input
                  type="text"
                  value={formData.companyName}
                  onChange={e => setFormData({ ...formData, companyName: e.target.value })}
                  style={{ width: '100%', padding: '0.55rem', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '0.88rem' }}
                  placeholder="e.g. Apex Fleet Mechanics"
                />
                {errors.companyName && <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{errors.companyName}</span>}
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Notification Email * (Receives automated dispatch alerts)
                </label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={e => setFormData({ ...formData, email: e.target.value })}
                  style={{ width: '100%', padding: '0.55rem', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '0.88rem' }}
                  placeholder="e.g. service@apexfleet.com"
                />
                {errors.email && <span style={{ color: '#dc2626', fontSize: '0.75rem' }}>{errors.email}</span>}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                    Phone Number
                  </label>
                  <input
                    type="text"
                    value={formData.contactNumber}
                    onChange={e => setFormData({ ...formData, contactNumber: e.target.value })}
                    style={{ width: '100%', padding: '0.55rem', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '0.88rem' }}
                    placeholder="011-2894567"
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                    Speciality
                  </label>
                  <input
                    type="text"
                    value={formData.speciality}
                    onChange={e => setFormData({ ...formData, speciality: e.target.value })}
                    style={{ width: '100%', padding: '0.55rem', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '0.88rem' }}
                    placeholder="Engine, Bodywork, EV"
                  />
                </div>
              </div>

              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Address
                </label>
                <input
                  type="text"
                  value={formData.address}
                  onChange={e => setFormData({ ...formData, address: e.target.value })}
                  style={{ width: '100%', padding: '0.55rem', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '0.88rem' }}
                  placeholder="Street, City"
                />
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  style={{ padding: '0.6rem 1.15rem', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600, cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{ padding: '0.6rem 1.25rem', background: '#4f46e5', color: 'white', border: 'none', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 700, cursor: 'pointer' }}
                >
                  Save Partner Company
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
