import React, { useState, useEffect, useMemo } from 'react';
import FilterByVehicleBrand from './FilterByVehicleBrand';
import StaffVehicleTable from './StaffVehicleTable';

export { FilterByVehicleBrand, StaffVehicleTable };

/**
 * DriveFlow Customer Vehicle Catalog Component (React)
 * Module: "Find your choice in our park"
 * 
 * Strict UI Rules & Constraints:
 * 1. Category Filter: Customer clicks a manufacturer brand to view the certified models stationed in our park.
 * 2. Dynamic Vehicle Counts: Brand cards render dynamic real-time vehicle counts calculated from live state.
 * 3. Strictly HIDE vehicle_id from all views and DOM elements exposed to customers.
 */
export default function CustomerVehicleCatalog({
  initialBrand = null,
  onSelectVehicle = null,
  vehicles: propVehicles = null,
  liveVehicles = null
}) {
  const brands = [
    { name: 'Toyota', logo: '/images/brand_toyota.png', emoji: '🔴', slogan: 'Hybrid Reliability & Versatile SUVs' },
    { name: 'Suzuki', logo: '/images/brand_suzuki.png', emoji: '🔵', slogan: 'Nimble City Hatchbacks & Compact 4x4s' },
    { name: 'Honda', logo: '/images/brand_honda.png', emoji: '🟡', slogan: 'VTEC Precision & Modern Hybrid Crossovers' },
    { name: 'Tesla', logo: '/images/brand_tesla.png', emoji: '⚡', slogan: 'Cutting-Edge Electric Power & Smart Tech' },
    { name: 'Benz', logo: '/images/brand_benz.png', emoji: '⭐', slogan: 'Executive Luxury, Prestige & Refined Comfort' }
  ];

  // Default fallback park dataset (strictly no vehicle_id displayed)
  const defaultMockPark = [
    // Toyota
    { model: 'Toyota Prius 2024', brand: 'Toyota', regNo: 'WP CA-1020', branchName: 'Colombo Central', color: 'Pearl White', mileage: 35000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Toyota Axio Hybrid', brand: 'Toyota', regNo: 'CP KA-3040', branchName: 'Kandy Hub', color: 'Silver', mileage: 42000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Toyota RAV4 Prime', brand: 'Toyota', regNo: 'WP NC-3344', branchName: 'Negombo Airport', color: 'Midnight Blue', mileage: 18000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    // Suzuki
    { model: 'Suzuki Swift Sport', brand: 'Suzuki', regNo: 'SP GA-4050', branchName: 'Galle Coastal', color: 'Burning Red', mileage: 19000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Suzuki Vitara AllGrip', brand: 'Suzuki', regNo: 'NP JC-5566', branchName: 'Jaffna City Point', color: 'Cool Black', mileage: 25000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Suzuki Jimny 4x4', brand: 'Suzuki', regNo: 'WP SJ-2024', branchName: 'Colombo Central', color: 'Kinetic Yellow', mileage: 12000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    // Honda
    { model: 'Honda Vezel e:HEV', brand: 'Honda', regNo: 'WP CB-2030', branchName: 'Colombo Central', color: 'Crystal Black', mileage: 28000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Honda Civic Turbo', brand: 'Honda', regNo: 'CP KB-1122', branchName: 'Kandy Hub', color: 'Rallye Red', mileage: 21000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Honda CR-V Elegance', brand: 'Honda', regNo: 'WP HC-7788', branchName: 'Negombo Airport', color: 'Platinum White', mileage: 16000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    // Tesla
    { model: 'Tesla Model 3 Dual Motor', brand: 'Tesla', regNo: 'WP TM-3001', branchName: 'Colombo Central', color: 'Deep Metallic Blue', mileage: 8000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Tesla Model Y Long Range', brand: 'Tesla', regNo: 'WP TY-3002', branchName: 'Kandy Hub', color: 'Solid Black', mileage: 9500, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Tesla Model S Plaid', brand: 'Tesla', regNo: 'WP TS-3003', branchName: 'Galle Coastal', color: 'Red Multi-Coat', mileage: 11000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    // Benz
    { model: 'Benz C-Class C200', brand: 'Benz', regNo: 'WP BC-5001', branchName: 'Colombo Central', color: 'Obsidian Black', mileage: 14000, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Benz E-Class E300', brand: 'Benz', regNo: 'WP BE-5002', branchName: 'Kandy Hub', color: 'Iridium Silver', mileage: 17500, status: 'AVAILABLE', bookingUrl: '/bookings/new' },
    { model: 'Benz GLC 300 4MATIC', brand: 'Benz', regNo: 'WP BG-5003', branchName: 'Galle Coastal', color: 'Polar White', mileage: 13000, status: 'AVAILABLE', bookingUrl: '/bookings/new' }
  ];

  const initialVehicles = propVehicles || liveVehicles || null;

  const [selectedBrand, setSelectedBrand] = useState(initialBrand);
  const [allVehicles, setAllVehicles] = useState(initialVehicles || []);
  const [loading, setLoading] = useState(!initialVehicles);
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch all vehicles from backend if not provided as props
  useEffect(() => {
    if (initialVehicles) {
      setAllVehicles(initialVehicles);
      setLoading(false);
      return;
    }

    setLoading(true);
    fetch('/vehicles/api/by-brand?availableOnly=true')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load fleet');
        return res.json();
      })
      .then(data => {
        setAllVehicles(data);
        setLoading(false);
      })
      .catch(err => {
        console.warn('API fetch fallback to embedded park data:', err);
        setAllVehicles(defaultMockPark);
        setLoading(false);
      });
  }, [propVehicles, liveVehicles]);

  // Synchronize when external props change
  useEffect(() => {
    if (propVehicles) {
      setAllVehicles(propVehicles);
    } else if (liveVehicles) {
      setAllVehicles(liveVehicles);
    }
  }, [propVehicles, liveVehicles]);

  // Filter vehicles according to selected brand and search query
  const displayedVehicles = useMemo(() => {
    return allVehicles.filter(v => {
      const matchBrand = !selectedBrand || (v.brand && v.brand.toLowerCase() === selectedBrand.toLowerCase());
      if (!matchBrand) return false;
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase();
      return (v.model && v.model.toLowerCase().includes(q)) || (v.regNo && v.regNo.toLowerCase().includes(q));
    });
  }, [allVehicles, selectedBrand, searchQuery]);

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' }}>
      {/* Catalog Header */}
      <div style={{ marginBottom: '2rem' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.4rem',
          padding: '0.25rem 0.8rem',
          background: '#ede9fe',
          color: '#6d28d9',
          borderRadius: '9999px',
          fontSize: '0.78rem',
          fontWeight: 700,
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          marginBottom: '0.5rem'
        }}>
          🚗 Customer Vehicle Catalog
        </div>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.5rem 0' }}>
          Find your choice in our park
        </h1>
        <p style={{ color: '#64748b', fontSize: '0.95rem', margin: 0 }}>
          Browse our certified park by automotive brand. Click a manufacturer below to view the certified models stationed in our park.
        </p>
      </div>

      {/* STEP 1: BRAND CATEGORY FILTER (Dynamic Count & Clean UI Subtext) */}
      <FilterByVehicleBrand
        vehicles={allVehicles}
        selectedBrand={selectedBrand}
        onSelectBrand={setSelectedBrand}
      />

      {/* STEP 2: NESTED VEHICLE DISPLAY UNDER SELECTED BRAND */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#0f172a', margin: 0 }}>
              {selectedBrand ? `${selectedBrand} Models in Our Park` : 'All Park Fleet Models'}
            </h2>
            <p style={{ color: '#64748b', fontSize: '0.85rem', margin: '0.2rem 0 0 0' }}>
              Strict customer privacy mode: Individual database IDs strictly hidden.
            </p>
          </div>

          <div style={{ minWidth: '240px' }}>
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Filter by model name..."
              style={{
                width: '100%',
                padding: '0.55rem 0.85rem',
                border: '1px solid #cbd5e1',
                borderRadius: '8px',
                fontSize: '0.88rem'
              }}
            />
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
            Loading vehicles from park...
          </div>
        ) : displayedVehicles.length === 0 ? (
          <div style={{
            background: '#ffffff',
            border: '1px solid #e2e8f0',
            borderRadius: '12px',
            padding: '3rem',
            textAlign: 'center',
            color: '#64748b'
          }}>
            No models found matching your filter.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.25rem' }}>
            {displayedVehicles.map((car, idx) => (
              <div
                key={car.regNo || idx}
                style={{
                  background: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: '14px',
                  padding: '1.25rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.04)',
                  transition: 'transform 0.2s',
                  position: 'relative'
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.6rem' }}>
                    <span style={{
                      padding: '0.15rem 0.6rem',
                      borderRadius: '9999px',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      background: '#ede9fe',
                      color: '#6d28d9'
                    }}>
                      {car.brand}
                    </span>
                    <span style={{
                      padding: '0.15rem 0.6rem',
                      borderRadius: '9999px',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      background: '#d1fae5',
                      color: '#065f46'
                    }}>
                      {car.status || 'Available'}
                    </span>
                  </div>

                  {/* STRICT RULE: Strictly NO vehicle_id rendered */}
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.35rem 0' }}>
                    {car.model}
                  </h3>

                  <div style={{ fontSize: '0.82rem', color: '#64748b', marginBottom: '0.85rem' }}>
                    Registration: <strong style={{ color: '#334155' }}>{car.regNo}</strong>
                  </div>

                  <div style={{
                    background: '#f8fafc',
                    borderRadius: '8px',
                    padding: '0.75rem',
                    fontSize: '0.82rem',
                    color: '#475569',
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr',
                    gap: '0.5rem',
                    marginBottom: '1.25rem'
                  }}>
                    <div>
                      <div style={{ color: '#94a3b8', fontSize: '0.72rem', textTransform: 'uppercase' }}>Stationed Branch</div>
                      <strong>{car.branchName || 'Head Office'}</strong>
                    </div>
                    <div>
                      <div style={{ color: '#94a3b8', fontSize: '0.72rem', textTransform: 'uppercase' }}>Odometer</div>
                      <strong>{car.mileage ? car.mileage.toLocaleString() + ' km' : 'Low mileage'}</strong>
                    </div>
                    <div style={{ gridColumn: 'span 2', display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.2rem' }}>
                      <span style={{
                        width: '12px',
                        height: '12px',
                        borderRadius: '50%',
                        background: car.color ? car.color.toLowerCase() : '#94a3b8',
                        border: '1px solid #cbd5e1'
                      }} />
                      <span>Color: <strong>{car.color || 'Standard'}</strong></span>
                    </div>
                  </div>
                </div>

                <a
                  href={car.bookingUrl || '/bookings/new'}
                  onClick={(e) => {
                    if (onSelectVehicle) {
                      e.preventDefault();
                      onSelectVehicle(car);
                    }
                  }}
                  style={{
                    display: 'block',
                    textAlign: 'center',
                    background: '#4f46e5',
                    color: '#ffffff',
                    fontWeight: 700,
                    fontSize: '0.92rem',
                    padding: '0.7rem 1.25rem',
                    borderRadius: '8px',
                    textDecoration: 'none',
                    boxShadow: '0 4px 6px -1px rgba(79, 70, 229, 0.25)',
                    transition: 'background 0.2s'
                  }}
                >
                  Pick this Car
                </a>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
