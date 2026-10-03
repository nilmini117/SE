-- ==============================================================================
-- Migration Script: Update chk_booking_status Constraint
-- Database: Microsoft SQL Server (and MySQL equivalent documented below)
-- System: DriveFlow Car Rental System
-- Purpose: Resolve CHECK constraint violation blocking vehicle return flow
--          by adding 'RETURNED' alongside all valid booking statuses.
-- ==============================================================================

USE driveflow_assignment_db;
GO

-- ------------------------------------------------------------------------------
-- 1. Microsoft SQL Server Migration
-- ------------------------------------------------------------------------------

-- Step 1: Drop existing constraint if it exists
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'chk_booking_status')
BEGIN
    ALTER TABLE dbo.booking DROP CONSTRAINT chk_booking_status;
    PRINT '>> Successfully dropped existing constraint: chk_booking_status';
END
GO

-- Step 2: Add updated check constraint with RETURNED and all valid statuses
ALTER TABLE dbo.booking 
    ADD CONSTRAINT chk_booking_status 
    CHECK (status IN ('PENDING', 'APPROVED', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'ACTIVE', 'RETURNED'));
PRINT '>> Successfully added updated constraint chk_booking_status with RETURNED support.';
GO

-- Step 3: Synchronize trg_AfterBookingStatusUpdate to release vehicle inventory on RETURNED
IF OBJECT_ID('dbo.trg_AfterBookingStatusUpdate', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_AfterBookingStatusUpdate;
GO

CREATE TRIGGER trg_AfterBookingStatusUpdate
ON dbo.booking
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(status)
    BEGIN
        -- Update vehicle to BOOKED when booking is confirmed or approved
        UPDATE v
        SET v.status = 'BOOKED'
        FROM dbo.vehicle v
        JOIN inserted i ON v.vehicle_id = i.vehicle_id
        WHERE i.status IN ('CONFIRMED', 'APPROVED');

        -- Update vehicle to AVAILABLE when booking is completed, cancelled, or returned
        UPDATE v
        SET v.status = 'AVAILABLE'
        FROM dbo.vehicle v
        JOIN inserted i ON v.vehicle_id = i.vehicle_id
        WHERE i.status IN ('COMPLETED', 'CANCELLED', 'RETURNED');
    END
END;
PRINT '>> Successfully updated trg_AfterBookingStatusUpdate trigger with RETURNED inventory release.';
GO

-- ------------------------------------------------------------------------------
-- 2. MySQL Compatible Syntax Reference (MySQL 8.0.16+)
-- ------------------------------------------------------------------------------
/*
ALTER TABLE booking DROP CHECK chk_booking_status;
ALTER TABLE booking ADD CONSTRAINT chk_booking_status 
    CHECK (status IN ('PENDING', 'APPROVED', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'ACTIVE', 'RETURNED'));
*/
