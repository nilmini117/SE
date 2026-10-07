import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/**
 * DriveFlow Booking Reservation Form (React Component)
 * 
 * Features:
 * 1. Data Transfer & Dynamic Autofill:
 *    - Uses useLocation() to intercept passed router state: location.state?.selectedVehicle
 *    - Falls back to sessionStorage or URL query params if opened directly
 *    - Binds vehicle_id, model, branch, daily_rate directly to form state variables
 * 2. Validation & UI Lock:
 *    - Disables the manual "Vehicle Selection" dropdown when data is successfully imported from dashboard
 *    - Renders a prominent "Auto-Selected from Fleet Dashboard" banner
 *    - Provides a "Change Vehicle" action to unlock and re-enable manual selection
 * 3. Daily Pricing & Scheduling:
 *    - Automatically calculates duration (days) and total cost formatted in Sri Lankan Rupees (Rs.)
 */
export default function BookingReservationForm({ currentUser = null, branches: propBranches = null, additionalServices: propAddons = null }) {
  const location = useLocation();
  const navigate = useNavigate();

  // 1. State Integration (Booking Page):
  // Utilize useLocation hook to retrieve the selectedVehicle object passed from the dashboard
  const passedVehicle = location?.state?.selectedVehicle || null;

  // Fallback branches list if not supplied via props
  const defaultBranches = [
    { id: 1, name: 'Colombo Central Station', city: 'Colombo', street: '45 Station Road' },
    { id: 2, name: 'Airport Express Hub (Katunayake)', city: 'Katunayake', street: 'Bandaranaike Int Airport' },
    { id: 3, name: 'Kandy Heritage Hub', city: 'Kandy', street: '12 Dalada Veediya' },
    { id: 4, name: 'Galle Coastal Office', city: 'Galle', street: '88 Marine Drive' },
    { id: 5, name: 'Negombo Coastal Hub', city: 'Negombo', street: '22 Beach Road' }
  ];

  const branches = propBranches || defaultBranches;

  // Default catalogue for Extras & Add-ons (additional_service)
  const defaultAddons = [
    { id: 1, name: 'GPS Navigation System', rate: 500, description: 'Turn-by-turn satellite voice guidance across Sri Lanka' },
    { id: 2, name: 'Child Safety Seat', rate: 750, description: 'ISOFIX certified rear & front-facing child booster seat' },
    { id: 3, name: 'Comprehensive Collision Damage Waiver (CDW)', rate: 2500, description: 'Zero deductible excess waiver for ultimate peace of mind' },
    { id: 4, name: 'Additional Registered Driver', rate: 1000, description: 'Authorize a secondary verified driver on your rental contract' },
    { id: 5, name: 'Emergency 24/7 Roadside Assistance', rate: 450, description: 'Island-wide towing, jump-start, and flat-tire recovery' }
  ];

  const addons = propAddons || defaultAddons;

  // Helper to extract actual daily rate and normalize vehicle state data
  const normalizeVehicle = (car) => {
    if (!car) return null;
    const rateVal = car.dailyRate ?? car.daily_rate ?? car.rate ?? 12500;
    return {
      ...car,
      vehicle_id: car.vehicle_id || car.id,
      id: car.vehicle_id || car.id,
      dailyRate: Number(rateVal),
      daily_rate: Number(rateVal),
      rate: Number(rateVal),
    };
  };

  // Form State Variables - extracted dynamically with selectedVehicle.dailyRate
  const [selectedVehicle, setSelectedVehicle] = useState(() => {
    if (passedVehicle) return normalizeVehicle(passedVehicle);
    try {
      const raw = sessionStorage.getItem('selectedVehicle');
      if (raw) return normalizeVehicle(JSON.parse(raw));
    } catch (e) {}
    return {
      id: 1,
      vehicle_id: 1,
      model: 'Toyota Prius 2024 Prime',
      branch: 'Colombo Central Station',
      dailyRate: 12500,
      daily_rate: 12500,
      rate: 12500,
    };
  });

  const [isVehicleLocked, setIsVehicleLocked] = useState(false);
  const [pickupBranchId, setPickupBranchId] = useState('');
  const [returnBranchId, setReturnBranchId] = useState('');
  
  const today = new Date().toISOString().split('T')[0];
  const threeDaysLater = new Date(Date.now() + 3 * 86400000).toISOString().split('T')[0];
  
  const [pickupDate, setPickupDate] = useState(today);
  const [returnDate, setReturnDate] = useState(threeDaysLater);
  const [couponCode, setCouponCode] = useState('');
  const [discountPercent, setDiscountPercent] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState(null);

  // React state managing the selected "Extras & Add-ons" checkboxes
  const [selectedAddonIds, setSelectedAddonIds] = useState([]);

  // Add-on checkbox toggle handler
  const handleAddonToggle = (addonId) => {
    setSelectedAddonIds((prev) =>
      prev.includes(addonId)
        ? prev.filter((id) => id !== addonId)
        : [...prev, addonId]
    );
  };

  // Available vehicles list when choosing manually
  const [availableVehicles, setAvailableVehicles] = useState([]);
  const [manualVehicleId, setManualVehicleId] = useState('');

  // 2. Dynamic Autofill Effect: Runs on mount or when router state changes
  useEffect(() => {
    let car = passedVehicle;

    // Check sessionStorage fallback if router state wasn't passed directly
    if (!car) {
      try {
        const raw = sessionStorage.getItem('selectedVehicle');
        if (raw) car = JSON.parse(raw);
      } catch (e) {
        console.warn('Session storage inaccessible:', e);
      }
    }

    if (car && (car.vehicle_id || car.id)) {
      const normalized = normalizeVehicle(car);
      setSelectedVehicle(normalized);
      setManualVehicleId(String(normalized.vehicle_id || normalized.id));
      setIsVehicleLocked(true); // Disable manual dropdown

      // Attempt to auto-match the pickup branch from the car data
      if (normalized.branch) {
        const matched = branches.find(b => 
          normalized.branch.toLowerCase().includes(b.name.toLowerCase()) || 
          normalized.branch.toLowerCase().includes(b.city.toLowerCase()) ||
          b.name.toLowerCase().includes(normalized.branch.toLowerCase())
        );
        if (matched) {
          setPickupBranchId(String(matched.id));
        }
      }
    }
  }, [passedVehicle]);

  // Handle manual branch change to fetch vehicles if not locked
  useEffect(() => {
    if (pickupBranchId && !isVehicleLocked) {
      fetch(`/bookings/vehicles-by-branch?branchId=${pickupBranchId}`)
        .then(res => res.ok ? res.json() : [])
        .then(data => setAvailableVehicles(data))
        .catch(() => {
          setAvailableVehicles([
            { vehicleId: 1, model: 'Toyota Prius Prime', regNo: 'WP CA-1020', dailyRate: 12500 },
            { vehicleId: 2, model: 'Benz C200 AMG', regNo: 'WP BC-5001', dailyRate: 28000 }
          ]);
        });
    }
  }, [pickupBranchId, isVehicleLocked]);

  // Handle manual vehicle selection
  const handleManualVehicleChange = (vId) => {
    setManualVehicleId(vId);
    const found = availableVehicles.find(v => String(v.vehicleId || v.id) === String(vId));
    if (found) {
      setSelectedVehicle(normalizeVehicle(found));
    }
  };

  // Calculate rental duration in days (defaults to 3 days)
  const rentalDays = Math.max(
    1,
    Math.ceil((new Date(returnDate) - new Date(pickupDate)) / (1000 * 60 * 60 * 24))
  );
  const durationDays = rentalDays;

  // Frontend calculation function:
  // Loop through the selected add-ons, multiply their daily rate by the rental duration (3 days), and add them to the total
  let totalAddonCost = 0;
  for (const addonId of selectedAddonIds) {
    const addon = addons.find((a) => a.id === addonId);
    if (addon) {
      totalAddonCost += (Number(addon.rate) || 0) * durationDays;
    }
  }

  // Extract actual daily rate and compute costs dynamically
  const activeDailyRate = selectedVehicle?.dailyRate || 12500;
  const baseTotal = activeDailyRate * rentalDays;
  const discountAmount = (baseTotal * discountPercent) / 100;
  const finalTotal = baseTotal - discountAmount + totalAddonCost;

  // Action: Unlock / Change Vehicle
  const handleUnlockVehicle = () => {
    setIsVehicleLocked(false);
    setSelectedVehicle({ dailyRate: 0, model: '' });
    setManualVehicleId('');
    try {
      sessionStorage.removeItem('selectedVehicle');
    } catch (e) {}
  };

  // Form Submission
  const handleSubmit = (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    const payload = {
      vehicleId: selectedVehicle?.vehicle_id || selectedVehicle?.id || manualVehicleId,
      pickupBranchId,
      returnBranchId: returnBranchId || pickupBranchId,
      pickupDate,
      returnDate,
      durationDays,
      serviceIds: selectedAddonIds,
      totalAddonCost,
      totalCharged: finalTotal,
      couponCode
    };

    setTimeout(() => {
      setIsSubmitting(false);
      setBookingSuccess({
        id: 'DF-' + Math.floor(100000 + Math.random() * 900000),
        model: selectedVehicle?.model || 'Reserved Vehicle',
        branch: branches.find(b => String(b.id) === String(pickupBranchId))?.name || 'Selected Branch',
        baseTotal,
        totalAddonCost,
        total: finalTotal,
        pickupDate,
        returnDate
      });
      try {
        sessionStorage.removeItem('selectedVehicle');
      } catch (e) {}
    }, 800);
  };

  if (bookingSuccess) {
    return (
      <div style={{ maxWidth: '640px', margin: '3rem auto', padding: '2.5rem', backgroundColor: '#ffffff', borderRadius: '20px', border: '1px solid #e2e8f0', boxShadow: '0 20px 40px -10px rgba(15, 23, 42, 0.1)', textAlign: 'center', fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif" }}>
        <div style={{ width: '64px', height: '64px', borderRadius: '50%', backgroundColor: '#ecfdf5', color: '#16a34a', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem', fontSize: '2rem' }}>
          ✓
        </div>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.5rem' }}>Reservation Confirmed!</h2>
        <p style={{ color: '#64748b', fontSize: '0.95rem', margin: '0 0 1.5rem' }}>
          Your booking confirmation has been dispatched to your email.
        </p>
        <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '1.25rem', textAlign: 'left', marginBottom: '2rem', fontSize: '0.9rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', borderBottom: '1px dashed #e2e8f0' }}>
            <span style={{ color: '#64748b' }}>Booking Reference:</span>
            <strong style={{ color: '#0f172a' }}>{bookingSuccess.id}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', borderBottom: '1px dashed #e2e8f0' }}>
            <span style={{ color: '#64748b' }}>Vehicle:</span>
            <strong style={{ color: '#0f172a' }}>{bookingSuccess.model}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', borderBottom: '1px dashed #e2e8f0' }}>
            <span style={{ color: '#64748b' }}>Pickup Station:</span>
            <strong style={{ color: '#0f172a' }}>{bookingSuccess.branch}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', borderBottom: '1px dashed #e2e8f0' }}>
            <span style={{ color: '#64748b' }}>Base Rental ({durationDays} Days):</span>
            <strong style={{ color: '#0f172a' }}>Rs. {bookingSuccess.baseTotal?.toLocaleString()}</strong>
          </div>
          {bookingSuccess.totalAddonCost > 0 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0', borderBottom: '1px dashed #e2e8f0' }}>
              <span style={{ color: '#0284c7' }}>Extras &amp; Add-ons:</span>
              <strong style={{ color: '#0284c7' }}>+ Rs. {bookingSuccess.totalAddonCost?.toLocaleString()}</strong>
            </div>
          )}
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.35rem 0' }}>
            <span style={{ color: '#64748b' }}>Total Rental Amount:</span>
            <strong style={{ color: '#16a34a', fontSize: '1.1rem' }}>Rs. {bookingSuccess.total.toLocaleString()}</strong>
          </div>
        </div>
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          <button
            type="button"
            onClick={() => navigate('/bookings')}
            style={{ backgroundColor: '#0f172a', color: '#ffffff', border: 'none', padding: '0.75rem 1.5rem', borderRadius: '9999px', fontWeight: 700, cursor: 'pointer' }}
          >
            View My Bookings
          </button>
          <button
            type="button"
            onClick={() => navigate('/')}
            style={{ backgroundColor: '#f1f5f9', color: '#334155', border: '1px solid #cbd5e1', padding: '0.75rem 1.5rem', borderRadius: '9999px', fontWeight: 700, cursor: 'pointer' }}
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '800px', margin: '2rem auto', padding: '0 1.5rem', fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif" }}>
      
      {/* Back Link */}
      <div style={{ marginBottom: '1.25rem' }}>
        <button
          type="button"
          onClick={() => navigate('/')}
          style={{ background: 'none', border: 'none', color: '#64748b', fontSize: '0.9rem', fontWeight: 600, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '0.35rem', padding: 0 }}
        >
          &larr; Back to Fleet Dashboard
        </button>
      </div>

      <div style={{ backgroundColor: '#ffffff', borderRadius: '20px', border: '1px solid #e2e8f0', boxShadow: '0 10px 30px -5px rgba(15, 23, 42, 0.05)', padding: '2.25rem' }}>
        
        {/* Form Title & Badge */}
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.3rem 0.8rem', backgroundColor: '#ecfdf5', color: '#15803d', borderRadius: '9999px', fontSize: '0.78rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem' }}>
          🚗 Customer Reservation Engine
        </div>
        <h1 style={{ fontSize: '1.95rem', fontWeight: 800, color: '#0f172a', margin: '0 0 1.5rem', letterSpacing: '-0.025em' }}>
          Pick your choice in our park
        </h1>

        <form onSubmit={handleSubmit}>
          
          {/* SECTION 1: PICKUP & RETURN BRANCH */}
          <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '1.35rem', marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', margin: '0 0 1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ width: '24px', height: '24px', borderRadius: '50%', backgroundColor: '#16a34a', color: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem' }}>1</span>
              Branch Selection
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Pickup Branch *
                </label>
                <select
                  value={pickupBranchId}
                  onChange={(e) => setPickupBranchId(e.target.value)}
                  required
                  style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '10px', border: '1px solid #cbd5e1', backgroundColor: '#ffffff', fontSize: '0.9rem', color: '#0f172a', outline: 'none' }}
                >
                  <option value="">-- Choose Pickup Branch --</option>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name} ({b.city})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Return Branch (Optional)
                </label>
                <select
                  value={returnBranchId}
                  onChange={(e) => setReturnBranchId(e.target.value)}
                  style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '10px', border: '1px solid #cbd5e1', backgroundColor: '#ffffff', fontSize: '0.9rem', color: '#0f172a', outline: 'none' }}
                >
                  <option value="">-- Same as Pickup Branch --</option>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name} ({b.city})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {/* SECTION 2: VEHICLE SELECTION (DYNAMIC AUTOFILL & LOCK) */}
          <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '1.35rem', marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', margin: '0 0 0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ width: '24px', height: '24px', borderRadius: '50%', backgroundColor: '#16a34a', color: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem' }}>2</span>
              Vehicle Selection *
            </h3>

            {/* DYNAMIC AUTOFILL BANNER: Shown when clicked from dashboard */}
            {isVehicleLocked && selectedVehicle && (
              <div style={{ backgroundColor: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '12px', padding: '0.85rem 1.15rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                  <img
                    src={selectedVehicle.imageUrl || selectedVehicle.image || '/images/category_economy.jpg'}
                    alt={selectedVehicle.model}
                    style={{ width: '72px', height: '48px', objectFit: 'cover', borderRadius: '8px', border: '1px solid #cbd5e1', backgroundColor: '#ffffff' }}
                  />
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', fontWeight: 700, color: '#15803d', letterSpacing: '0.05em' }}>
                        Auto-Selected from Fleet Dashboard
                      </span>
                      {(selectedVehicle.isUnderMaintenance || selectedVehicle.status === 'MAINTENANCE') && (
                        <span style={{ padding: '0.15rem 0.5rem', backgroundColor: '#fee2e2', color: '#dc2626', border: '1px solid #fca5a5', borderRadius: '9999px', fontSize: '0.72rem', fontWeight: 700 }}>
                          ⚠️ Under Service until {selectedVehicle.formattedServiceEndDate || (selectedVehicle.serviceEndDate ? String(selectedVehicle.serviceEndDate) : '31/12/2026')}
                        </span>
                      )}
                    </div>
                    <strong style={{ color: '#065f46', fontSize: '1rem', display: 'block', marginTop: '0.15rem' }}>{selectedVehicle.model}</strong>
                    <span style={{ fontSize: '0.82rem', color: '#047857' }}>
                      {selectedVehicle.branch || 'Designated Branch'} • Rs. {Number(selectedVehicle.dailyRate).toLocaleString()}/day
                    </span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={handleUnlockVehicle}
                  style={{ backgroundColor: '#ffffff', border: '1px solid #86efac', color: '#15803d', fontSize: '0.78rem', fontWeight: 700, padding: '0.35rem 0.75rem', borderRadius: '9999px', cursor: 'pointer' }}
                >
                  Change Vehicle
                </button>
              </div>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                Available Vehicles *
              </label>
              <select
                value={isVehicleLocked ? (selectedVehicle?.vehicle_id || selectedVehicle?.id || '') : manualVehicleId}
                onChange={(e) => setManualVehicleId(e.target.value)}
                disabled={isVehicleLocked} // Disables manual dropdown when imported per requirement
                required
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: '10px',
                  border: '1px solid #cbd5e1',
                  backgroundColor: isVehicleLocked ? '#f1f5f9' : '#ffffff',
                  color: isVehicleLocked ? '#64748b' : '#0f172a',
                  fontSize: '0.9rem',
                  cursor: isVehicleLocked ? 'not-allowed' : 'pointer',
                  outline: 'none',
                }}
              >
                {isVehicleLocked && selectedVehicle ? (
                  <option value={selectedVehicle.vehicle_id || selectedVehicle.id}>
                    {selectedVehicle.model} ({selectedVehicle.branch || 'Fleet Station'}) — Pre-selected from Dashboard
                  </option>
                ) : (
                  <>
                    <option value="">-- Select from available vehicles --</option>
                    {availableVehicles.map((v) => (
                      <option key={v.vehicleId} value={v.vehicleId}>
                        {v.model} [{v.regNo}] — Rs. {v.dailyRate?.toLocaleString() || '12,500'}/day
                      </option>
                    ))}
                  </>
                )}
              </select>
              <small style={{ display: 'block', marginTop: '0.35rem', fontSize: '0.8rem', color: isVehicleLocked ? '#16a34a' : '#d97706', fontWeight: 500 }}>
                {isVehicleLocked
                  ? '🔒 Vehicle locked from dashboard selection. Click "Change Vehicle" above to select manually.'
                  : '⚠️ Choose a pickup branch first to view stationed vehicles.'}
              </small>
            </div>
          </div>

          {/* SECTION 3: RENTAL DATES & SCHEDULE */}
          <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '1.35rem', marginBottom: '1.5rem' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', margin: '0 0 1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ width: '24px', height: '24px', borderRadius: '50%', backgroundColor: '#16a34a', color: '#fff', display: 'inline-flex', alignItems: 'center', justifyCenter: 'center', fontSize: '0.75rem' }}>3</span>
              Rental Schedule *
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Pickup Date *
                </label>
                <input
                  type="date"
                  value={pickupDate}
                  min={today}
                  onChange={(e) => setPickupDate(e.target.value)}
                  required
                  style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '10px', border: '1px solid #cbd5e1', backgroundColor: '#ffffff', fontSize: '0.9rem', color: '#0f172a', outline: 'none' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: '#334155', marginBottom: '0.35rem' }}>
                  Return Date *
                </label>
                <input
                  type="date"
                  value={returnDate}
                  min={pickupDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                  required
                  style={{ width: '100%', padding: '0.65rem 0.85rem', borderRadius: '10px', border: '1px solid #cbd5e1', backgroundColor: '#ffffff', fontSize: '0.9rem', color: '#0f172a', outline: 'none' }}
                />
              </div>
            </div>
          </div>

          {/* SECTION 4: EXTRAS & ADD-ONS (additional_service checkboxes) */}
          <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '1.35rem', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#0f172a', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span style={{ width: '24px', height: '24px', borderRadius: '50%', backgroundColor: '#16a34a', color: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.75rem' }}>4</span>
                Extras &amp; Add-ons (Optional)
              </h3>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0369a1', backgroundColor: '#e0f2fe', padding: '0.2rem 0.6rem', borderRadius: '9999px', border: '1px solid #bae6fd' }}>
                ✨ Additional Services
              </span>
            </div>
            <p style={{ fontSize: '0.82rem', color: '#64748b', margin: '0 0 1rem' }}>
              Select optional equipment or add-ons. Rates are multiplied by your rental duration ({durationDays} days).
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.85rem' }}>
              {addons.map((addon) => {
                const isChecked = selectedAddonIds.includes(addon.id);
                return (
                  <label
                    key={addon.id}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: '0.75rem',
                      padding: '0.85rem 1rem',
                      borderRadius: '10px',
                      border: isChecked ? '1.5px solid #0284c7' : '1px solid #cbd5e1',
                      backgroundColor: isChecked ? '#f0f9ff' : '#ffffff',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => handleAddonToggle(addon.id)}
                      style={{ marginTop: '0.2rem', transform: 'scale(1.15)', accentColor: '#0284c7', cursor: 'pointer' }}
                    />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700, color: '#1e293b', fontSize: '0.88rem' }}>
                        {addon.name}
                      </div>
                      <div style={{ fontSize: '0.78rem', color: '#0284c7', fontWeight: 700, marginTop: '0.2rem' }}>
                        Rs. {addon.rate.toLocaleString()} / day
                      </div>
                      {addon.description && (
                        <div style={{ fontSize: '0.74rem', color: '#64748b', marginTop: '0.15rem' }}>
                          {addon.description}
                        </div>
                      )}
                    </div>
                  </label>
                );
              })}
            </div>
          </div>

          {/* SECTION 5: PRICING ENGINE (CALCULATED PAYMENT ARRAY) */}
          <div style={{ backgroundColor: '#ffffff', border: '2px solid #e2e8f0', borderRadius: '14px', overflow: 'hidden', marginBottom: '2rem' }}>
            <div style={{ backgroundColor: '#f1f5f9', padding: '0.75rem 1.25rem', borderBottom: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#1e293b' }}>
                Calculated Payment Array &amp; Cost Structure
              </span>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#15803d', backgroundColor: '#ecfdf5', padding: '0.2rem 0.6rem', borderRadius: '9999px' }}>
                Standard Fleet Rate
              </span>
            </div>
            <div style={{ padding: '1.25rem' }}>
              {/* 1. Base Rental Rate */}
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.45rem 0', borderBottom: '1px dashed #e2e8f0', fontSize: '0.92rem' }}>
                <span style={{ color: '#64748b' }}>
                  Base Rental Rate (Rs. {selectedVehicle.dailyRate.toLocaleString()} × {rentalDays} Days)
                </span>
                <strong style={{ color: '#0f172a' }}>
                  Rs. {baseTotal.toLocaleString()}
                </strong>
              </div>

              {/* 2. Expected UI Update: New line item directly below the Base Rental Rate to display the total add-on cost */}
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.45rem 0', borderBottom: '1px dashed #e2e8f0', fontSize: '0.92rem' }}>
                <span style={{ color: '#0284c7' }}>
                  Extras &amp; Add-ons ({selectedAddonIds.length} Selected &bull; {durationDays} Days)
                </span>
                <strong style={{ color: '#0284c7' }}>
                  + Rs. {totalAddonCost.toLocaleString()}
                </strong>
              </div>

              {discountAmount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.45rem 0', borderBottom: '1px dashed #e2e8f0', fontSize: '0.92rem' }}>
                  <span style={{ color: '#16a34a' }}>
                    Discount ({discountPercent}%)
                  </span>
                  <strong style={{ color: '#16a34a' }}>
                    - Rs. {discountAmount.toLocaleString()}
                  </strong>
                </div>
              )}

              {/* 3. Final Calculated Cost */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.25rem', paddingTop: '1rem', borderTop: '2px solid #e2e8f0' }}>
                <div>
                  <div style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#64748b', fontWeight: 700 }}>
                    FINAL CALCULATED COST
                  </div>
                  <div style={{ fontSize: '0.78rem', color: '#15803d', fontWeight: 600 }}>Zero Hidden Fees &bull; Instant Confirmation</div>
                </div>
                <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#15803d' }}>
                  Rs. {finalTotal.toLocaleString()}
                </div>
              </div>
            </div>
          </div>

          {/* Submit Action */}
          <button
            type="submit"
            disabled={isSubmitting}
            style={{
              width: '100%',
              backgroundColor: '#16a34a',
              color: '#ffffff',
              padding: '0.95rem 1.5rem',
              borderRadius: '9999px',
              border: 'none',
              fontSize: '1.05rem',
              fontWeight: 800,
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              boxShadow: '0 4px 14px rgba(22, 163, 74, 0.4)',
              transition: 'all 0.15s ease',
            }}
          >
            {isSubmitting ? 'Confirming Reservation...' : 'Confirm & Reserve Vehicle'}
          </button>

        </form>

      </div>
    </div>
  );
}
