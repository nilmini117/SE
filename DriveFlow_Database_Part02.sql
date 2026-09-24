-- ============================================================================
-- SRI LANKA INSTITUTE OF INFORMATION TECHNOLOGY (SLIIT)
-- Faculty of Computing | Department of Information Technology
-- IT2140: Database Design and Development (Year 2, Semester 1 - 2026)
-- Assignment Part 02: Relational Schema Design, SQL Implementation & Queries
-- Project: Web-based Car Rental System (DriveFlow)
-- Group: 26-MTR-SE2030-30
-- ============================================================================

-- Create and Use Database
IF DB_ID('driveflow_assignment_db') IS NOT NULL
BEGIN
    ALTER DATABASE driveflow_assignment_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE driveflow_assignment_db;
END
GO

CREATE DATABASE driveflow_assignment_db;
GO

USE driveflow_assignment_db;
GO

-- ============================================================================
-- PART B: SQL DDL IMPLEMENTATION WITH CONSTRAINTS & DATA INTEGRITY
-- ============================================================================

-- 1. BRANCH TABLE
-- Decomposes composite attribute 'Location' into Street and City
CREATE TABLE branch (
    branch_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_name VARCHAR(100) NOT NULL,
    street VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);
GO

-- Multivalued contact numbers for Branch (1NF Decomposition)
CREATE TABLE branch_contact_number (
    branch_id BIGINT NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    PRIMARY KEY (branch_id, contact_number),
    CONSTRAINT fk_branch_contact FOREIGN KEY (branch_id) REFERENCES branch(branch_id) ON DELETE CASCADE
);
GO

-- 2. USERS TABLE (Superclass in ISA Specialization Hierarchy)
-- Decomposes composite attribute 'Name' into first_name and last_name
-- 'age' is implemented as a calculated derived column from 'dob'
CREATE TABLE users (
    system_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nic VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    dob DATE NOT NULL,
    age AS (DATEDIFF(YEAR, dob, GETDATE()) - CASE WHEN DATEADD(YEAR, DATEDIFF(YEAR, dob, GETDATE()), dob) > GETDATE() THEN 1 ELSE 0 END),
    contact_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT chk_user_dob CHECK (dob <= GETDATE())
);
GO

-- Multivalued contact numbers for Users (1NF Decomposition)
CREATE TABLE user_contact_number (
    system_id BIGINT NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    PRIMARY KEY (system_id, contact_number),
    CONSTRAINT fk_user_contact FOREIGN KEY (system_id) REFERENCES users(system_id) ON DELETE CASCADE
);
GO

-- 3. STAFF TABLE (Subclass ISA of USERS via Joined Table Inheritance)
-- Models the EMPLOYS relationship with BRANCH
CREATE TABLE staff (
    system_id BIGINT PRIMARY KEY,
    salary DECIMAL(10,2) NOT NULL CHECK (salary >= 0),
    branch_id BIGINT NOT NULL,
    CONSTRAINT fk_staff_user FOREIGN KEY (system_id) REFERENCES users(system_id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_branch FOREIGN KEY (branch_id) REFERENCES branch(branch_id)
);
GO

-- 4. CUSTOMER TABLE (Subclass ISA of USERS via Joined Table Inheritance)
CREATE TABLE customer (
    system_id BIGINT PRIMARY KEY,
    driving_license VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT fk_customer_user FOREIGN KEY (system_id) REFERENCES users(system_id) ON DELETE CASCADE
);
GO

-- 5. VEHICLE TABLE (Composite Reg_No decomposed, STATIONS relationship at BRANCH)
CREATE TABLE vehicle (
    vehicle_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reg_no VARCHAR(50) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL,
    color VARCHAR(50) NOT NULL,
    mileage INT NOT NULL CHECK (mileage >= 0),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    branch_id BIGINT NOT NULL,
    CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE', 'DECOMMISSIONED')),
    CONSTRAINT fk_vehicle_branch FOREIGN KEY (branch_id) REFERENCES branch(branch_id)
);
GO

-- 6. VEHICLE_DOCUMENT TABLE (HOLDS relationship with VEHICLE, 1:N)
CREATE TABLE vehicle_document (
    doc_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    doc_type VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    vehicle_id BIGINT NOT NULL,
    CONSTRAINT chk_doc_type CHECK (doc_type IN ('INSURANCE', 'REVENUE_LICENSE', 'FITNESS_CERT', 'EMISSION')),
    CONSTRAINT fk_doc_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE CASCADE
);
GO

-- 7. MAINTENANCE_RECORD TABLE (UNDERGOES relationship with VEHICLE, 1:N)
CREATE TABLE maintenance_record (
    maintenance_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    service_date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    cost DECIMAL(10,2) NOT NULL CHECK (cost >= 0),
    vehicle_id BIGINT NOT NULL,
    CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE CASCADE
);
GO

-- 8. ADDITIONAL_SERVICE TABLE
CREATE TABLE additional_service (
    service_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    rate_per_day DECIMAL(10,2) NOT NULL CHECK (rate_per_day >= 0)
);
GO

-- 9. BOOKING TABLE (PLACES by Customer, ALLOCATES Vehicle, PICKUP_AT and RETURNS_TO Branch)
-- 'duration' is implemented as a derived computed column
CREATE TABLE booking (
    booking_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    end_date DATE NOT NULL,
    duration AS (DATEDIFF(DAY, booking_date, end_date)),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    charged_rate DECIMAL(10,2) NOT NULL CHECK (charged_rate >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    customer_id BIGINT NOT NULL,
    pickup_branch_id BIGINT NOT NULL,
    return_branch_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_booking_dates CHECK (end_date >= booking_date),
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customer(system_id),
    CONSTRAINT fk_booking_pickup FOREIGN KEY (pickup_branch_id) REFERENCES branch(branch_id),
    CONSTRAINT fk_booking_return FOREIGN KEY (return_branch_id) REFERENCES branch(branch_id),
    CONSTRAINT fk_booking_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
);
GO

-- 10. BOOKING_ADDITIONAL_SERVICE (M:N Bridge Table for INCLUDES relationship)
CREATE TABLE booking_additional_service (
    booking_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    PRIMARY KEY (booking_id, service_id),
    CONSTRAINT fk_bas_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_bas_service FOREIGN KEY (service_id) REFERENCES additional_service(service_id) ON DELETE CASCADE
);
GO

-- 11. INVOICE TABLE (GENERATES relationship with BOOKING, 1:1 total participation)
-- 'total_amt' is implemented as a derived computed column (rental_amt + late_fee)
CREATE TABLE invoice (
    invoice_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    invoice_date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    rental_amt DECIMAL(10,2) NOT NULL CHECK (rental_amt >= 0),
    late_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (late_fee >= 0),
    total_amt AS (rental_amt + late_fee),
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    booking_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT chk_invoice_status CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')),
    CONSTRAINT fk_invoice_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE
);
GO

-- 12. PAYMENT TABLE (RECEIVES relationship with INVOICE, 1:N)
CREATE TABLE payment (
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    ref_no VARCHAR(100) NOT NULL UNIQUE,
    payment_date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    amount_paid DECIMAL(10,2) NOT NULL CHECK (amount_paid > 0),
    invoice_id BIGINT NOT NULL,
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(invoice_id) ON DELETE CASCADE
);
GO

-- 13. CREDIT_CARD_PAY TABLE (Subclass ISA of PAYMENT via Joined Table Inheritance)
CREATE TABLE credit_card_pay (
    payment_id BIGINT PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    card_no VARCHAR(20) NOT NULL,
    CONSTRAINT fk_cc_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id) ON DELETE CASCADE
);
GO

-- 14. HAS_DEPOSIT TABLE (REQUIRES relationship with BOOKING, PAY relationship with PAYMENT)
CREATE TABLE has_deposit (
    deposit_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'HELD',
    booking_id BIGINT NOT NULL UNIQUE,
    payment_id BIGINT NULL,
    CONSTRAINT chk_deposit_status CHECK (status IN ('HELD', 'REFUNDED', 'FORFEITED')),
    CONSTRAINT fk_deposit_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_deposit_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id)
);
GO

-- 15. REFUND TABLE (REFUNDED relationship with PAYMENT, 1:N)
CREATE TABLE refund (
    refund_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_id BIGINT NOT NULL,
    CONSTRAINT chk_refund_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payment(payment_id)
);
GO

-- 16. INSPECTION TABLE (INVOLVE relationship with BOOKING, 1:N)
CREATE TABLE inspection (
    inspection_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    fuel_level INT NOT NULL,
    damage_notes VARCHAR(500) NULL,
    booking_id BIGINT NOT NULL,
    CONSTRAINT chk_inspection_type CHECK (type IN ('PICKUP', 'RETURN', 'ROUTINE')),
    CONSTRAINT chk_inspection_fuel CHECK (fuel_level BETWEEN 0 AND 100),
    CONSTRAINT fk_inspection_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE
);
GO

-- 17. FEEDBACK TABLE (SUBMITS relationship with CUSTOMER, 1:N)
CREATE TABLE feedback (
    feedback_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    category VARCHAR(50) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    customer_id BIGINT NOT NULL,
    CONSTRAINT chk_feedback_category CHECK (category IN ('SERVICE', 'VEHICLE', 'PRICING', 'STAFF', 'OTHER')),
    CONSTRAINT fk_feedback_customer FOREIGN KEY (customer_id) REFERENCES customer(system_id) ON DELETE CASCADE
);
GO

-- 18. INCIDENT TABLE (Safety & Damage Reports, links to VEHICLE and CUSTOMER)
CREATE TABLE incident (
    incident_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    date DATE NOT NULL,
    description VARCHAR(1000) NOT NULL,
    severity VARCHAR(50) NOT NULL DEFAULT 'LOW',
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    vehicle_id BIGINT NULL,
    customer_id BIGINT NULL,
    staff_message VARCHAR(1000) NULL,
    CONSTRAINT chk_incident_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_incident_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT fk_incident_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id) ON DELETE SET NULL,
    CONSTRAINT fk_incident_customer FOREIGN KEY (customer_id) REFERENCES customer(system_id) ON DELETE SET NULL
);
GO


-- ============================================================================
-- PART C: INSERT SAMPLE DATA (At least 5 records per table)
-- ============================================================================

-- 1. Insert Branches (5 records)
INSERT INTO branch (branch_name, street, city, contact_number, email) VALUES
('Colombo Central', '120 Galle Road, Colpetty', 'Colombo', '0112345671', 'colombo@driveflow.com'),
('Kandy Hub', '45 Peradeniya Road', 'Kandy', '0812345672', 'kandy@driveflow.com'),
('Galle Coastal', '78 Matara Road', 'Galle', '0912345673', 'galle@driveflow.com'),
('Negombo Airport Branch', '12 Baseline Road', 'Negombo', '0312345674', 'negombo@driveflow.com'),
('Jaffna City Point', '89 Hospital Road', 'Jaffna', '0212345675', 'jaffna@driveflow.com');
GO

-- 2. Insert Branch Contact Numbers
INSERT INTO branch_contact_number (branch_id, contact_number) VALUES
(1, '0112345671'), (1, '0112345679'),
(2, '0812345672'),
(3, '0912345673'),
(4, '0312345674'),
(5, '0212345675');
GO

-- 3. Insert Users (Superclass: 10 records: 5 Staff + 5 Customers)
INSERT INTO users (nic, first_name, last_name, dob, contact_number, email, password) VALUES
('199010010101', 'Alex', 'Perera', '1990-01-15', '0771234567', 'alex.p@driveflow.com', 'Pass123!'),
('198820020202', 'Samantha', 'Silva', '1988-04-20', '0772345678', 'samantha.s@driveflow.com', 'Pass123!'),
('199230030303', 'Kamal', 'Fernando', '1992-08-10', '0773456789', 'kamal.f@driveflow.com', 'Pass123!'),
('198540040404', 'Nimal', 'Jayawardena', '1985-11-25', '0774567890', 'nimal.j@driveflow.com', 'Pass123!'),
('199450050505', 'Dilini', 'Gunasekara', '1994-06-30', '0775678901', 'dilini.g@driveflow.com', 'Pass123!'),
('199560060606', 'John', 'Doe', '1995-05-15', '0719876543', 'john.doe@gmail.com', 'Pass123!'),
('199770070707', 'Kasun', 'Bandara', '1997-09-12', '0718765432', 'kasun.b@gmail.com', 'Pass123!'),
('199380080808', 'Amara', 'Senanayake', '1993-02-18', '0717654321', 'amara.s@gmail.com', 'Pass123!'),
('200090090909', 'Pravin', 'Kumar', '2000-12-05', '0716543210', 'pravin.k@gmail.com', 'Pass123!'),
('199600001010', 'Fatima', 'Rizwan', '1996-07-22', '0715432109', 'fatima.r@gmail.com', 'Pass123!');
GO

-- 4. Insert User Contact Numbers
INSERT INTO user_contact_number (system_id, contact_number) VALUES
(1, '0771234567'), (1, '0112999888'),
(2, '0772345678'),
(3, '0773456789'),
(4, '0774567890'),
(5, '0775678901'),
(6, '0719876543'), (6, '0781112223'),
(7, '0718765432'),
(8, '0717654321'),
(9, '0716543210'),
(10, '0715432109');
GO

-- 5. Insert Staff (5 records)
INSERT INTO staff (system_id, salary, branch_id) VALUES
(1, 85000.00, 1),
(2, 90000.00, 2),
(3, 75000.00, 3),
(4, 95000.00, 4),
(5, 78000.00, 5);
GO

-- 6. Insert Customers (5 records)
INSERT INTO customer (system_id, driving_license) VALUES
(6, 'B98765432'),
(7, 'B87654321'),
(8, 'B76543210'),
(9, 'B65432109'),
(10, 'B54321098');
GO

-- 7. Insert Vehicles (5 records)
INSERT INTO vehicle (reg_no, model, color, mileage, status, branch_id) VALUES
('WP CA-1020', 'Toyota Prius', 'Pearl White', 35000, 'AVAILABLE', 1),
('WP CB-2030', 'Honda Vezel', 'Crystal Black', 28000, 'AVAILABLE', 1),
('CP KA-3040', 'Toyota Axio', 'Silver', 42000, 'AVAILABLE', 2),
('SP GA-4050', 'Suzuki Swift', 'Burning Red', 19000, 'AVAILABLE', 3),
('WP CC-5060', 'Nissan X-Trail', 'Navy Blue', 51000, 'AVAILABLE', 4);
GO

-- 8. Insert Vehicle Documents (5 records)
INSERT INTO vehicle_document (doc_type, expiry_date, vehicle_id) VALUES
('INSURANCE', '2027-05-30', 1),
('REVENUE_LICENSE', '2027-04-15', 1),
('INSURANCE', '2027-06-20', 2),
('REVENUE_LICENSE', '2027-08-10', 3),
('FITNESS_CERT', '2027-09-01', 4);
GO

-- 9. Insert Maintenance Records (5 records)
INSERT INTO maintenance_record (service_date, cost, vehicle_id) VALUES
('2026-08-10', 18500.00, 1),
('2026-08-15', 24000.00, 2),
('2026-08-20', 15000.00, 3),
('2026-09-02', 12000.00, 4),
('2026-09-10', 32000.00, 5);
GO

-- 10. Insert Additional Services (5 records)
INSERT INTO additional_service (service_name, rate_per_day) VALUES
('GPS Navigation System', 500.00),
('Child Safety Seat', 800.00),
('Chauffeur / Dedicated Driver', 3500.00),
('Excess Baggage Roof Carrier', 1200.00),
('Collision Damage Waiver (CDW)', 1500.00);
GO

-- 11. Insert Bookings (5 records)
INSERT INTO booking (booking_date, end_date, quantity, charged_rate, status, customer_id, pickup_branch_id, return_branch_id, vehicle_id) VALUES
('2026-09-01', '2026-09-05', 1, 8500.00, 'COMPLETED', 6, 1, 1, 1),
('2026-09-05', '2026-09-08', 1, 9500.00, 'COMPLETED', 7, 1, 2, 2),
('2026-09-10', '2026-09-15', 1, 7500.00, 'COMPLETED', 8, 2, 2, 3),
('2026-09-18', '2026-09-22', 1, 6000.00, 'CONFIRMED', 9, 3, 3, 4),
('2026-09-25', '2026-09-28', 1, 14000.00, 'PENDING', 10, 4, 1, 5);
GO

-- 12. Insert Booking Additional Services (5 records)
INSERT INTO booking_additional_service (booking_id, service_id) VALUES
(1, 1),
(1, 5),
(2, 3),
(3, 2),
(4, 5);
GO

-- 13. Insert Invoices (5 records)
INSERT INTO invoice (invoice_date, rental_amt, late_fee, status, booking_id) VALUES
('2026-09-05', 34000.00, 0.00, 'PAID', 1),
('2026-09-08', 28500.00, 1500.00, 'PAID', 2),
('2026-09-15', 37500.00, 0.00, 'PAID', 3),
('2026-09-18', 24000.00, 0.00, 'PARTIALLY_PAID', 4),
('2026-09-25', 42000.00, 0.00, 'UNPAID', 5);
GO

-- 14. Insert Payments (5 records)
INSERT INTO payment (ref_no, payment_date, amount_paid, invoice_id) VALUES
('PAY-20260905-001', '2026-09-05', 34000.00, 1),
('PAY-20260908-002', '2026-09-08', 30000.00, 2),
('PAY-20260915-003', '2026-09-15', 37500.00, 3),
('PAY-20260918-004', '2026-09-18', 12000.00, 4),
('PAY-20260920-005', '2026-09-20', 12000.00, 4);
GO

-- 15. Insert Credit Card Payments (5 records)
INSERT INTO credit_card_pay (payment_id, bank_name, card_no) VALUES
(1, 'Commercial Bank', '4111-XXXX-XXXX-1111'),
(2, 'Sampath Bank', '5200-XXXX-XXXX-2222'),
(3, 'Hatton National Bank', '4532-XXXX-XXXX-3333'),
(4, 'Nations Trust Bank', '3712-XXXX-XXXX-4444'),
(5, 'Commercial Bank', '4111-XXXX-XXXX-5555');
GO

-- 16. Insert Deposits (5 records)
INSERT INTO has_deposit (amount, status, booking_id, payment_id) VALUES
(15000.00, 'REFUNDED', 1, 1),
(15000.00, 'REFUNDED', 2, 2),
(15000.00, 'REFUNDED', 3, 3),
(15000.00, 'HELD', 4, 4),
(25000.00, 'HELD', 5, NULL);
GO

-- 17. Insert Refunds (5 records)
INSERT INTO refund (amount, approval_status, payment_id) VALUES
(15000.00, 'APPROVED', 1),
(15000.00, 'APPROVED', 2),
(15000.00, 'APPROVED', 3),
(2500.00, 'PENDING', 4),
(1000.00, 'REJECTED', 5);
GO

-- 18. Insert Inspections (5 records)
INSERT INTO inspection (type, fuel_level, damage_notes, booking_id) VALUES
('PICKUP', 100, 'Handover completed in spotless condition.', 1),
('RETURN', 100, 'Clean return, no dents or missing accessories.', 1),
('PICKUP', 90, 'Minor scratch on rear left bumper noted.', 2),
('RETURN', 80, 'Returned with 80% fuel; late fee adjusted.', 2),
('PICKUP', 100, 'Brand new vehicle handover.', 3);
GO

-- 19. Insert Feedback (5 records)
INSERT INTO feedback (date, category, message, customer_id) VALUES
('2026-09-06', 'SERVICE', 'Prompt dispatch and spotless vehicle in Colombo!', 6),
('2026-09-09', 'VEHICLE', 'Smooth drive on Kandy roads, highly recommended.', 7),
('2026-09-16', 'PRICING', 'Deposit refunded swiftly without unnecessary hassle.', 8),
('2026-09-19', 'STAFF', 'Very professional counter team at Galle branch.', 9),
('2026-09-21', 'SERVICE', 'Online reservation process was fast and effortless.', 10);
GO


-- ============================================================================
-- PART D: 5 SQL QUERIES WITH BUSINESS LOGIC & EXPLANATIONS
-- ============================================================================

PRINT '==================== QUERY 1: SIMPLE SELECT WITH FILTER & SORT ====================';
-- Business Scenario: Branch managers querying immediately dispatchable vehicles with low mileage (< 40,000 km).
SELECT 
    vehicle_id, 
    reg_no, 
    model, 
    color, 
    mileage, 
    status 
FROM vehicle
WHERE status = 'AVAILABLE' AND mileage < 40000
ORDER BY mileage ASC;
GO

PRINT '==================== QUERY 2: MULTI-TABLE JOIN (INNER & LEFT) ====================';
-- Business Scenario: Full Booking Dispatch & Billing Manifest linking Customer, Vehicle, Branches, and Invoice.
SELECT 
    b.booking_id,
    u.first_name + ' ' + u.last_name AS customer_name,
    u.contact_number,
    c.driving_license,
    v.model AS vehicle_model,
    v.reg_no AS vehicle_reg_no,
    pb.branch_name AS pickup_branch,
    rb.branch_name AS return_branch,
    b.booking_date,
    b.end_date,
    b.duration AS rental_days,
    b.status AS booking_status,
    ISNULL(i.total_amt, 0.00) AS total_invoice_amount,
    ISNULL(i.status, 'NO_INVOICE') AS invoice_status
FROM booking b
JOIN customer c ON b.customer_id = c.system_id
JOIN users u ON c.system_id = u.system_id
JOIN vehicle v ON b.vehicle_id = v.vehicle_id
JOIN branch pb ON b.pickup_branch_id = pb.branch_id
JOIN branch rb ON b.return_branch_id = rb.branch_id
LEFT JOIN invoice i ON b.booking_id = i.booking_id
ORDER BY b.booking_id ASC;
GO

PRINT '==================== QUERY 3: AGGREGATE FUNCTIONS ====================';
-- Business Scenario: Executive dashboard summarizing overall fleet utilization and financial totals.
SELECT 
    COUNT(b.booking_id) AS total_bookings_count,
    SUM(i.rental_amt) AS total_rental_revenue,
    AVG(b.charged_rate) AS avg_daily_charged_rate,
    MIN(b.charged_rate) AS min_daily_rate,
    MAX(b.charged_rate) AS max_daily_rate,
    SUM(i.late_fee) AS total_late_fees_collected,
    SUM(i.total_amt) AS gross_invoiced_revenue
FROM booking b
JOIN invoice i ON b.booking_id = i.booking_id;
GO

PRINT '==================== QUERY 4: GROUP BY WITH HAVING CLAUSE ====================';
-- Business Scenario: Branch performance analysis identifying top revenue branches generating over 30,000 LKR.
SELECT 
    pb.branch_id,
    pb.branch_name,
    pb.city,
    COUNT(b.booking_id) AS total_rentals_dispatched,
    SUM(b.duration) AS total_days_rented,
    SUM(i.total_amt) AS total_revenue_generated
FROM branch pb
JOIN booking b ON pb.branch_id = b.pickup_branch_id
JOIN invoice i ON b.booking_id = i.booking_id
GROUP BY pb.branch_id, pb.branch_name, pb.city
HAVING SUM(i.total_amt) > 30000.00
ORDER BY total_revenue_generated DESC;
GO

PRINT '==================== QUERY 5: NESTED SUBQUERY ====================';
-- Business Scenario: Loyalty department identifying VIP customers whose spending exceeds average customer spending.
SELECT 
    u.system_id,
    u.first_name + ' ' + u.last_name AS customer_name,
    u.email,
    u.contact_number,
    c.driving_license,
    SUM(p.amount_paid) AS total_paid
FROM users u
JOIN customer c ON u.system_id = c.system_id
JOIN booking b ON c.system_id = b.customer_id
JOIN invoice i ON b.booking_id = i.booking_id
JOIN payment p ON i.invoice_id = p.invoice_id
GROUP BY u.system_id, u.first_name, u.last_name, u.email, u.contact_number, c.driving_license
HAVING SUM(p.amount_paid) > (
    SELECT AVG(cust_totals.total_spent)
    FROM (
        SELECT SUM(p2.amount_paid) AS total_spent
        FROM booking b2
        JOIN invoice i2 ON b2.booking_id = i2.booking_id
        JOIN payment p2 ON i2.invoice_id = p2.invoice_id
        GROUP BY b2.customer_id
    ) AS cust_totals
);
GO


-- ============================================================================
-- PART E: STORED FUNCTION AND STORED PROCEDURE
-- ============================================================================

-- 1. Stored Function: fn_CalculateRentalCost
-- Computes the anticipated total charge including base duration and late penalties
IF OBJECT_ID('fn_CalculateRentalCost', 'FN') IS NOT NULL
    DROP FUNCTION fn_CalculateRentalCost;
GO

CREATE FUNCTION fn_CalculateRentalCost (
    @DailyRate DECIMAL(10,2),
    @DurationDays INT,
    @LateDays INT,
    @LateDailyPenalty DECIMAL(10,2)
)
RETURNS DECIMAL(10,2)
AS
BEGIN
    DECLARE @TotalCost DECIMAL(10,2);
    SET @TotalCost = (@DailyRate * @DurationDays) + (@LateDays * @LateDailyPenalty);
    RETURN ISNULL(@TotalCost, 0.00);
END;
GO

-- Demonstration: Test Stored Function
SELECT 
    b.booking_id,
    b.charged_rate,
    b.duration,
    dbo.fn_CalculateRentalCost(b.charged_rate, b.duration, 1, 1500.00) AS simulated_cost_with_1day_late
FROM booking b
WHERE b.booking_id = 1;
GO

-- 2. Stored Procedure: sp_ProcessInvoiceSettlement
-- Validates payment, records transaction, links card details if provided, and auto-updates Invoice status
IF OBJECT_ID('sp_ProcessInvoiceSettlement', 'P') IS NOT NULL
    DROP PROCEDURE sp_ProcessInvoiceSettlement;
GO

CREATE PROCEDURE sp_ProcessInvoiceSettlement
    @InvoiceID BIGINT,
    @PaymentAmount DECIMAL(10,2),
    @BankName VARCHAR(100) = NULL,
    @CardNo VARCHAR(20) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    BEGIN TRY
        -- Validate invoice exists
        IF NOT EXISTS (SELECT 1 FROM invoice WHERE invoice_id = @InvoiceID)
        BEGIN
            RAISERROR('Error: Invoice ID does not exist.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- Generate unique payment reference
        DECLARE @NewRefNo VARCHAR(100);
        SET @NewRefNo = 'PAY-' + CONVERT(VARCHAR(8), GETDATE(), 112) + '-' + CAST(ABS(CHECKSUM(NEWID())) % 90000 + 10000 AS VARCHAR(10));

        -- Insert Payment record
        INSERT INTO payment (ref_no, payment_date, amount_paid, invoice_id)
        VALUES (@NewRefNo, CAST(GETDATE() AS DATE), @PaymentAmount, @InvoiceID);

        DECLARE @NewPaymentID BIGINT = SCOPE_IDENTITY();

        -- Record Credit Card details if provided
        IF @BankName IS NOT NULL AND @CardNo IS NOT NULL
        BEGIN
            INSERT INTO credit_card_pay (payment_id, bank_name, card_no)
            VALUES (@NewPaymentID, @BankName, @CardNo);
        END

        -- Compute cumulative amount paid and compare against invoice total
        DECLARE @TotalInvoiced DECIMAL(10,2);
        DECLARE @TotalPaid DECIMAL(10,2);

        SELECT @TotalInvoiced = total_amt FROM invoice WHERE invoice_id = @InvoiceID;
        SELECT @TotalPaid = SUM(amount_paid) FROM payment WHERE invoice_id = @InvoiceID;

        IF @TotalPaid >= @TotalInvoiced
        BEGIN
            UPDATE invoice SET status = 'PAID' WHERE invoice_id = @InvoiceID;
        END
        ELSE
        BEGIN
            UPDATE invoice SET status = 'PARTIALLY_PAID' WHERE invoice_id = @InvoiceID;
        END

        COMMIT TRANSACTION;
        PRINT 'Payment processed successfully for Invoice ID: ' + CAST(@InvoiceID AS VARCHAR(10));
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
        RAISERROR(@ErrMsg, 16, 1);
    END CATCH
END;
GO

-- Demonstration: Test Stored Procedure
EXEC sp_ProcessInvoiceSettlement 
    @InvoiceID = 5, 
    @PaymentAmount = 42000.00, 
    @BankName = 'Commercial Bank', 
    @CardNo = '4111-XXXX-XXXX-9999';
GO

-- Verify Invoice Status after Stored Procedure execution
SELECT invoice_id, rental_amt, total_amt, status FROM invoice WHERE invoice_id = 5;
GO


-- ============================================================================
-- PART F: TRIGGER IMPLEMENTATION & DEMONSTRATION
-- ============================================================================

-- Trigger: trg_AfterBookingStatusUpdate
-- Automated status synchronization between Booking and Vehicle:
-- 1. When Booking becomes 'CONFIRMED', Vehicle status is auto-updated to 'BOOKED'
-- 2. When Booking is 'COMPLETED' or 'CANCELLED', Vehicle status is auto-updated to 'AVAILABLE'
IF OBJECT_ID('trg_AfterBookingStatusUpdate', 'TR') IS NOT NULL
    DROP TRIGGER trg_AfterBookingStatusUpdate;
GO

CREATE TRIGGER trg_AfterBookingStatusUpdate
ON booking
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(status)
    BEGIN
        -- Update vehicle to BOOKED when booking is confirmed
        UPDATE v
        SET v.status = 'BOOKED'
        FROM vehicle v
        JOIN inserted i ON v.vehicle_id = i.vehicle_id
        WHERE i.status = 'CONFIRMED';

        -- Update vehicle to AVAILABLE when booking is completed or cancelled
        UPDATE v
        SET v.status = 'AVAILABLE'
        FROM vehicle v
        JOIN inserted i ON v.vehicle_id = i.vehicle_id
        WHERE i.status IN ('COMPLETED', 'CANCELLED');
    END
END;
GO

-- Demonstration of Trigger
PRINT '---------------- TRIGGER DEMONSTRATION ----------------';
PRINT '1. Vehicle status before booking update:';
SELECT vehicle_id, model, status FROM vehicle WHERE vehicle_id = 4;

PRINT '2. Updating Booking 4 to CONFIRMED...';
UPDATE booking SET status = 'CONFIRMED' WHERE booking_id = 4;

PRINT '3. Vehicle status AFTER trigger fired:';
SELECT vehicle_id, model, status FROM vehicle WHERE vehicle_id = 4;

PRINT '4. Updating Booking 4 to COMPLETED...';
UPDATE booking SET status = 'COMPLETED' WHERE booking_id = 4;

PRINT '5. Vehicle status AFTER completion trigger fired:';
SELECT vehicle_id, model, status FROM vehicle WHERE vehicle_id = 4;
GO
