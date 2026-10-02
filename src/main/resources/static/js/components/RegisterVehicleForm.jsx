import React, { useState } from 'react';

/**
 * RegisterVehicleForm Component
 * Refactored vehicle registration form enforcing:
 * 1. Brand Field: Mandatory dropdown with Toyota, Suzuki, Honda, Tesla, Benz.
 * 2. Operational Status Constraint: Locked strictly to "AVAILABLE" in registration mode.
 * 3. Quantity Field Enforcement: Disabled & locked to 1; helper text completely removed.
 */
const RegisterVehicleForm = ({ branches = [], onSuccess, onCancel }) => {
    const BRANDS = ['Toyota', 'Suzuki', 'Honda', 'Tesla', 'Benz'];

    const [formData, setFormData] = useState({
        brand: '',
        model: '',
        regNo: '',
        color: '',
        mileage: 0,
        status: 'AVAILABLE', // Strictly locked to AVAILABLE on create
        quantity: 1,         // Strictly locked to 1 on create
        branchId: branches.length > 0 ? branches[0].branchId : ''
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);

    const handleChange = (e) => {
        const { name, value } = e.target;
        // Prevent manual modification of status and quantity in registration mode
        if (name === 'status' || name === 'quantity') {
            return;
        }
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccessMessage(null);

        if (!formData.brand) {
            setError('Please select a valid Vehicle Brand from the dropdown.');
            return;
        }
        if (!formData.regNo.trim()) {
            setError('Registration number is required.');
            return;
        }
        if (!formData.model.trim()) {
            setError('Vehicle make/model is required.');
            return;
        }

        setLoading(true);
        try {
            // Payload sent to backend; backend service layer also unconditionally enforces status=AVAILABLE and quantity=1
            const payload = {
                brand: formData.brand,
                model: formData.model.trim(),
                regNo: formData.regNo.trim().toUpperCase(),
                color: formData.color.trim(),
                mileage: parseInt(formData.mileage, 10) || 0,
                status: 'AVAILABLE',
                quantity: 1,
                branchId: formData.branchId ? parseInt(formData.branchId, 10) : null
            };

            const response = await fetch('/vehicles/api/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || 'Failed to register vehicle.');
            }

            const savedVehicle = await response.json();
            setSuccessMessage(`Vehicle ${savedVehicle.model || formData.model} registered successfully with status AVAILABLE!`);

            if (onSuccess) {
                onSuccess(savedVehicle);
            }
        } catch (err) {
            setError(err.message || 'An error occurred while registering the vehicle.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="df-form-card" style={{ maxWidth: '680px', margin: '0 auto', padding: '2rem' }}>
            <div style={{ marginBottom: '1.5rem' }}>
                <span className="badge badge-info" style={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Fleet Management
                </span>
                <h2 style={{ fontSize: '1.6rem', fontWeight: 800, marginTop: '0.5rem', color: 'var(--slate-900)' }}>
                    Register New Vehicle
                </h2>
                <p style={{ color: 'var(--slate-500)', fontSize: '0.9rem' }}>
                    Register a new fleet asset. Status is automatically initialized to AVAILABLE, and quantity is fixed to 1.
                </p>
            </div>

            {error && (
                <div style={{
                    background: '#fef2f2',
                    border: '1px solid #fecaca',
                    color: '#991b1b',
                    padding: '0.85rem 1.25rem',
                    borderRadius: 'var(--radius-md)',
                    marginBottom: '1.5rem',
                    fontSize: '0.9rem'
                }}>
                    ⚠️ {error}
                </div>
            )}

            {successMessage && (
                <div style={{
                    background: '#f0fdf4',
                    border: '1px solid #bbf7d0',
                    color: '#166534',
                    padding: '0.85rem 1.25rem',
                    borderRadius: 'var(--radius-md)',
                    marginBottom: '1.5rem',
                    fontSize: '0.9rem'
                }}>
                    ✓ {successMessage}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                {/* Row 1: Brand & Model */}
                <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="react-brand" style={{ fontWeight: 600 }}>
                            Vehicle Brand <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <select
                            id="react-brand"
                            name="brand"
                            className="form-select"
                            value={formData.brand}
                            onChange={handleChange}
                            required
                        >
                            <option value="" disabled>-- Select Vehicle Brand --</option>
                            {BRANDS.map(brand => (
                                <option key={brand} value={brand}>{brand}</option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="react-model" style={{ fontWeight: 600 }}>
                            Vehicle Make / Model <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <input
                            type="text"
                            id="react-model"
                            name="model"
                            className="form-control"
                            placeholder="e.g. Prius 2024 or RAV4 Prime"
                            value={formData.model}
                            onChange={handleChange}
                            required
                        />
                    </div>
                </div>

                {/* Row 2: Reg No & Color */}
                <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="react-regNo" style={{ fontWeight: 600 }}>
                            Registration Number <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <input
                            type="text"
                            id="react-regNo"
                            name="regNo"
                            className="form-control"
                            placeholder="e.g. WP CA-8942"
                            value={formData.regNo}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="react-color" style={{ fontWeight: 600 }}>
                            Vehicle Color <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <input
                            type="text"
                            id="react-color"
                            name="color"
                            className="form-control"
                            placeholder="e.g. Midnight Blue"
                            value={formData.color}
                            onChange={handleChange}
                            required
                        />
                    </div>
                </div>

                {/* Row 3: Mileage & Branch */}
                <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.25rem' }}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="react-mileage" style={{ fontWeight: 600 }}>
                            Current Mileage (km) <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <input
                            type="number"
                            id="react-mileage"
                            name="mileage"
                            className="form-control"
                            placeholder="0"
                            min="0"
                            value={formData.mileage}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="react-branchId" style={{ fontWeight: 600 }}>
                            Assigned Branch
                        </label>
                        <select
                            id="react-branchId"
                            name="branchId"
                            className="form-select"
                            value={formData.branchId}
                            onChange={handleChange}
                        >
                            <option value="">-- Select Branch --</option>
                            {branches.map(b => (
                                <option key={b.branchId} value={b.branchId}>
                                    {b.branchName} ({b.city})
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* Row 4: Operational Status (Locked) & Quantity (Locked to 1) */}
                <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="react-status" style={{ fontWeight: 600 }}>
                            Operational Status <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        {/* Locked strictly to AVAILABLE only in create mode */}
                        <select
                            id="react-status"
                            name="status"
                            className="form-select"
                            value="AVAILABLE"
                            disabled
                            style={{ backgroundColor: 'var(--slate-100)', cursor: 'not-allowed' }}
                        >
                            <option value="AVAILABLE">AVAILABLE — Ready for Rent</option>
                        </select>
                        <small style={{ color: 'var(--slate-500)', fontSize: '0.78rem', display: 'block', marginTop: '0.25rem' }}>
                            Initial registration status is locked to AVAILABLE.
                        </small>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="react-quantity" style={{ fontWeight: 600 }}>
                            Quantity
                        </label>
                        {/* Completely disabled and locked to 1; helper text removed */}
                        <input
                            type="number"
                            id="react-quantity"
                            name="quantity"
                            className="form-control"
                            value={1}
                            disabled
                            readOnly
                            style={{
                                backgroundColor: 'var(--slate-100)',
                                cursor: 'not-allowed',
                                fontWeight: 600,
                                color: 'var(--slate-700)'
                            }}
                        />
                    </div>
                </div>

                <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem' }}>
                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ flex: 1, padding: '0.75rem' }}
                        disabled={loading}
                    >
                        {loading ? 'Saving Vehicle...' : 'Register Vehicle'}
                    </button>
                    {onCancel && (
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={onCancel}
                            disabled={loading}
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
};

export default RegisterVehicleForm;
