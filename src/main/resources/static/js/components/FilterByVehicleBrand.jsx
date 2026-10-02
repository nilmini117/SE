import React from 'react';

/**
 * FilterByVehicleBrand Component (React)
 * Module: "Filter by Vehicle Brand"
 * 
 * Features:
 * 1. UI Text Clean-up:
 *    - Header instructional subtext strictly reads:
 *      "Click a manufacturer to view the certified models stationed in our park."
 *    - Vehicle count subtitle text node is completely removed from all brand filter cards.
 * 2. Visual Balance:
 *    - BrandCard displays strictly:
 *      * Brand circular icon
 *      * Brand name heading (Toyota, Suzuki, Honda, Tesla, Benz)
 *      * Bottom action badge (View Cars → / Selected)
 *    - Clean, compact, and balanced vertical spacing.
 */
/**
 * BrandCard Component
 * Displays an individual brand filter card:
 * - Circular brand icon
 * - Brand name heading (Toyota, Suzuki, Honda, Tesla, Benz)
 * - Bottom action button or badge (View Cars → / Selected)
 * 
 * Note: Vehicle count subtitle text node is completely removed from DOM.
 */
export function BrandCard({ brand, isSelected, onSelect }) {
  return (
    <div
      data-testid={`brand-card-${brand.name.toLowerCase()}`}
      onClick={() => onSelect && onSelect(isSelected ? null : brand.name)}
      style={{
        cursor: 'pointer',
        padding: '1.25rem 1rem',
        borderRadius: '12px',
        border: isSelected ? '2px solid #4f46e5' : '2px solid #e2e8f0',
        background: isSelected ? '#eef2ff' : '#f8fafc',
        boxShadow: isSelected ? '0 4px 6px -1px rgba(79, 70, 229, 0.15)' : 'none',
        transition: 'all 0.2s ease',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        textAlign: 'center',
        gap: '0.65rem'
      }}
    >
      {/* 1. Circular Brand Icon */}
      <div
        style={{
          width: '48px',
          height: '48px',
          borderRadius: '50%',
          background: brand.color || '#f1f5f9',
          color: brand.textColor || '#0f172a',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 800,
          fontSize: '1.1rem'
        }}
      >
        {brand.emoji}
      </div>

      {/* 2. Brand Name Heading */}
      <strong style={{ fontSize: '1.05rem', color: '#0f172a', lineHeight: 1.2 }}>
        {brand.name}
      </strong>

      {/* 3. Bottom Action Button / Badge */}
      <span
        style={{
          fontSize: '0.72rem',
          padding: '0.2rem 0.65rem',
          borderRadius: '9999px',
          fontWeight: 700,
          background: isSelected ? '#4f46e5' : '#e2e8f0',
          color: isSelected ? '#ffffff' : '#334155',
          marginTop: '0.15rem'
        }}
      >
        {isSelected ? 'Selected' : 'View Cars →'}
      </span>
    </div>
  );
}

export default function FilterByVehicleBrand({
  vehicles = [],
  selectedBrand = null,
  onSelectBrand = null,
  status = 'AVAILABLE',
  brands = [
    { name: 'Toyota', emoji: '🔴', color: '#fee2e2', textColor: '#dc2626' },
    { name: 'Suzuki', emoji: '🔵', color: '#dbeafe', textColor: '#2563eb' },
    { name: 'Honda', emoji: '🟡', color: '#fef3c7', textColor: '#d97706' },
    { name: 'Tesla', emoji: '⚡', color: '#e0e7ff', textColor: '#4338ca' },
    { name: 'Benz', emoji: '⭐', color: '#f1f5f9', textColor: '#0f172a' }
  ]
}) {
  return (
    <div
      className="filter-by-vehicle-brand"
      style={{
        background: '#ffffff',
        border: '1px solid #e2e8f0',
        borderRadius: '16px',
        padding: '1.5rem',
        marginBottom: '2rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
      }}
    >
      {/* 1. Header & Instructional Subtext */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '1rem',
        flexWrap: 'wrap',
        gap: '0.5rem'
      }}>
        <div>
          <h3 style={{
            fontSize: '1.1rem',
            fontWeight: 700,
            color: '#0f172a',
            margin: 0,
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}>
            <span>🏎️</span>
            Filter by Vehicle Brand
          </h3>
          <p style={{ fontSize: '0.85rem', color: '#64748b', margin: '0.25rem 0 0 0' }}>
            Click a manufacturer to view the certified models stationed in our park.
          </p>
        </div>
        {selectedBrand && (
          <div>
            <button
              type="button"
              onClick={() => onSelectBrand && onSelectBrand(null)}
              style={{
                fontSize: '0.8rem',
                padding: '0.35rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                background: '#f8fafc',
                color: '#334155',
                cursor: 'pointer',
                fontWeight: 600
              }}
            >
              &larr; View All Brands
            </button>
          </div>
        )}
      </div>

      {/* 2. Brand Cards Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
        gap: '1rem'
      }}>
        {brands.map((b) => {
          const isSelected = selectedBrand && selectedBrand.toLowerCase() === b.name.toLowerCase();

          return (
            <BrandCard
              key={b.name}
              brand={b}
              isSelected={isSelected}
              onSelect={onSelectBrand}
            />
          );
        })}
      </div>
    </div>
  );
}

export { FilterByVehicleBrand };

