-- ==============================================================================
-- Schema Migration: Enforce NIC, Mobile, and Customer Field Constraints
-- Target Database: Relational (MySQL & SQL Server compatible)
-- ==============================================================================

-- 1. MySQL Schema Update Syntax
-- Enforce exact length of 12 for NIC and 10 for Contact/Mobile number in users/customer tables

-- In MySQL:
/*
ALTER TABLE users 
    MODIFY COLUMN nic VARCHAR(12) NOT NULL,
    MODIFY COLUMN contact_number VARCHAR(10) NOT NULL;

-- Enforce integer-only check constraints (MySQL 8.0.16+)
ALTER TABLE users 
    ADD CONSTRAINT chk_nic_length_integers 
    CHECK (CHAR_LENGTH(nic) = 12 AND nic REGEXP '^[0-9]{12}$');

ALTER TABLE users 
    ADD CONSTRAINT chk_mobile_length_integers 
    CHECK (CHAR_LENGTH(contact_number) = 10 AND contact_number REGEXP '^[0-9]{10}$');
*/

-- 2. Microsoft SQL Server Syntax (Current Sandbox Instance)
-- Modify column lengths if needed
IF COL_LENGTH('users', 'nic') IS NOT NULL
BEGIN
    ALTER TABLE users ALTER COLUMN nic VARCHAR(12) NOT NULL;
END;

IF COL_LENGTH('users', 'contact_number') IS NOT NULL
BEGIN
    ALTER TABLE users ALTER COLUMN contact_number VARCHAR(10) NOT NULL;
END;

-- Add check constraints to enforce integer-only and exact length on SQL Server
IF NOT EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'chk_nic_length_integers')
BEGIN
    ALTER TABLE users 
    ADD CONSTRAINT chk_nic_length_integers 
    CHECK (LEN(nic) = 12 AND nic NOT LIKE '%[^0-9]%');
END;

IF NOT EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'chk_mobile_length_integers')
BEGIN
    ALTER TABLE users 
    ADD CONSTRAINT chk_mobile_length_integers 
    CHECK (LEN(contact_number) = 10 AND contact_number NOT LIKE '%[^0-9]%');
END;
