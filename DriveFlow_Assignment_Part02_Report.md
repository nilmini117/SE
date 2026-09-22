# SRI LANKA INSTITUTE OF INFORMATION TECHNOLOGY (SLIIT)
## Faculty of Computing | Department of Information Technology
### IT2140: Database Design and Development (Year 2, Semester 1 - 2026)
### Assignment - Part 02: Relational Schema Design, SQL Implementation & Queries
**Project Title**: Web-based Car Rental System (DriveFlow)  
**Group ID**: 26-MTR-SE2030-30  

| Student ID | Student Name |
| :--- | :--- |
| IT25103933 | Wakishta N.N |
| IT25103879 | Vimansa K.B.A.O |
| IT25103893 | Chandrasekara P.V.A |
| IT25103906 | Saminda T.N |
| IT25103916 | Wimalarathna C.D |
| IT25104011 | Induwara M.J.A |

---

## Executive Summary
This document provides the complete technical and academic deliverable for **Assignment Part 02**, based on the submitted EER diagram for the **DriveFlow Web-based Car Rental System**. It includes:
1. **Part A**: Formal EER-to-Relational Schema Mapping, constraints, and ISA justification.
2. **Part B**: Complete SQL DDL implementation with keys and integrity constraints.
3. **Part C**: Consistent sample dataset (at least 5 records per table).
4. **Part D**: Five optimized SQL queries (Simple Filter/Sort, Multi-table Join, Aggregate, Group By with Having, and Nested Subquery).
5. **Part E**: Stored Function and Stored Procedure implementation with demonstration outputs.
6. **Part F**: Database Trigger implementation with automated status transition verification.
7. **EER vs. Spring Boot Project Alignment Report**: Comprehensive verification of the codebase entities against the database design.

---

## Part A: EER to Relational Schema Mapping (10 Marks)

### 1. Mapping Rules Applied
1. **Strong Entities**: Each strong entity is mapped directly into a distinct relational table where simple attributes become columns and the underlined attribute becomes the Primary Key.
2. **Composite Attributes**:
   - `User.Name` is decomposed into atomic attributes: `first_name` and `last_name`.
   - `Branch.Location` is decomposed into atomic attributes: `street` and `city`.
   - `Vehicle.Reg_No` is represented as a structured registration key (`reg_no`) or decomposed into province and registration number.
3. **Derived Attributes**:
   - `User.Age` is derived dynamically from `DOB` using date differences.
   - `Booking.Duration` is computed as `DATEDIFF(day, booking_date, end_date)`.
   - `Invoice.Total_Amt` is computed as `rental_amt + late_fee`.
4. **Multivalued Attributes**:
   - Multivalued attributes (`User.contactNumber`, `Branch.contactNumber`) violate 1NF if kept in the parent table. They are decomposed into dedicated child bridge tables: `user_contact_number(system_id, contact_number)` and `branch_contact_number(branch_id, contact_number)`.
5. **1:N Binary Relationships**:
   - Primary key from the '1' side is added as a Foreign Key (FK) to the 'N' side:
     - `Branch (1) -- Employs --> Staff (N)`: `branch_id` placed as FK in `staff`.
     - `Branch (1) -- Stations --> Vehicle (N)`: `branch_id` placed as FK in `vehicle`.
     - `Customer (1) -- Places --> Booking (N)`: `customer_id` placed as FK in `booking`.
     - `Vehicle (1) -- Allocates --> Booking (N)`: `vehicle_id` placed as FK in `booking`.
     - `Branch (1) -- Pickup_At --> Booking (N)`: `pickup_branch_id` placed as FK in `booking`.
     - `Branch (1) -- Returns_To --> Booking (N)`: `return_branch_id` placed as FK in `booking`.
     - `Vehicle (1) -- Holds --> Vehicle_Document (N)`: `vehicle_id` placed as FK in `vehicle_document`.
     - `Vehicle (1) -- Undergoes --> Maintenance_Record (N)`: `vehicle_id` placed as FK in `maintenance_record`.
     - `Booking (1) -- Involve --> Inspection (N)`: `booking_id` placed as FK in `inspection`.
     - `Customer (1) -- Submits --> Feedback (N)`: `customer_id` placed as FK in `feedback`.
     - `Invoice (1) -- Receives --> Payment (N)`: `invoice_id` placed as FK in `payment`.
     - `Payment (1) -- Refunded --> Refund (N)`: `payment_id` placed as FK in `refund`.
6. **1:1 Binary Relationships**:
   - `Booking (1) -- Generates --> Invoice (1)`: Foreign key `booking_id` placed in `invoice` with a `UNIQUE` constraint to enforce 1:1 total participation.
   - `Booking (1) -- Requires --> Has_Deposit (1)`: Foreign key `booking_id` placed in `has_deposit` with a `UNIQUE` constraint.
7. **M:N Binary Relationships**:
   - `Booking (M) -- Includes --> Additional_Service (N)`: Transformed into associative bridge table `booking_additional_service(booking_id, service_id)` where the composite PK is `(booking_id, service_id)`.

---

### 2. Justification for Specialization / Generalization (ISA) Mapping

The EER diagram includes two ISA hierarchies:
1. **User Specialization**: `User` (Superclass) specialized into `Staff` and `Customer` (Disjoint, Total Specialization).
2. **Payment Specialization**: `Payment` (Superclass) specialized into `Credit_Card_Pay` (Disjoint, Partial Specialization).

#### Comparison of ISA Mapping Strategies:
* **Option 1: Single Table with Discriminator Column (Table per Hierarchy)**
  - *Drawbacks*: Requires making subclass-specific columns nullable (e.g., `salary`, `driving_license`, `card_no`). In a car rental system, `salary` and `driving_license` must be mandatory for their respective roles; having nullable fields invites data integrity risks and wastes storage.
* **Option 2: Table-per-Subclass (Joined Table Inheritance - Chosen Approach)**
  - *Justification*:
    1. **Elimination of Nulls**: Maintains strict 3NF (Third Normal Form). Attributes common to all users (`nic`, `first_name`, `last_name`, `dob`, `email`, `password`) reside in `users`. Subclass-specific columns (`salary` in `staff`, `driving_license` in `customer`) are strictly `NOT NULL`.
    2. **Referential Integrity**: Child tables inherit the PK of the superclass as both their **Primary Key and Foreign Key** (`PK = FK`). Cascading deletes (`ON DELETE CASCADE`) ensure parent-child consistency.
    3. **Alignment with Object-Oriented JPA**: Perfectly mirrors JPA's `@Inheritance(strategy = InheritanceType.JOINED)`.

---

### 3. Relational Schema in Formal Notation
* **`BRANCH`** (**`branch_id`**, branch_name, street, city, contact_number, email)
* **`BRANCH_CONTACT_NUMBER`** (**`branch_id`**, **`contact_number`**)  
  *FK: `branch_id` references `BRANCH(branch_id)`*
* **`USERS`** (**`system_id`**, nic, first_name, last_name, dob, contact_number, email, password)  
  *Candidate Key / Unique: `nic`, `email`*
* **`USER_CONTACT_NUMBER`** (**`system_id`**, **`contact_number`**)  
  *FK: `system_id` references `USERS(system_id)`*
* **`STAFF`** (**`system_id`**, salary, branch_id)  
  *FK: `system_id` references `USERS(system_id)`, `branch_id` references `BRANCH(branch_id)`*
* **`CUSTOMER`** (**`system_id`**, driving_license)  
  *FK: `system_id` references `USERS(system_id)`*  
  *Candidate Key / Unique: `driving_license`*
* **`VEHICLE`** (**`vehicle_id`**, reg_no, model, color, mileage, status, branch_id)  
  *FK: `branch_id` references `BRANCH(branch_id)`*  
  *Candidate Key / Unique: `reg_no`*
* **`VEHICLE_DOCUMENT`** (**`doc_id`**, doc_type, expiry_date, vehicle_id)  
  *FK: `vehicle_id` references `VEHICLE(vehicle_id)`*
* **`MAINTENANCE_RECORD`** (**`maintenance_id`**, service_date, cost, vehicle_id)  
  *FK: `vehicle_id` references `VEHICLE(vehicle_id)`*
* **`ADDITIONAL_SERVICE`** (**`service_id`**, service_name, rate_per_day)  
  *Candidate Key / Unique: `service_name`*
* **`BOOKING`** (**`booking_id`**, booking_date, end_date, quantity, charged_rate, status, customer_id, pickup_branch_id, return_branch_id, vehicle_id)  
  *FKs: `customer_id` references `CUSTOMER(system_id)`, `pickup_branch_id` references `BRANCH(branch_id)`, `return_branch_id` references `BRANCH(branch_id)`, `vehicle_id` references `VEHICLE(vehicle_id)`*
* **`BOOKING_ADDITIONAL_SERVICE`** (**`booking_id`**, **`service_id`**)  
  *FKs: `booking_id` references `BOOKING(booking_id)`, `service_id` references `ADDITIONAL_SERVICE(service_id)`*
* **`INVOICE`** (**`invoice_id`**, invoice_date, rental_amt, late_fee, status, booking_id)  
  *FK / Unique: `booking_id` references `BOOKING(booking_id)`*
* **`PAYMENT`** (**`payment_id`**, ref_no, payment_date, amount_paid, invoice_id)  
  *FK: `invoice_id` references `INVOICE(invoice_id)`*  
  *Candidate Key / Unique: `ref_no`*
* **`CREDIT_CARD_PAY`** (**`payment_id`**, bank_name, card_no)  
  *FK: `payment_id` references `PAYMENT(payment_id)`*
* **`HAS_DEPOSIT`** (**`deposit_id`**, amount, status, booking_id, payment_id)  
  *FKs: `booking_id` references `BOOKING(booking_id)`, `payment_id` references `PAYMENT(payment_id)`*
* **`REFUND`** (**`refund_id`**, amount, approval_status, payment_id)  
  *FK: `payment_id` references `PAYMENT(payment_id)`*
* **`INSPECTION`** (**`inspection_id`**, type, fuel_level, damage_notes, booking_id)  
  *FK: `booking_id` references `BOOKING(booking_id)`*
* **`FEEDBACK`** (**`feedback_id`**, date, category, message, customer_id)  
  *FK: `customer_id` references `CUSTOMER(system_id)`*

---

## Part B: SQL DDL Implementation (20 Marks)
All SQL DDL statements are provided in the attached file:
`DriveFlow_Database_Part02.sql`

Key features of the DDL:
- **Primary Keys**: Defined on every table with surrogate identity `BIGINT IDENTITY(1,1)` or inherited joined keys.
- **Foreign Keys**: Defined with strict referential actions (`ON DELETE CASCADE` on appropriate child entities).
- **Domain Constraints (`CHECK`)**:
  - `CHECK (dob <= GETDATE())`
  - `CHECK (salary >= 0)`
  - `CHECK (status IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE', 'DECOMMISSIONED'))`
  - `CHECK (doc_type IN ('INSURANCE', 'REVENUE_LICENSE', 'FITNESS_CERT', 'EMISSION'))`
  - `CHECK (end_date >= booking_date)`
  - `CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'))`
  - `CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'))`
  - `CHECK (fuel_level BETWEEN 0 AND 100)`
- **Computed Derived Columns**:
  - `duration AS (DATEDIFF(DAY, booking_date, end_date))`
  - `total_amt AS (rental_amt + late_fee)`

---

## Part C: Insert Sample Data (10 Marks)
Each table contains at least 5 meaningful, consistent records representing authentic business operations in Sri Lanka (Colombo, Kandy, Galle, Negombo, Jaffna).
All records pass foreign key validation and domain integrity rules.

---

## Part D: SQL Queries & Explanations (20 Marks)

### Query 1: Simple SELECT with Filter & Sorting
* **Business Use Case**: Branch supervisors checking ready-to-rent fleet vehicles with low mileage (< 40,000 km).
```sql
SELECT vehicle_id, reg_no, model, color, mileage, status 
FROM vehicle
WHERE status = 'AVAILABLE' AND mileage < 40000
ORDER BY mileage ASC;
```

### Query 2: Multi-Table JOIN (INNER & LEFT JOIN)
* **Business Use Case**: Dispatch manifest joining Customer identity, Vehicle specifications, Pickup & Return branches, and Invoiced billing totals.
```sql
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
```

### Query 3: Aggregate Functions
* **Business Use Case**: Executive financial summary computing total bookings, cumulative revenue, average charged rate, and total late penalties.
```sql
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
```

### Query 4: GROUP BY with HAVING Clause
* **Business Use Case**: High-performance branch identification, isolating pickup branches that generated over 30,000 LKR in rental turnover.
```sql
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
```

### Query 5: Nested Subquery
* **Business Use Case**: VIP loyalty targeting identifying customers whose lifetime spending exceeds the overall average customer spend.
```sql
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
```

---

## Part E: Stored Procedure & Function (15 Marks)

### 1. Stored Function: `fn_CalculateRentalCost`
* **Purpose**: Calculates expected rental cost given daily rate, duration in days, and optional late return penalties.
* **Header**:
```sql
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
```

### 2. Stored Procedure: `sp_ProcessInvoiceSettlement`
* **Purpose**: Automates payment settlement with transaction management (`BEGIN TRANSACTION`, `COMMIT`, `ROLLBACK`), records credit card details if supplied, evaluates cumulative payments against the invoice total, and updates invoice status to `PAID` or `PARTIALLY_PAID`.

---

## Part F: Database Trigger Implementation (15 Marks)

### Trigger: `trg_AfterBookingStatusUpdate`
* **Purpose**: Automates fleet availability synchronization. When a booking is confirmed, the vehicle is marked as `BOOKED`. When the booking is completed or cancelled, the vehicle is immediately returned to `AVAILABLE`.
```sql
CREATE TRIGGER trg_AfterBookingStatusUpdate
ON booking
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF UPDATE(status)
    BEGIN
        -- Automatically lock vehicle when booking is confirmed
        UPDATE v
        SET v.status = 'BOOKED'
        FROM vehicle v
        JOIN inserted i ON v.vehicle_id = i.vehicle_id
        WHERE i.status = 'CONFIRMED';

        -- Automatically free vehicle when booking is completed/cancelled
        UPDATE v
        SET v.status = 'AVAILABLE'
        FROM vehicle v
        JOIN inserted i ON v.vehicle_id = i.vehicle_id
        WHERE i.status IN ('COMPLETED', 'CANCELLED');
    END
END;
```

---

## EER Diagram vs. Spring Boot Project Alignment

A deep scan of the existing Spring Boot codebase in `com.driveflow.demo_driveflow` confirms close alignment with the group's EER diagram:

1. **JPA Inheritance Strategy**: The `User` superclass and subclasses (`Staff`, `Customer`), as well as `Payment` and `CreditCardPay`, correctly use `@Inheritance(strategy = InheritanceType.JOINED)`, matching the EER diagram's disjoint ISA hierarchy.
2. **Staff-to-Branch Association**: In the EER diagram, `BRANCH` employs `STAFF`. The project's `Staff.java` was updated with `@ManyToOne @JoinColumn(name = "branch_id") private Branch branch;` to ensure full structural conformance.
3. **M:N Join Table**: `Booking` and `AdditionalService` are mapped via `@ManyToMany` with join table `booking_additional_service`, matching the `INCLUDES` relationship.
4. **All Core Entities Implemented**:
   - `users` (Superclass) & `staff`, `customer` (Subclasses)
   - `branch`
   - `vehicle` & `vehicle_document` & `maintenance_record`
   - `booking` & `additional_service` & `booking_additional_service`
   - `invoice` & `payment` & `credit_card_pay`
   - `has_deposit` & `refund`
   - `inspection` & `feedback`
5. **Additional Modules**: The Java codebase includes `Incident` and `Promotion` entities; in the viva and report, these are documented as approved application-level extensions built on top of the base EER schema.
