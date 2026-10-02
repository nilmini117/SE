import React, { useState, useEffect } from 'react';
import { BrandCard } from './FilterByVehicleBrand';

/**
 * DriveFlow Staff Vehicle Management & Fleet Table Component (React)
 * 
 * Features:
 * 1. Table UI & Deletion Action:
 *    - Populates the ACTIONS column with a red "Delete" action button for each vehicle row.
 *    - Prompting a standard confirmation modal: "Are you sure you want to remove this vehicle from the fleet?".
 *    - Dispatches a DELETE request to /api/vehicles/{id} (or by registration number).
 * 2. Reactive Brand Count Synchronization:
 *    - Displays brand cards at the top ("Toyota", "Suzuki", "Honda", "Tesla", "Benz").
 *    - Derives card count strictly via:
 *      vehicles.filter(v => v.brand.toLowerCase() === brand.toLowerCase()).length
 *    - Immediate State Update:
 *      * Upon deletion: removes record from local state so brand count immediately decrements (e.g. from 3 down to 2).
 *      * Upon addition: appends new vehicle to state so brand count immediately increments (e.g. from 3 up to 4).
 *      * No full browser reload required.
 */
export default function StaffVehicleTable({
  initialVehicles = null,
  onVehicleDeleted = null,
  onVehicleAdded = null
}) {
  const BRANDS = [
    { name: 'Toyota', emoji: '🔴', color: '#fee2e2', textColor: '#dc2626' },
    { name: 'Suzuki', emoji: '🔵', color: '#dbeafe', textColor: '#2563eb' },
    { name: 'Honda', emoji: '🟡', color: '#fef3c7', textColor: '#d97706' },
    { name: 'Tesla', emoji: '⚡', color: '#e0e7ff', textColor: '#4338ca' },
    { name: 'Benz', emoji: '⭐', color: '#f1f5f9', textColor: '#0f172a' }
  ];

  const [vehicles, setVehicles] = useState(initialVehicles || []);
  const [loading, setLoading] = useState(!initialVehicles);
  const [selectedBrand, setSelectedBrand] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    vehicle: null,
    isDeleting: false,
    error: null
  });
  const [notification, setNotification] = useState(null);

  // Fetch initial fleet data from backend API
  useEffect(() => {
    if (initialVehicles) {
      setVehicles(initialVehicles);
      setLoading(false);
      return;
    }

    setLoading(true);
    fetch('/api/vehicles')
      .then(res => {
        if (!res.ok) throw new Error('Failed to fetch fleet records');
        return res.json();
      })
      .then(data => {
        setVehicles(data);
        setLoading(false);
      })
      .catch(err => {
        console.warn('Fallback to /vehicles/api/by-brand:', err);
        fetch('/vehicles/api/by-brand?availableOnly=false')
          .then(res => res.ok ? res.json() : [])
          .then(data => {
            setVehicles(data);
            setLoading(false);
          })
          .catch(() => {
            setVehicles([]);
            setLoading(false);
          });
      });
  }, [initialVehicles]);

  // Synchronize when external props change
  useEffect(() => {
    if (initialVehicles) {
      setVehicles(initialVehicles);
    }
  }, [initialVehicles]);

  // Strict Brand Count Derivation Rule:
  // Derive the number strictly via vehicles.filter(v => v.brand.toLowerCase() === brand.toLowerCase()).length
  const getBrandCount = (brandName) => {
    if (!Array.isArray(vehicles)) return 0;
    return vehicles.filter(v => 
      v && v.brand && v.brand.toLowerCase() === brandName.toLowerCase()
    ).length;
  };

  // Open confirmation modal for deletion
  const promptDeleteVehicle = (vehicle) => {
    setDeleteModal({
      isOpen: true,
      vehicle,
      isDeleting: false,
      error: null
    });
  };

  const closeDeleteModal = () => {
    if (!deleteModal.isDeleting) {
      setDeleteModal({
        isOpen: false,
        vehicle: null,
        isDeleting: false,
        error: null
      });
    }
  };

  // Execute DELETE API call & immediate state update
  const confirmDeleteVehicle = async () => {
    const vehicle = deleteModal.vehicle;
    if (!vehicle) return;

    setDeleteModal(prev => ({ ...prev, isDeleting: true, error: null }));
    const id = vehicle.vehicleId || vehicle.id;

    try {
      // API Call: Dispatch DELETE request to /api/vehicles/{id} (or by registration number)
      const url = id ? `/api/vehicles/${id}` : `/api/vehicles/reg/${encodeURIComponent(vehicle.regNo)}`;
      const response = await fetch(url, {
        method: 'DELETE',
        headers: { 'Accept': 'application/json' }
      });

      if (!response.ok) {
        // Fallback endpoint attempt
        const fallbackUrl = `/vehicles/api/${id}`;
        const fallbackRes = await fetch(fallbackUrl, { method: 'DELETE' });
        if (!fallbackRes.ok) {
          throw new Error(`Failed to remove vehicle #${id} (${response.statusText})`);
        }
      }

      // Immediate State Update: Remove record from local React state
      // This immediately decrements that brand's "Models in Park" badge count without browser reload
      setVehicles(prevVehicles => 
        prevVehicles.filter(v => {
          if (id && (v.vehicleId || v.id)) {
            return (v.vehicleId || v.id) !== id;
          }
          return v.regNo !== vehicle.regNo;
        })
      );

      setNotification({
        type: 'success',
        message: `Vehicle ${vehicle.model || vehicle.regNo} successfully removed from fleet operations.`
      });

      if (onVehicleDeleted) {
        onVehicleDeleted(vehicle);
      }

      setDeleteModal({ isOpen: false, vehicle: null, isDeleting: false, error: null });
    } catch (err) {
      console.error('Vehicle deletion error:', err);
      setDeleteModal(prev => ({
        ...prev,
        isDeleting: false,
        error: err.message || 'Unable to delete vehicle from fleet.'
      }));
    }
  };

  // Handler for adding a new vehicle to state
  const handleAddVehicle = (newVehicle) => {
    if (!newVehicle) return;
    // Immediate State Update: Append to active state so brand count immediately increments
    setVehicles(prev => [newVehicle, ...prev]);
    setNotification({
      type: 'success',
      message: `Vehicle ${newVehicle.model || newVehicle.regNo} added to park inventory.`
    });
    if (onVehicleAdded) {
      onVehicleAdded(newVehicle);
    }
  };

  // Filtered vehicles for table display
  const displayedVehicles = vehicles.filter(v => {
    const matchBrand = !selectedBrand || (v.brand && v.brand.toLowerCase() === selectedBrand.toLowerCase());
    if (!matchBrand) return false;
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      (v.model && v.model.toLowerCase().includes(q)) ||
      (v.regNo && v.regNo.toLowerCase().includes(q)) ||
      (v.brand && v.brand.toLowerCase().includes(q))
    );
  });

  return (
    <div className="staff-vehicle-management" style={{ maxWidth: '1200px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' }}>
      
      {/* Alert Notification */}
      {notification && (
        <div style={{
          marginBottom: '1.25rem',
          padding: '0.85rem 1.25rem',
          borderRadius: '8px',
          background: notification.type === 'success' ? '#ecfdf5' : '#fef2f2',
          border: `1px solid ${notification.type === 'success' ? '#a7f3d0' : '#fecaca'}`,
          color: notification.type === 'success' ? '#065f46' : '#991b1b',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span>{notification.message}</span>
          <button
            onClick={() => setNotification(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', fontWeight: 700, color: 'inherit' }}
          >
            ✕
          </button>
        </div>
      )}

      {/* 1. FILTER BY VEHICLE BRAND - DYNAMIC BRAND CARDS */}
      <div style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '1.5rem',
        marginBottom: '2rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#0f172a', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span>🏎️</span>
              Filter by Vehicle Brand
            </h3>
            <p style={{ fontSize: '0.85rem', color: '#64748b', margin: '0.25rem 0 0 0' }}>
              Click a manufacturer to view the certified models stationed in our park.
            </p>
          </div>
          {selectedBrand && (
            <button
              onClick={() => setSelectedBrand(null)}
              style={{
                background: '#f8fafc',
                border: '1px solid #cbd5e1',
                borderRadius: '8px',
                padding: '0.35rem 0.85rem',
                fontSize: '0.82rem',
                fontWeight: 600,
                color: '#334155',
                cursor: 'pointer'
              }}
            >
              &larr; View All Brands
            </button>
          )}
        </div>

        {/* 5 Brand Cards Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
          {BRANDS.map(b => {
            const isSelected = selectedBrand && selectedBrand.toLowerCase() === b.name.toLowerCase();
            return (
              <BrandCard
                key={b.name}
                brand={b}
                isSelected={isSelected}
                onSelect={setSelectedBrand}
              />
            );
          })}
        </div>
      </div>

      {/* 2. FLEET TABLE CONTROLS & SEARCH */}
      <div style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '12px',
        padding: '1rem 1.25rem',
        marginBottom: '1.5rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '0.75rem'
      }}>
        <div style={{ flex: 1, minWidth: '240px' }}>
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Search by model, brand, or reg number (e.g. Prius, WP CA)..."
            style={{
              width: '100%',
              padding: '0.55rem 0.85rem',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              fontSize: '0.88rem'
            }}
          />
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {selectedBrand && (
            <span style={{ fontSize: '0.85rem', color: '#4f46e5', fontWeight: 600 }}>
              Filtered: {selectedBrand} ({displayedVehicles.length} cars)
            </span>
          )}
        </div>
      </div>

      {/* 3. STAFF VEHICLE LIST TABLE WITH POPULATED ACTIONS COLUMN */}
      <div className="df-table-container" style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '12px',
        overflow: 'hidden',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
      }}>
        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
            Loading vehicle fleet records...
          </div>
        ) : displayedVehicles.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
            No vehicle records found matching current criteria.
          </div>
        ) : (
          <table className="df-table" style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontSize: '0.8rem', textTransform: 'uppercase' }}>
                <th style={{ padding: '0.85rem 1rem' }}>Vehicle ID</th>
                <th style={{ padding: '0.85rem 1rem' }}>Reg Number</th>
                <th style={{ padding: '0.85rem 1rem' }}>Model</th>
                <th style={{ padding: '0.85rem 1rem' }}>Branch</th>
                <th style={{ padding: '0.85rem 1rem' }}>Color</th>
                <th style={{ padding: '0.85rem 1rem' }}>Mileage</th>
                <th style={{ padding: '0.85rem 1rem' }}>Status</th>
                <th style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {displayedVehicles.map(v => {
                const vid = v.vehicleId || v.id;
                return (
                  <tr
                    key={vid || v.regNo}
                    data-testid={`vehicle-row-${vid || v.regNo}`}
                    style={{ borderBottom: '1px solid #f1f5f9', transition: 'background 0.15s' }}
                  >
                    <td style={{ padding: '0.85rem 1rem', fontWeight: 700, color: '#334155' }}>
                      #VH-{vid || '—'}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', fontWeight: 700, color: '#0f172a' }}>
                      {v.regNo}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: '#0f172a' }}>
                      {v.model}
                    </td>
                    <td style={{ padding: '0.85rem 1rem', color: '#475569' }}>
                      {v.branchName || 'Colombo Central'}
                    </td>
                    <td style={{ padding: '0.85rem 1rem' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                        <span style={{
                          width: '12px',
                          height: '12px',
                          borderRadius: '50%',
                          border: '1px solid #cbd5e1',
                          backgroundColor: v.color ? v.color.toLowerCase() : '#94a3b8'
                        }} />
                        <span>{v.color || 'Standard'}</span>
                      </span>
                    </td>
                    <td style={{ padding: '0.85rem 1rem', color: '#475569' }}>
                      {v.mileage ? v.mileage.toLocaleString() + ' km' : '-'}
                    </td>
                    <td style={{ padding: '0.85rem 1rem' }}>
                      <span style={{
                        padding: '0.2rem 0.6rem',
                        borderRadius: '9999px',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        background: v.status === 'AVAILABLE' ? '#d1fae5' : v.status === 'BOOKED' ? '#fef3c7' : '#fee2e2',
                        color: v.status === 'AVAILABLE' ? '#065f46' : v.status === 'BOOKED' ? '#92400e' : '#991b1b'
                      }}>
                        {v.status || 'AVAILABLE'}
                      </span>
                    </td>

                    {/* POPULATED ACTIONS COLUMN WITH RED DELETE BUTTON */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm btn-delete-vehicle"
                        data-testid={`delete-btn-${vid || v.regNo}`}
                        onClick={() => promptDeleteVehicle(v)}
                        style={{
                          backgroundColor: '#dc2626',
                          color: '#ffffff',
                          border: 'none',
                          padding: '0.4rem 0.85rem',
                          borderRadius: '6px',
                          fontSize: '0.82rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.35rem',
                          boxShadow: '0 1px 2px rgba(220, 38, 38, 0.2)',
                          transition: 'background 0.2s'
                        }}
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                        Delete
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* 4. DELETE CONFIRMATION MODAL */}
      {deleteModal.isOpen && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(15, 23, 42, 0.6)',
          zIndex: 9999,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backdropFilter: 'blur(2px)'
        }}>
          <div style={{
            background: '#ffffff',
            borderRadius: '16px',
            maxWidth: '440px',
            width: '90%',
            padding: '2rem',
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            border: '1px solid #e2e8f0',
            textAlign: 'center'
          }}>
            <div style={{
              width: '52px',
              height: '52px',
              borderRadius: '50%',
              background: '#fee2e2',
              color: '#dc2626',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.25rem',
              fontSize: '1.6rem'
            }}>
              ⚠️
            </div>

            <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.5rem 0' }}>
              Confirm Fleet Removal
            </h3>

            <p style={{ fontSize: '0.92rem', color: '#64748b', margin: '0 0 1.5rem 0', lineHeight: 1.5 }}>
              Are you sure you want to remove this vehicle from the fleet?
            </p>

            {deleteModal.vehicle && (
              <div style={{
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                padding: '0.75rem',
                marginBottom: '1.5rem',
                fontSize: '0.85rem',
                color: '#334155'
              }}>
                <strong>{deleteModal.vehicle.model}</strong> ({deleteModal.vehicle.regNo}) &bull; {deleteModal.vehicle.brand}
              </div>
            )}

            {deleteModal.error && (
              <div style={{
                background: '#fef2f2',
                border: '1px solid #fecaca',
                color: '#991b1b',
                padding: '0.65rem',
                borderRadius: '6px',
                fontSize: '0.82rem',
                marginBottom: '1.25rem'
              }}>
                {deleteModal.error}
              </div>
            )}

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <button
                type="button"
                onClick={closeDeleteModal}
                disabled={deleteModal.isDeleting}
                style={{
                  padding: '0.6rem 1.25rem',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  background: '#f8fafc',
                  color: '#475569',
                  fontWeight: 600,
                  fontSize: '0.88rem',
                  cursor: deleteModal.isDeleting ? 'not-allowed' : 'pointer'
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                data-testid="confirm-delete-button"
                onClick={confirmDeleteVehicle}
                disabled={deleteModal.isDeleting}
                style={{
                  padding: '0.6rem 1.25rem',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#dc2626',
                  color: '#ffffff',
                  fontWeight: 700,
                  fontSize: '0.88rem',
                  cursor: deleteModal.isDeleting ? 'not-allowed' : 'pointer',
                  boxShadow: '0 4px 6px -1px rgba(220, 38, 38, 0.25)'
                }}
              >
                {deleteModal.isDeleting ? 'Removing...' : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export { StaffVehicleTable };
