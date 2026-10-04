-- ===================================================================================
-- DriveFlow Car Rental System
-- Database Cleanup Script: Purge Users Preserving Strictly System IDs 12 & 22
--
-- Execution Order:
--   1. Clear Refunds (linked payments belonging to users NOT IN (12, 22))
--   2. Clear Payments (credit_card_pay, has_deposit, and payment records for users NOT IN (12, 22))
--   3. Clear Feedback (feedback linked to bookings or customers NOT IN (12, 22))
--   4. Clear Bookings (invoices, inspections, additional services, and bookings for users NOT IN (12, 22))
--   5. Clear Incidents (incidents for users NOT IN (12, 22))
--   6. Purge Users (contact numbers, customer, staff, and main users records for system_id NOT IN (12, 22))
-- ===================================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

-- -----------------------------------------------------------------------------------
-- Step 1: Clear Refunds
-- Delete all records from the refund table where the linked payment belongs to a user whose ID is NOT 12 or 22
-- -----------------------------------------------------------------------------------
DELETE FROM refund
WHERE payment_id IN (
    SELECT p.payment_id
    FROM payment p
    JOIN invoice i ON p.invoice_id = i.invoice_id
    JOIN booking b ON i.booking_id = b.booking_id
    WHERE b.customer_id NOT IN (12, 22)
);

-- Dependent payment child tables (credit_card_pay and has_deposit)
DELETE FROM credit_card_pay
WHERE payment_id IN (
    SELECT p.payment_id
    FROM payment p
    JOIN invoice i ON p.invoice_id = i.invoice_id
    JOIN booking b ON i.booking_id = b.booking_id
    WHERE b.customer_id NOT IN (12, 22)
);

DELETE FROM has_deposit
WHERE booking_id IN (
    SELECT booking_id FROM booking WHERE customer_id NOT IN (12, 22)
) OR payment_id IN (
    SELECT p.payment_id
    FROM payment p
    JOIN invoice i ON p.invoice_id = i.invoice_id
    JOIN booking b ON i.booking_id = b.booking_id
    WHERE b.customer_id NOT IN (12, 22)
) OR invoice_id IN (
    SELECT i.invoice_id
    FROM invoice i
    JOIN booking b ON i.booking_id = b.booking_id
    WHERE b.customer_id NOT IN (12, 22)
);

-- -----------------------------------------------------------------------------------
-- Step 2: Clear Payments
-- Delete all records from the payment table where the user_id (or customer_id) is NOT IN (12, 22)
-- -----------------------------------------------------------------------------------
DELETE FROM payment
WHERE invoice_id IN (
    SELECT i.invoice_id
    FROM invoice i
    JOIN booking b ON i.booking_id = b.booking_id
    WHERE b.customer_id NOT IN (12, 22)
);

-- Delete invoices linked to bookings of users NOT IN (12, 22)
DELETE FROM invoice
WHERE booking_id IN (
    SELECT booking_id FROM booking WHERE customer_id NOT IN (12, 22)
);

-- -----------------------------------------------------------------------------------
-- Step 3: Clear Feedback
-- Delete all records from the feedback table linked to bookings belonging to users NOT IN (12, 22)
-- -----------------------------------------------------------------------------------
DELETE FROM feedback
WHERE customer_id NOT IN (12, 22)
   OR booking_id IN (
       SELECT booking_id FROM booking WHERE customer_id NOT IN (12, 22)
   );

-- Delete inspection and booking_additional_service records linked to bookings of users NOT IN (12, 22)
DELETE FROM inspection
WHERE booking_id IN (
    SELECT booking_id FROM booking WHERE customer_id NOT IN (12, 22)
);

DELETE FROM booking_additional_service
WHERE booking_id IN (
    SELECT booking_id FROM booking WHERE customer_id NOT IN (12, 22)
);

-- -----------------------------------------------------------------------------------
-- Step 4: Clear Bookings
-- Delete all records from the booking table where the user ID is NOT IN (12, 22)
-- -----------------------------------------------------------------------------------
DELETE FROM booking
WHERE customer_id NOT IN (12, 22);

-- -----------------------------------------------------------------------------------
-- Step 5: Clear Incidents
-- Delete all records from the incident table where the user ID is NOT IN (12, 22)
-- -----------------------------------------------------------------------------------
DELETE FROM incident
WHERE customer_id NOT IN (12, 22);

-- -----------------------------------------------------------------------------------
-- Step 6: Purge Users
-- Delete dependent user child table records (user_contact_number, customer, staff)
-- and then delete from the main users table where system_id is NOT IN (12, 22)
-- -----------------------------------------------------------------------------------
DELETE FROM user_contact_number
WHERE system_id NOT IN (12, 22);

DELETE FROM customer
WHERE system_id NOT IN (12, 22);

DELETE FROM staff
WHERE system_id NOT IN (12, 22);

DELETE FROM users
WHERE system_id NOT IN (12, 22);

COMMIT TRANSACTION;
GO
