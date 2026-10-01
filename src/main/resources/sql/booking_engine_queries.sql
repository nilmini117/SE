-- ============================================================================
-- DRIVEFLOW CAR RENTAL SYSTEM
-- Customer Booking Engine: Underlying Validation & Business Logic Queries
-- Module: "Pick your choice in our park"
-- ============================================================================

USE driveflow_assignment_db;
GO

-- ----------------------------------------------------------------------------
-- 1. BRANCH-MANDATED VEHICLE AVAILABILITY QUERY
-- Validates that the vehicle is stationed at the user-selected pickup branch
-- and has zero overlapping active bookings for the desired rental date window.
-- ----------------------------------------------------------------------------
-- Parameters:
--   @PickupBranchId BIGINT = 1
--   @StartDate      DATE   = '2026-10-05'
--   @EndDate        DATE   = '2026-10-08'

SELECT 
    v.vehicle_id,
    v.reg_no,
    v.model,
    v.color,
    v.mileage,
    v.status,
    b.branch_id,
    b.branch_name,
    b.city
FROM vehicle v
INNER JOIN branch b ON v.branch_id = b.branch_id
WHERE v.branch_id = 1 -- Mandated Pickup Branch
  AND UPPER(v.status) = 'AVAILABLE'
  AND v.vehicle_id NOT IN (
      SELECT bk.vehicle_id 
      FROM booking bk 
      WHERE bk.vehicle_id = v.vehicle_id 
        AND UPPER(bk.status) IN ('PENDING', 'CONFIRMED', 'APPROVED')
        AND NOT (bk.end_date < '2026-10-05' OR bk.booking_date > '2026-10-08')
  )
ORDER BY v.model ASC;
GO


-- ----------------------------------------------------------------------------
-- 2. STRICT CONCURRENCY LIMIT QUERY
-- Verifies that the customer has EXACTLY 0 active bookings.
-- If active_booking_count > 0, the booking transaction is BLOCKED.
-- ----------------------------------------------------------------------------
-- Parameters:
--   @CustomerId BIGINT = 6

SELECT 
    COUNT(*) AS active_booking_count
FROM booking
WHERE customer_id = 6
  AND UPPER(status) IN ('PENDING', 'CONFIRMED', 'APPROVED');
GO

-- Diagnostic query: Retrieve details of any active conflicting booking
SELECT 
    booking_id,
    booking_date,
    end_date,
    status,
    charged_rate,
    pickup_branch_id,
    vehicle_id
FROM booking
WHERE customer_id = 6
  AND UPPER(status) IN ('PENDING', 'CONFIRMED', 'APPROVED');
GO


-- ----------------------------------------------------------------------------
-- 3. PRICING ENGINE: ACTIVE SEASONAL PROMOTIONS & COUPON LOOKUP
-- Fetches active marketing campaigns and matches coupon codes for discount calculation.
-- ----------------------------------------------------------------------------
-- Parameters:
--   @CurrentDate DATE = CAST(GETDATE() AS DATE)
--   @CouponCode  VARCHAR(50) = 'SUMMER15'

-- 3A. Active seasonal promotions valid for the current date
SELECT 
    promotion_id,
    title,
    coupon_code,
    discount_rate,
    start_date,
    end_date,
    status
FROM promotion
WHERE UPPER(status) = 'ACTIVE'
  AND (start_date IS NULL OR start_date <= CAST(GETDATE() AS DATE))
  AND (end_date IS NULL OR end_date >= CAST(GETDATE() AS DATE))
ORDER BY discount_rate DESC;
GO

-- 3B. Coupon-specific promotion matching
SELECT 
    promotion_id,
    title,
    coupon_code,
    discount_rate,
    status
FROM promotion
WHERE UPPER(status) = 'ACTIVE'
  AND UPPER(coupon_code) = UPPER('SUMMER15');
GO


-- ----------------------------------------------------------------------------
-- 4. IMMUTABILITY ENFORCEMENT TRIGGER (OPTIONAL DDL LEVEL SAFEGUARD)
-- Locks booking records against customer-initiated modifications to dates,
-- vehicle allocation, and pickup/return branches once created.
-- ----------------------------------------------------------------------------
IF OBJECT_ID('trg_EnforceBookingImmutability', 'TR') IS NOT NULL
    DROP TRIGGER trg_EnforceBookingImmutability;
GO

CREATE TRIGGER trg_EnforceBookingImmutability
ON booking
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Prohibit alterations to booking_date, end_date, vehicle_id, pickup_branch_id, or return_branch_id
    IF UPDATE(booking_date) OR UPDATE(end_date) OR UPDATE(vehicle_id) 
       OR UPDATE(pickup_branch_id) OR UPDATE(return_branch_id)
    BEGIN
        -- If update was initiated and status is not staff admin system override
        IF EXISTS (
            SELECT 1 
            FROM inserted i
            JOIN deleted d ON i.booking_id = d.booking_id
            WHERE (i.booking_date <> d.booking_date
               OR i.end_date <> d.end_date
               OR i.vehicle_id <> d.vehicle_id
               OR i.pickup_branch_id <> d.pickup_branch_id
               OR i.return_branch_id <> d.return_branch_id)
              AND d.status IN ('PENDING', 'CONFIRMED', 'APPROVED')
        )
        BEGIN
            RAISERROR('Booking Immutability Violation: Booking records cannot be altered once created. Dates, vehicles, and branches are strictly locked.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END
    END
END;
GO


-- ----------------------------------------------------------------------------
-- 5. CANCELLATION FLOW STATE CHECK QUERY
-- State check before processing customer cancellation.
-- Only allows cancellation if status = 'PENDING'. If APPROVED/CONFIRMED, cancels are blocked.
-- ----------------------------------------------------------------------------
-- Parameters:
--   @BookingId  BIGINT = 1
--   @CustomerId BIGINT = 6

SELECT 
    b.booking_id,
    b.status,
    b.customer_id,
    CASE 
        WHEN UPPER(b.status) = 'PENDING' THEN 'CANCELLATION_ALLOWED'
        WHEN UPPER(b.status) IN ('CONFIRMED', 'APPROVED') THEN 'CANCELLATION_DISABLED_APPROVED'
        ELSE 'CANCELLATION_DISABLED_INACTIVE'
    END AS cancellation_eligibility
FROM booking b
WHERE b.booking_id = 1
  AND b.customer_id = 6;
GO

-- Safe atomic cancellation execution for PENDING bookings only:
UPDATE booking
SET status = 'CANCELLED',
    staff_message = 'Cancelled by customer prior to staff approval.'
WHERE booking_id = 1
  AND customer_id = 6
  AND UPPER(status) = 'PENDING';
GO


-- ----------------------------------------------------------------------------
-- 6. PAYMENT GATE UNLOCKING ON STAFF APPROVAL
-- Generates and unlocks UNPAID invoice when booking transitions to CONFIRMED/APPROVED.
-- ----------------------------------------------------------------------------
-- Step A: Staff approves booking
UPDATE booking
SET status = 'CONFIRMED',
    staff_message = 'Your booking has been approved by our team. Payment gate is now unlocked.'
WHERE booking_id = 1
  AND UPPER(status) = 'PENDING';
GO

-- Step B: Insert or unlock invoice for customer payment
IF NOT EXISTS (SELECT 1 FROM invoice WHERE booking_id = 1)
BEGIN
    INSERT INTO invoice (invoice_date, rental_amt, late_fee, status, booking_id)
    SELECT 
        CAST(GETDATE() AS DATE),
        b.charged_rate,
        0.00,
        'UNPAID',
        b.booking_id
    FROM booking b
    WHERE b.booking_id = 1;
END
GO
