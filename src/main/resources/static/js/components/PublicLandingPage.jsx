import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * DriveFlow Unified Public & Customer Landing Page (Dashboard) - Light Theme
 * 
 * Features:
 * 1. UI Unification & Light Theme:
 *    - Transitioned from dark navy to clean, bright light aesthetic (#ffffff / #f8fafc)
 *    - High-contrast typography with dark slate/navy text (#0f172a / #334155)
 *    - Distinct green DriveFlow monogram logo in navigation bar
 * 2. Layout Parity:
 *    - Search bar, brand filters, and featured fleet grid are 100% IDENTICAL for both
 *      public visitors and authenticated customers
 *    - The only dynamic UI change is top-right navigation:
 *      * Guest: "Sign In" and "Register" buttons
 *      * Authenticated: Verified customer avatar pill badge and "Sign Out"
 * 3. Interactive Vehicle Cards ("Click-to-Book"):
 *    - Car images act as interactive triggers with hover zoom & "Click to Reserve" pill
 *    - Clicking the image routes user immediately to /booking (or /bookings/new)
 * 4. Data Transfer & Autofill Logic:
 *    - Passes full context (vehicle_id, model, branch, daily_rate, transmission, seats, fuel, image)
 *      through React Router state: navigate('/booking', { state: { selectedVehicle: carData } })
 *    - Also writes to sessionStorage for browser parity
 */
export default function PublicLandingPage({
  currentUser = null, // e.g. { name: 'John Doe', email: 'john@driveflow.com', role: 'CUSTOMER' }
  initialVehicles = null,
  onSelectVehicle = null
}) {
  // Navigation hook (safe fallback if outside BrowserRouter)
  let navigate;
  try {
    navigate = useNavigate();
  } catch (e) {
    navigate = (path, options) => {
      if (options?.state?.selectedVehicle) {
        try {
          sessionStorage.setItem('selectedVehicle', JSON.stringify(options.state.selectedVehicle));
        } catch (err) {}
      }
      window.location.href = path;
    };
  }

  // Active navigation tab
  const [activeTab, setActiveTab] = useState('Dashboard');

  // Filter States
  const [selectedBrand, setSelectedBrand] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [availability, setAvailability] = useState('AVAILABLE');

  // Favorites state
  const [favorites, setFavorites] = useState({});

  // Corporate Lease Modal state
  const [isSalesModalOpen, setIsSalesModalOpen] = useState(false);

  // Toggle favorite
  const toggleFavorite = (vehicleId) => {
    setFavorites((prev) => ({
      ...prev,
      [vehicleId]: !prev[vehicleId],
    }));
  };

  // 5 Certified Park Manufacturer Brands with Authentic Vector Logos
  const brands = [
    { name: 'Toyota', logo: '/images/brand_toyota.png', color: '#fee2e2', textColor: '#dc2626' },
    { name: 'Suzuki', logo: '/images/brand_suzuki.png', color: '#dbeafe', textColor: '#2563eb' },
    { name: 'Honda', logo: '/images/brand_honda.png', color: '#fef3c7', textColor: '#d97706' },
    { name: 'Tesla', logo: '/images/brand_tesla.png', color: '#e0e7ff', textColor: '#4338ca' },
    { name: 'Benz', logo: '/images/brand_benz.png', color: '#f1f5f9', textColor: '#0f172a' },
  ];

  // Authentic Sri Lankan Fleet Vehicles Dataset
  const defaultFleet = [
    {
      id: 1,
      vehicle_id: 1,
      model: 'Toyota Prius 2024 Prime',
      brand: 'Toyota',
      regNo: 'WP CA-1020',
      categoryTag: 'Hybrid Efficient',
      transmission: 'Automatic (CVT)',
      seats: 5,
      fuel: 'Hybrid 24 km/L',
      rate: 12500,
      daily_rate: 12500,
      branch: 'Colombo Central Station',
      status: 'AVAILABLE',
      image: '/images/category_economy.jpg',
    },
    {
      id: 2,
      vehicle_id: 2,
      model: 'Benz C-Class C200 AMG',
      brand: 'Benz',
      regNo: 'WP BC-5001',
      categoryTag: 'Executive Sedan',
      transmission: 'Automatic (9G)',
      seats: 5,
      fuel: 'Turbo Petrol',
      rate: 28000,
      daily_rate: 28000,
      branch: 'Airport Express Hub (Katunayake)',
      status: 'AVAILABLE',
      image: '/images/category_luxury.jpg',
    },
    {
      id: 5,
      vehicle_id: 5,
      model: 'Tesla Model 3 Dual Motor',
      brand: 'Tesla',
      regNo: 'WP TM-3001',
      categoryTag: '100% Electric EV',
      transmission: 'Single-Speed EV',
      seats: 5,
      fuel: 'Electric (490 km)',
      rate: 32000,
      daily_rate: 32000,
      branch: 'Colombo Central Station',
      status: 'AVAILABLE',
      image: '/images/car_tesla_model3.jpg',
    },
    {
      id: 4,
      vehicle_id: 4,
      model: 'Suzuki Swift Sport',
      brand: 'Suzuki',
      regNo: 'SP GA-4050',
      categoryTag: 'City Hatchback',
      transmission: 'Automatic',
      seats: 4,
      fuel: 'Smart Hybrid',
      rate: 8500,
      daily_rate: 8500,
      branch: 'Galle Coastal Office',
      status: 'AVAILABLE',
      image: '/images/car_suzuki_swift.jpg',
    },
    {
      id: 2,
      vehicle_id: 2,
      model: 'Honda Vezel e:HEV RS',
      brand: 'Honda',
      regNo: 'WP CB-2030',
      categoryTag: 'Urban Crossover',
      transmission: 'Automatic (e-CVT)',
      seats: 5,
      fuel: 'Hybrid i-MMD',
      rate: 14500,
      daily_rate: 14500,
      branch: 'Colombo Central Station',
      status: 'AVAILABLE',
      image: '/images/lifestyle_car.jpg',
    },
    {
      id: 16,
      vehicle_id: 16,
      model: 'Toyota Land Cruiser Prado',
      brand: 'Toyota',
      regNo: 'WP NC-3344',
      categoryTag: 'Adventure 4x4',
      transmission: 'Automatic 4WD',
      seats: 7,
      fuel: 'Diesel Turbo',
      rate: 35000,
      daily_rate: 35000,
      branch: 'Kandy Heritage Hub',
      status: 'AVAILABLE',
      image: '/images/category_suv.jpg',
    },
  ];

  const vehicles = initialVehicles || defaultFleet;

  // Filter fleet based on brand, search query, and availability
  const filteredFleet = useMemo(() => {
    return vehicles.filter((v) => {
      if (selectedBrand && v.brand.toLowerCase() !== selectedBrand.toLowerCase()) {
        return false;
      }
      if (availability !== 'ALL' && v.status.toUpperCase() !== availability.toUpperCase()) {
        return false;
      }
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesModel = v.model && v.model.toLowerCase().includes(q);
        const matchesReg = v.regNo && v.regNo.toLowerCase().includes(q);
        const matchesBranch = v.branch && v.branch.toLowerCase().includes(q);
        const matchesBrand = v.brand && v.brand.toLowerCase().includes(q);
        if (!matchesModel && !matchesReg && !matchesBranch && !matchesBrand) {
          return false;
        }
      }
      return true;
    });
  }, [vehicles, selectedBrand, availability, searchQuery]);

  // Click-to-Book handler passing full context via router state
  const handleCarClick = (car) => {
    const carData = {
      vehicle_id: car.vehicle_id || car.id,
      id: car.vehicle_id || car.id,
      model: car.model,
      branch: car.branch,
      daily_rate: car.daily_rate || car.rate,
      rate: car.daily_rate || car.rate,
      transmission: car.transmission,
      seats: car.seats,
      fuel: car.fuel,
      image: car.image,
    };

    // 1. SessionStorage storage for direct browser reload resilience
    try {
      sessionStorage.setItem('selectedVehicle', JSON.stringify(carData));
    } catch (e) {}

    // 2. Call optional external callback
    if (onSelectVehicle) {
      onSelectVehicle(carData);
    }

    // 3. Navigate with router state
    navigate('/booking', { state: { selectedVehicle: carData } });
  };

  return (
    <div style={{ fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif", backgroundColor: '#f8fafc', color: '#0f172a', minHeight: '100vh' }}>
      
      {/* ============================================================== */}
      {/* 1. UNIFIED LIGHT-THEMED TOP NAVIGATION BAR                     */}
      {/* ============================================================== */}
      <nav style={{
        position: 'sticky',
        top: 0,
        zIndex: 1000,
        backgroundColor: '#ffffff',
        borderBottom: '1px solid #e2e8f0',
        boxShadow: '0 2px 12px rgba(15, 23, 42, 0.05)',
        height: '72px',
        display: 'flex',
        alignItems: 'center',
      }}>
        <div style={{
          maxWidth: '1280px',
          width: '100%',
          margin: '0 auto',
          padding: '0 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}>
          {/* Logo */}
          <a href="/" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none' }}>
            <div style={{
              width: '42px',
              height: '42px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #15803d 0%, #16a34a 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 14px rgba(22, 163, 74, 0.35)',
            }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.3" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="9" />
                <circle cx="12" cy="12" r="3" />
                <path d="M12 3v6" />
                <path d="M12 15v6" />
                <path d="M3 12h6" />
                <path d="M15 12h6" />
              </svg>
            </div>
            <div style={{ fontSize: '1.4rem', fontWeight: 800, letterSpacing: '-0.03em', color: '#0f172a', lineHeight: 1 }}>
              Drive<span style={{ color: '#16a34a' }}>Flow</span>
            </div>
          </a>

          {/* Nav Links (Strict Layout Parity) */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.75rem' }}>
            {['Dashboard', 'Vehicles', 'Bookings', 'Feedback'].map((tab) => {
              const isActive = activeTab === tab;
              return (
                <a
                  key={tab}
                  href={tab === 'Dashboard' ? '/' : `/${tab.toLowerCase()}`}
                  onClick={(e) => {
                    if (tab === 'Dashboard') {
                      e.preventDefault();
                      setActiveTab(tab);
                    }
                  }}
                  style={{
                    color: isActive ? '#15803d' : '#475569',
                    textDecoration: 'none',
                    fontWeight: isActive ? 700 : 600,
                    fontSize: '0.95rem',
                    transition: 'all 0.15s ease',
                    position: 'relative',
                    padding: '0.35rem 0',
                    borderBottom: isActive ? '2.5px solid #16a34a' : '2.5px solid transparent',
                  }}
                >
                  {tab}
                </a>
              );
            })}

            <a
              href="/incident"
              style={{
                color: '#475569',
                textDecoration: 'none',
                fontWeight: 600,
                fontSize: '0.92rem',
                transition: 'all 0.15s ease',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
              }}
            >
              <span>Need to contact us?</span>
            </a>
          </div>

          {/* Dynamic Auth Actions: Only UI difference between guest and customer */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
            {currentUser ? (
              // Logged-in Customer View
              <>
                <a
                  href="/profile"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    padding: '0.35rem 0.85rem',
                    backgroundColor: '#f1f5f9',
                    border: '1px solid #e2e8f0',
                    borderRadius: '9999px',
                    fontSize: '0.85rem',
                    fontWeight: 600,
                    color: '#0f172a',
                    textDecoration: 'none',
                  }}
                >
                  <span style={{
                    width: '28px',
                    height: '28px',
                    borderRadius: '50%',
                    backgroundColor: '#16a34a',
                    color: '#ffffff',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                  }}>
                    {currentUser.name ? currentUser.name.charAt(0) : 'C'}
                  </span>
                  <span>{currentUser.name || 'Customer'}</span>
                </a>
                <a
                  href="/logout"
                  style={{
                    color: '#475569',
                    textDecoration: 'none',
                    fontWeight: 600,
                    fontSize: '0.85rem',
                    padding: '0.4rem 0.9rem',
                    borderRadius: '9999px',
                    border: '1px solid #e2e8f0',
                    backgroundColor: '#ffffff',
                  }}
                >
                  Sign Out
                </a>
              </>
            ) : (
              // Public Visitor View
              <>
                <a
                  href="/login"
                  style={{
                    color: '#334155',
                    textDecoration: 'none',
                    fontWeight: 600,
                    fontSize: '0.92rem',
                    padding: '0.5rem 1.15rem',
                    borderRadius: '9999px',
                    border: '1px solid #e2e8f0',
                    backgroundColor: '#ffffff',
                  }}
                >
                  Sign In
                </a>
                <a
                  href="/register"
                  style={{
                    backgroundColor: '#16a34a',
                    color: '#ffffff',
                    fontWeight: 700,
                    fontSize: '0.92rem',
                    padding: '0.6rem 1.35rem',
                    borderRadius: '9999px',
                    boxShadow: '0 4px 14px rgba(22, 163, 74, 0.35)',
                    textDecoration: 'none',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                  }}
                >
                  Register
                </a>
              </>
            )}
          </div>
        </div>
      </nav>

      {/* ============================================================== */}
      {/* 2. LIGHT-THEMED HERO SECTION & BRAND FILTERS                   */}
      {/* ============================================================== */}
      <header style={{
        backgroundImage: 'url(/images/lifestyle_car.jpg)',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
        position: 'relative',
        borderBottom: '1px solid #e2e8f0',
        padding: '3.5rem 1.5rem 4rem',
        color: '#0f172a',
        textAlign: 'center',
      }}>
        {/* White overlay with 60% opacity */}
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(255, 255, 255, 0.60)',
          zIndex: 1,
        }} />

        <div style={{ maxWidth: '1200px', margin: '0 auto', position: 'relative', zIndex: 10 }}>
          {/* Badge */}
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.35rem 0.95rem',
            backgroundColor: '#ecfdf5',
            border: '1px solid #bbf7d0',
            color: '#15803d',
            borderRadius: '9999px',
            fontSize: '0.78rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
            marginBottom: '1.25rem',
          }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2" />
              <circle cx="7" cy="17" r="2" />
              <path d="M9 17h6" />
              <circle cx="17" cy="17" r="2" />
            </svg>
            Certified Sri Lankan Park Inventory
          </div>

          {/* Title & Subtext */}
          <h1 style={{
            fontSize: 'clamp(2.2rem, 4.5vw, 3.4rem)',
            fontWeight: 800,
            letterSpacing: '-0.03em',
            margin: '0 0 0.85rem',
            color: '#0f172a',
            lineHeight: 1.15,
          }}>
            Find your choice in our park
          </h1>
          <p style={{
            fontSize: 'clamp(1.05rem, 2vw, 1.25rem)',
            color: '#0f172a',
            fontWeight: 600,
            maxWidth: '720px',
            margin: '0 auto 2.5rem',
            lineHeight: 1.6,
          }}>
            Safe drive makes happy journey
          </p>

          {/* Horizontal Row of 5 Brand Filter Cards (Light Theme) */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '1.25rem',
            maxWidth: '1100px',
            margin: '0 auto 2.5rem',
          }}>
            {brands.map((b) => {
              const isSelected = selectedBrand && selectedBrand.toLowerCase() === b.name.toLowerCase();
              return (
                <div
                  key={b.name}
                  onClick={() => setSelectedBrand(isSelected ? null : b.name)}
                  style={{
                    backgroundColor: isSelected ? '#ecfdf5' : '#ffffff',
                    border: isSelected ? '2px solid #16a34a' : '2px solid #e2e8f0',
                    boxShadow: isSelected ? '0 0 0 3px rgba(22, 163, 74, 0.2), 0 8px 20px -4px rgba(22, 163, 74, 0.2)' : '0 2px 6px rgba(15, 23, 42, 0.04)',
                    borderRadius: '16px',
                    padding: '1.35rem 1rem',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.65rem',
                    cursor: 'pointer',
                    transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                    userSelect: 'none',
                  }}
                >
                  <div style={{
                    width: '54px',
                    height: '54px',
                    borderRadius: '50%',
                    backgroundColor: '#ffffff',
                    border: '1.5px solid #e2e8f0',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '8px',
                    boxShadow: '0 2px 6px rgba(0, 0, 0, 0.04)',
                  }}>
                    <img
                      src={b.logo}
                      alt={b.name}
                      style={{
                        width: '32px',
                        height: '32px',
                        objectFit: 'contain',
                        display: 'block',
                      }}
                    />
                  </div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 800, color: '#0f172a', margin: 0 }}>
                    {b.name}
                  </div>
                  <div style={{
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    padding: '0.25rem 0.75rem',
                    borderRadius: '9999px',
                    backgroundColor: isSelected ? '#16a34a' : '#f1f5f9',
                    color: isSelected ? '#ffffff' : '#475569',
                    transition: 'all 0.2s ease',
                  }}>
                    {isSelected ? 'Selected ✓' : 'View Cars →'}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Pill-Shaped Search Bar (Light Theme) */}
          <div style={{ maxWidth: '920px', margin: '0 auto' }}>
            <div style={{
              backgroundColor: '#ffffff',
              borderRadius: '9999px',
              padding: '0.6rem 0.75rem',
              border: '1px solid #e2e8f0',
              boxShadow: '0 12px 32px -6px rgba(15, 23, 42, 0.08)',
              display: 'flex',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '0.5rem',
            }}>
              {/* Input */}
              <div style={{
                flex: '2 1 280px',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.5rem 1.25rem',
                borderRight: '1px solid #e2e8f0',
              }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2.3">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search by model or reg number (e.g. Prius, WP CA)..."
                  style={{
                    width: '100%',
                    border: 'none',
                    outline: 'none',
                    fontFamily: 'inherit',
                    fontSize: '0.95rem',
                    fontWeight: 600,
                    color: '#0f172a',
                    backgroundColor: 'transparent',
                  }}
                />
              </div>

              {/* Select */}
              <div style={{
                flex: '1 1 200px',
                display: 'flex',
                alignItems: 'center',
                gap: '0.65rem',
                padding: '0.5rem 1rem',
              }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2.3">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
                <select
                  value={availability}
                  onChange={(e) => setAvailability(e.target.value)}
                  style={{
                    width: '100%',
                    border: 'none',
                    outline: 'none',
                    fontFamily: 'inherit',
                    fontSize: '0.92rem',
                    fontWeight: 700,
                    color: '#0f172a',
                    backgroundColor: 'transparent',
                    cursor: 'pointer',
                  }}
                >
                  <option value="ALL">All Availability</option>
                  <option value="AVAILABLE">Available Only</option>
                  <option value="BOOKED">Booked</option>
                </select>
              </div>

              {/* Filter Button */}
              <button
                type="button"
                onClick={() => {}}
                style={{
                  backgroundColor: '#16a34a',
                  color: '#ffffff',
                  border: 'none',
                  padding: '0.85rem 1.85rem',
                  borderRadius: '9999px',
                  fontFamily: 'inherit',
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  boxShadow: '0 4px 14px rgba(22, 163, 74, 0.35)',
                  whiteSpace: 'nowrap',
                }}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
                </svg>
                Filter Fleet
              </button>
            </div>

            {/* Active Filter Pill */}
            {selectedBrand && (
              <div style={{
                marginTop: '1rem',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.75rem',
                backgroundColor: '#ffffff',
                border: '1px solid #bbf7d0',
                padding: '0.4rem 1rem',
                borderRadius: '9999px',
                fontSize: '0.85rem',
                color: '#334155',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
              }}>
                <span>Filtering by: <strong style={{ color: '#15803d' }}>{selectedBrand}</strong></span>
                <button
                  type="button"
                  onClick={() => setSelectedBrand(null)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#15803d',
                    cursor: 'pointer',
                    fontWeight: 700,
                    fontSize: '0.82rem',
                    textDecoration: 'underline',
                    padding: 0,
                    fontFamily: 'inherit',
                  }}
                >
                  Clear Filter &bull; Show All
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* ============================================================== */}
      {/* 3. FEATURED FLEET (CLICK-TO-BOOK CARD GRID)                    */}
      {/* ============================================================== */}
      <section style={{ padding: '4.5rem 1.5rem', maxWidth: '1280px', margin: '0 auto' }}>
        <div style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          marginBottom: '2.25rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}>
          <div>
            <h2 style={{ fontSize: '1.95rem', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.025em', margin: '0 0 0.35rem' }}>
              Latest additions to our fleet
            </h2>
            <p style={{ color: '#64748b', fontSize: '0.95rem', margin: 0 }}>
              Certified, safety-inspected passenger cars. Click any vehicle image or "Book Now" to automatically reserve.
            </p>
          </div>
          <a
            href="/vehicles"
            style={{
              color: '#15803d',
              fontWeight: 700,
              fontSize: '0.95rem',
              textDecoration: 'none',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            Browse all fleet vehicles &rarr;
          </a>
        </div>

        {/* 3-Column Vehicle Card Grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '1.75rem',
        }}>
          {filteredFleet.map((car) => {
            const isFav = !!favorites[car.id];
            return (
              <div
                key={car.id}
                style={{
                  backgroundColor: '#ffffff',
                  borderRadius: '18px',
                  border: '1px solid #e2e8f0',
                  overflow: 'hidden',
                  boxShadow: '0 4px 18px rgba(15, 23, 42, 0.05)',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'all 0.25s ease',
                }}
              >
                {/* Interactive Clickable Vehicle Image Trigger */}
                <div
                  onClick={() => handleCarClick(car)}
                  title="Click image to reserve this vehicle"
                  style={{
                    position: 'relative',
                    width: '100%',
                    height: '220px',
                    overflow: 'hidden',
                    backgroundColor: '#f1f5f9',
                    cursor: 'pointer',
                  }}
                >
                  <img
                    src={car.image}
                    alt={car.model}
                    style={{ width: '100%', height: '100%', objectFit: 'cover', transition: 'transform 0.4s ease' }}
                  />
                  
                  {/* Click-to-Reserve Hover Badge */}
                  <div style={{
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    transform: 'translate(-50%, -50%)',
                    backgroundColor: 'rgba(15, 23, 42, 0.85)',
                    backdropFilter: 'blur(8px)',
                    color: '#ffffff',
                    fontSize: '0.82rem',
                    fontWeight: 700,
                    padding: '0.5rem 1rem',
                    borderRadius: '9999px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.4rem',
                    boxShadow: '0 8px 20px rgba(0, 0, 0, 0.3)',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                    whiteSpace: 'nowrap',
                  }}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polyline></svg>
                    Click to Reserve
                  </div>

                  {/* Heart / Favorite Button */}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleFavorite(car.id);
                    }}
                    style={{
                      position: 'absolute',
                      top: '14px',
                      right: '14px',
                      width: '38px',
                      height: '38px',
                      borderRadius: '50%',
                      backgroundColor: 'rgba(255, 255, 255, 0.92)',
                      backdropFilter: 'blur(8px)',
                      border: '1px solid rgba(255, 255, 255, 0.8)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer',
                      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                      zIndex: 10,
                    }}
                    title="Toggle Favorite"
                  >
                    <svg
                      width="18"
                      height="18"
                      viewBox="0 0 24 24"
                      fill={isFav ? '#ef4444' : 'none'}
                      stroke={isFav ? '#ef4444' : '#64748b'}
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                  </button>

                  <div style={{
                    position: 'absolute',
                    bottom: '12px',
                    left: '14px',
                    backgroundColor: 'rgba(15, 23, 42, 0.82)',
                    backdropFilter: 'blur(8px)',
                    color: '#ffffff',
                    fontSize: '0.72rem',
                    fontWeight: 700,
                    padding: '0.25rem 0.65rem',
                    borderRadius: '6px',
                    textTransform: 'uppercase',
                    zIndex: 5,
                  }}>
                    {car.categoryTag}
                  </div>
                </div>

                {/* Card Body */}
                <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', flex: 1 }}>
                  {/* Branch Location */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.82rem', fontWeight: 600, color: '#64748b', marginBottom: '0.45rem' }}>
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                      <circle cx="12" cy="10" r="3" />
                    </svg>
                    {car.branch}
                  </div>

                  {/* Model Name */}
                  <h3
                    onClick={() => handleCarClick(car)}
                    style={{ fontSize: '1.22rem', fontWeight: 800, color: '#0f172a', margin: '0 0 1rem', lineHeight: 1.25, cursor: 'pointer' }}
                  >
                    {car.model}
                  </h3>

                  {/* Vehicle Specs */}
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(3, 1fr)',
                    gap: '0.5rem',
                    padding: '0.75rem 0',
                    borderTop: '1px solid #f1f5f9',
                    borderBottom: '1px solid #f1f5f9',
                    marginBottom: '1.25rem',
                  }}>
                    <div>
                      <span style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: '#94a3b8' }}>Transmission</span>
                      <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#334155' }}>{car.transmission}</span>
                    </div>
                    <div>
                      <span style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: '#94a3b8' }}>Capacity</span>
                      <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#334155' }}>{car.seats} Seats</span>
                    </div>
                    <div>
                      <span style={{ display: 'block', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: '#94a3b8' }}>Engine / Fuel</span>
                      <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#334155' }}>{car.fuel}</span>
                    </div>
                  </div>

                  {/* Pricing & CTA */}
                  <div style={{ marginTop: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', paddingTop: '0.5rem' }}>
                    <div>
                      <span style={{ display: 'block', fontSize: '0.72rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>Daily Rental</span>
                      <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#0f172a' }}>
                        Rs. {Number(car.daily_rate || car.rate).toLocaleString()} <small style={{ fontSize: '0.8rem', fontWeight: 600, color: '#64748b' }}>/day</small>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleCarClick(car)}
                      style={{
                        backgroundColor: '#16a34a',
                        color: '#ffffff',
                        fontWeight: 700,
                        fontSize: '0.88rem',
                        padding: '0.65rem 1.25rem',
                        borderRadius: '9999px',
                        border: 'none',
                        cursor: 'pointer',
                        boxShadow: '0 4px 12px rgba(22, 163, 74, 0.3)',
                      }}
                    >
                      Book Now
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Empty state */}
        {filteredFleet.length === 0 && (
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '16px',
            border: '2px dashed #cbd5e1',
            padding: '3.5rem 1.5rem',
            textAlign: 'center',
            color: '#64748b',
            marginTop: '2rem',
          }}>
            <h3 style={{ color: '#0f172a', fontSize: '1.25rem', fontWeight: 700, margin: '0 0 0.5rem' }}>No vehicles match your filter</h3>
            <p style={{ margin: '0 0 1.25rem' }}>Try clearing the manufacturer filter or search keyword.</p>
            <button
              type="button"
              onClick={() => { setSelectedBrand(null); setSearchQuery(''); setAvailability('ALL'); }}
              style={{
                backgroundColor: '#16a34a',
                color: '#ffffff',
                border: 'none',
                padding: '0.65rem 1.5rem',
                borderRadius: '9999px',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Reset Filters &bull; View All
            </button>
          </div>
        )}
      </section>

      {/* ============================================================== */}
      {/* 4. "HOW BOOKING WORKS" SECTION (CAR RENTAL WORKFLOW)          */}
      {/* ============================================================== */}
      <section style={{
        backgroundColor: '#ffffff',
        borderTop: '1px solid #e2e8f0',
        borderBottom: '1px solid #e2e8f0',
        padding: '5rem 1.5rem',
      }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '3.5rem', alignItems: 'center' }}>
            {/* Visual on left */}
            <div style={{ position: 'relative', borderRadius: '20px', overflow: 'hidden', boxShadow: '0 20px 40px -10px rgba(15, 23, 42, 0.12)' }}>
              <img
                src="/images/lifestyle_car.jpg"
                alt="DriveFlow vehicle fleet Sri Lanka"
                style={{ width: '100%', height: '100%', maxHeight: '480px', objectFit: 'cover', display: 'block' }}
              />
              <div style={{
                position: 'absolute',
                bottom: '20px',
                left: '20px',
                backgroundColor: 'rgba(15, 23, 42, 0.88)',
                backdropFilter: 'blur(10px)',
                color: '#ffffff',
                padding: '0.85rem 1.35rem',
                borderRadius: '14px',
                display: 'flex',
                alignItems: 'center',
                gap: '0.85rem',
              }}>
                <div style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: '#22c55e' }} />
                <div>
                  <strong style={{ fontSize: '0.95rem', display: 'block', color: '#ffffff' }}>100% Inspected Fleet</strong>
                  <span style={{ fontSize: '0.78rem', color: '#cbd5e1' }}>Zero hidden fees &bull; 24/7 Islandwide Assistance</span>
                </div>
              </div>
            </div>

            {/* Workflow on right */}
            <div>
              <div style={{ fontSize: '0.85rem', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.08em', color: '#16a34a', marginBottom: '0.5rem' }}>
                Seamless Reservation Process
              </div>
              <h2 style={{ fontSize: 'clamp(1.85rem, 3.2vw, 2.5rem)', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.03em', margin: '0 0 1.25rem', lineHeight: 1.2 }}>
                How Booking Works
              </h2>
              <p style={{ color: '#64748b', fontSize: '1.05rem', lineHeight: 1.6, margin: '0 0 2rem' }}>
                DriveFlow makes renting a certified car in Sri Lanka effortless. From our central hubs to airport pickups, your journey starts with 3 simple steps:
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
                <div style={{ display: 'flex', gap: '1.25rem', alignItems: 'flex-start' }}>
                  <div style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '12px',
                    backgroundColor: '#ecfdf5',
                    color: '#15803d',
                    fontWeight: 800,
                    fontSize: '1.1rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    border: '1px solid #bbf7d0',
                  }}>
                    1
                  </div>
                  <div>
                    <h4 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.35rem' }}>
                      Browse the Fleet
                    </h4>
                    <p style={{ fontSize: '0.92rem', color: '#64748b', margin: 0, lineHeight: 1.5 }}>
                      Filter our certified park inventory by brand, branch, or vehicle type to select the model that suits your journey.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '1.25rem', alignItems: 'flex-start' }}>
                  <div style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '12px',
                    backgroundColor: '#ecfdf5',
                    color: '#15803d',
                    fontWeight: 800,
                    fontSize: '1.1rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    border: '1px solid #bbf7d0',
                  }}>
                    2
                  </div>
                  <div>
                    <h4 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.35rem' }}>
                      Reserve Securely
                    </h4>
                    <p style={{ fontSize: '0.92rem', color: '#64748b', margin: 0, lineHeight: 1.5 }}>
                      Lock in your dates and receive an instant booking confirmation via email with transparent pricing in Sri Lankan Rupees.
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '1.25rem', alignItems: 'flex-start' }}>
                  <div style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '12px',
                    backgroundColor: '#ecfdf5',
                    color: '#15803d',
                    fontWeight: 800,
                    fontSize: '1.1rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    border: '1px solid #bbf7d0',
                  }}>
                    3
                  </div>
                  <div>
                    <h4 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#0f172a', margin: '0 0 0.35rem' }}>
                      Pick Up &amp; Drive
                    </h4>
                    <p style={{ fontSize: '0.92rem', color: '#64748b', margin: 0, lineHeight: 1.5 }}>
                      Collect your keys from the designated branch and enjoy your journey across scenic Sri Lankan highways with 24/7 support.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 5. FOOTER & CORPORATE LEASE BANNER                            */}
      {/* ============================================================== */}
      <section style={{
        backgroundImage: 'linear-gradient(135deg, rgba(15, 23, 42, 0.9) 0%, rgba(30, 41, 59, 0.95) 100%), url("/images/corporate_lease.jpg")',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        color: '#ffffff',
        padding: '4.5rem 1.5rem',
        textAlign: 'center',
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
      }}>
        <div style={{ maxWidth: '820px', margin: '0 auto' }}>
          <h2 style={{ fontSize: 'clamp(1.85rem, 3.5vw, 2.6rem)', fontWeight: 800, letterSpacing: '-0.025em', margin: '0 0 1rem' }}>
            Need a vehicle for a long-term corporate lease?
          </h2>
          <p style={{ fontSize: '1.05rem', color: '#cbd5e1', margin: '0 auto 2rem', maxWidth: '650px', lineHeight: 1.6 }}>
            Custom monthly packages, executive sedans, dedicated fleet maintenance, and priority replacement vehicles for Sri Lankan businesses.
          </p>
          <button
            type="button"
            onClick={() => { window.location.href = '/incident'; }}
            style={{
              backgroundColor: '#ffffff',
              color: '#0f172a',
              fontWeight: 800,
              fontSize: '0.95rem',
              padding: '0.85rem 2rem',
              borderRadius: '9999px',
              border: 'none',
              cursor: 'pointer',
              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.25)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            Contact Sales
          </button>
        </div>
      </section>

      {/* Standard Footer (Light Theme) */}
      <footer style={{ backgroundColor: '#ffffff', borderTop: '1px solid #e2e8f0', color: '#64748b', padding: '4.5rem 1.5rem 2.5rem', fontSize: '0.9rem' }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '3rem',
            marginBottom: '3.5rem',
          }}>
            <div>
              <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#0f172a', marginBottom: '1rem' }}>
                Drive<span style={{ color: '#16a34a' }}>Flow</span>
              </div>
              <p style={{ lineHeight: 1.6, fontSize: '0.88rem' }}>
                Sri Lanka's trusted digital car rental network. Providing certified passenger sedans, eco hybrids, and 4x4 adventure vehicles with transparent daily rates and 24/7 roadside assistance.
              </p>
            </div>
            <div>
              <h4 style={{ color: '#0f172a', fontSize: '0.95rem', fontWeight: 700, margin: '0 0 1.25rem' }}>Explore</h4>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <li><a href="/vehicles" style={{ color: '#64748b', textDecoration: 'none' }}>Browse All Fleet</a></li>
                <li><a href="/bookings/new" style={{ color: '#64748b', textDecoration: 'none' }}>Make a Reservation</a></li>
                <li><a href="/promotions" style={{ color: '#64748b', textDecoration: 'none' }}>Seasonal Offers</a></li>
                <li><a href="/feedback" style={{ color: '#64748b', textDecoration: 'none' }}>Customer Reviews</a></li>
              </ul>
            </div>
            <div>
              <h4 style={{ color: '#0f172a', fontSize: '0.95rem', fontWeight: 700, margin: '0 0 1.25rem' }}>Account</h4>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <li><a href="/login" style={{ color: '#64748b', textDecoration: 'none' }}>Customer Sign In</a></li>
                <li><a href="/register" style={{ color: '#64748b', textDecoration: 'none' }}>Register Account</a></li>
                <li><a href="/profile" style={{ color: '#64748b', textDecoration: 'none' }}>My Profile &amp; KYC</a></li>
              </ul>
            </div>
            <div>
              <h4 style={{ color: '#0f172a', fontSize: '0.95rem', fontWeight: 700, margin: '0 0 1.25rem' }}>Help</h4>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <li><a href="/incidents/report" style={{ color: '#64748b', textDecoration: 'none' }}>24/7 Breakdown Assistance</a></li>
                <li><span style={{ color: '#334155' }}>Hotline: 011 234 5678</span></li>
                <li><span style={{ color: '#334155' }}>support@driveflow.com</span></li>
              </ul>
            </div>
          </div>

          <div style={{
            borderTop: '1px solid #e2e8f0',
            paddingTop: '2rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
            fontSize: '0.82rem',
          }}>
            <div>&copy; 2026 DriveFlow Car Rental (Pvt) Ltd. Sri Lanka.</div>
            <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
              <span>🇱🇰 SLTDA Registered</span>
              <strong style={{ color: '#16a34a' }}>Strictly Car Rental &bull; Rates in Sri Lankan Rupees (Rs.)</strong>
            </div>
          </div>
        </div>
      </footer>

    </div>
  );
}
