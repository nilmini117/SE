package com.driveflow.demo_driveflow;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.booking.BookingRepository;
import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.feedback.Feedback;
import com.driveflow.demo_driveflow.feedback.FeedbackRepository;
import com.driveflow.demo_driveflow.incident.Incident;
import com.driveflow.demo_driveflow.incident.IncidentRepository;
import com.driveflow.demo_driveflow.maintenance.Maintenance;
import com.driveflow.demo_driveflow.maintenance.MaintenanceCompany;
import com.driveflow.demo_driveflow.maintenance.MaintenanceCompanyRepository;
import com.driveflow.demo_driveflow.maintenance.MaintenanceRepository;
import com.driveflow.demo_driveflow.payment.CreditCardPay;
import com.driveflow.demo_driveflow.payment.Invoice;
import com.driveflow.demo_driveflow.payment.InvoiceRepository;
import com.driveflow.demo_driveflow.payment.PaymentRepository;
import com.driveflow.demo_driveflow.promotion.Promotion;
import com.driveflow.demo_driveflow.promotion.PromotionRepository;
import com.driveflow.demo_driveflow.users.*;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceCompanyRepository maintenanceCompanyRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        // Ensure SQL Server database constraints accept updated statuses
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE vehicle DROP CONSTRAINT IF EXISTS chk_vehicle_status;");
                jdbcTemplate.execute("ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE', 'DECOMMISSIONED', 'UNAVAILABLE'));");
            } catch (Exception e) {
                // Table might not exist or running on in-memory DB in tests
            }
            try {
                jdbcTemplate.execute("ALTER TABLE booking DROP CONSTRAINT IF EXISTS chk_booking_status;");
                jdbcTemplate.execute("ALTER TABLE booking ADD CONSTRAINT chk_booking_status CHECK (status IN ('PENDING', 'APPROVED', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'ACTIVE', 'RETURNED'));");
            } catch (Exception e) {
                // Table might not exist or running on in-memory DB in tests
            }
        }

        // 1. Seed Branches (5 authentic Sri Lankan hub locations)
        List<Branch> branches = seedBranches();

        // 2. Seed Staff Accounts (manager, counter, operations)
        seedStaff(branches);

        // 3. Seed Customer Accounts (with valid Sri Lankan licenses & NICs)
        List<Customer> customers = seedCustomers();

        // 4. Seed 3 Demo Maintenance Companies
        List<MaintenanceCompany> maintenanceCompanies = seedMaintenanceCompanies();

        // 5. Seed 15 Target Fleet Vehicles across 5 brands (Toyota, Suzuki, Honda, Tesla, Benz)
        List<Vehicle> vehicles = seedVehicles(branches);

        // 6. Seed Active Seasonal Promotions (in Sri Lankan Rupees / percentage discounts)
        seedPromotions();

        // 7. Seed Bookings (COMPLETED, RETURNED, CONFIRMED, CANCELLED)
        List<Booking> bookings = seedBookings(customers, vehicles, branches);

        // 8. Seed Invoices & Verified Payments
        seedInvoicesAndPayments(bookings);

        // 9. Seed Customer Feedback (Approved & Publicly Visible, Pending, and Private)
        seedFeedback(customers, bookings);

        // 10. Seed Maintenance Logs
        seedMaintenance(vehicles, maintenanceCompanies);

        // 11. Seed Incident Reports
        seedIncidents(customers, vehicles);

        System.out.println(">> DriveFlow Database Initializer: All core platform data successfully synced.");
    }

    private List<Branch> seedBranches() {
        record BranchData(String name, String street, String city, String contact, String email) {}
        List<BranchData> branchCatalog = List.of(
            new BranchData("Colombo Central Station", "10 Galle Road, Kollupitiya", "Colombo", "0112345678", "colombo@driveflow.com"),
            new BranchData("Kandy Heritage Hub", "45 Peradeniya Road", "Kandy", "0812233445", "kandy@driveflow.com"),
            new BranchData("Galle Coastal Office", "12 Rampart Street, Fort", "Galle", "0912244668", "galle@driveflow.com"),
            new BranchData("Bandaranaike Airport Express", "Airport Access Road", "Katunayake", "0112252844", "airport@driveflow.com"),
            new BranchData("Negombo Beachway Branch", "88 Lewis Place", "Negombo", "0312224455", "negombo@driveflow.com")
        );

        List<Branch> result = new ArrayList<>();
        List<Branch> existingBranches = branchRepository.findAll();
        for (BranchData bData : branchCatalog) {
            Branch branch = existingBranches.stream()
                    .filter(b -> (b.getBranchName() != null && b.getBranchName().equalsIgnoreCase(bData.name))
                              || (b.getEmail() != null && b.getEmail().equalsIgnoreCase(bData.email)))
                    .findFirst()
                    .orElseGet(() -> {
                        Branch b = new Branch();
                        b.setBranchName(bData.name);
                        b.setStreet(bData.street);
                        b.setCity(bData.city);
                        b.setContactNumber(bData.contact);
                        b.setEmail(bData.email);
                        return branchRepository.save(b);
                    });
            result.add(branch);
        }
        if (result.isEmpty()) {
            result.addAll(branchRepository.findAll());
        }
        return result;
    }

    private void seedStaff(List<Branch> branches) {
        Branch b1 = branches.size() > 0 ? branches.get(0) : null;
        Branch b2 = branches.size() > 1 ? branches.get(1) : b1;
        Branch b4 = branches.size() > 3 ? branches.get(3) : b1;

        if (!userRepository.existsByEmail("staff@driveflow.com") && !userRepository.existsByNic("199010010011") && !userRepository.existsByNic("STAFF1001")) {
            Staff staff = new Staff();
            staff.setFirstName("Alex");
            staff.setLastName("Staff");
            staff.setNic("199010010011");
            staff.setEmail("staff@driveflow.com");
            staff.setContactNumber("0771234567");
            staff.setDob(LocalDate.of(1990, 1, 1));
            staff.setPassword(passwordEncoder.encode("Staff123!"));
            staff.setSalary(new BigDecimal("85000.00"));
            staff.setBranch(b1);
            staffRepository.save(staff);
            System.out.println(">> Seeded staff account: staff@driveflow.com / Staff123!");
        }

        if (!userRepository.existsByEmail("manager@driveflow.com") && !userRepository.existsByNic("198520020022")) {
            Staff manager = new Staff();
            manager.setFirstName("Sarah");
            manager.setLastName("Manager");
            manager.setNic("198520020022");
            manager.setEmail("manager@driveflow.com");
            manager.setContactNumber("0712345678");
            manager.setDob(LocalDate.of(1985, 4, 12));
            manager.setPassword(passwordEncoder.encode("Staff123!"));
            manager.setSalary(new BigDecimal("125000.00"));
            manager.setBranch(b2);
            staffRepository.save(manager);
            System.out.println(">> Seeded manager account: manager@driveflow.com / Staff123!");
        }

        if (!userRepository.existsByEmail("counter@driveflow.com") && !userRepository.existsByNic("199430030033")) {
            Staff counter = new Staff();
            counter.setFirstName("Dinesh");
            counter.setLastName("Counter");
            counter.setNic("199430030033");
            counter.setEmail("counter@driveflow.com");
            counter.setContactNumber("0753456789");
            counter.setDob(LocalDate.of(1994, 9, 20));
            counter.setPassword(passwordEncoder.encode("Staff123!"));
            counter.setSalary(new BigDecimal("75000.00"));
            counter.setBranch(b4);
            staffRepository.save(counter);
        }
    }

    private List<Customer> seedCustomers() {
        record CustomerSeed(String first, String last, String email, String nic, String mobile, String license, LocalDate dob) {}
        List<CustomerSeed> list = List.of(
            new CustomerSeed("John", "Customer", "customer@driveflow.com", "199510002002", "0719876543", "B111122", LocalDate.of(1995, 5, 15)),
            new CustomerSeed("Alice", "Smith", "alice.smith@driveflow.com", "199220003003", "0772345678", "B223344", LocalDate.of(1992, 8, 22)),
            new CustomerSeed("Kasun", "Perera", "kasun.perera@driveflow.com", "198830004004", "0713456789", "B334455", LocalDate.of(1988, 11, 5)),
            new CustomerSeed("Nilmini", "Fernando", "nilmini.fernando@driveflow.com", "199640005005", "0754567890", "B445566", LocalDate.of(1996, 3, 30))
        );

        List<Customer> result = new ArrayList<>();
        for (CustomerSeed cSeed : list) {
            Optional<User> existingUser = userRepository.findByEmail(cSeed.email);
            if (existingUser.isPresent() && existingUser.get() instanceof Customer c) {
                c.setPassword(passwordEncoder.encode("Customer123!"));
                if (c.getDrivingLicense() == null || !c.getDrivingLicense().matches("^[A-Za-z]\\d{6}$")) {
                    c.setDrivingLicense(cSeed.license);
                }
                customerRepository.save(c);
                result.add(c);
            } else if (!userRepository.existsByEmail(cSeed.email) && !userRepository.existsByNic(cSeed.nic)) {
                Customer customer = new Customer();
                customer.setFirstName(cSeed.first);
                customer.setLastName(cSeed.last);
                customer.setEmail(cSeed.email);
                customer.setNic(cSeed.nic);
                customer.setContactNumber(cSeed.mobile);
                customer.setDrivingLicense(cSeed.license);
                customer.setDob(cSeed.dob);
                customer.setPassword(passwordEncoder.encode("Customer123!"));
                customerRepository.save(customer);
                result.add(customer);
            }
        }
        if (result.isEmpty()) {
            result.addAll(customerRepository.findAll());
        }
        return result;
    }

    private List<MaintenanceCompany> seedMaintenanceCompanies() {
        if (maintenanceCompanyRepository.count() == 0) {
            MaintenanceCompany c1 = new MaintenanceCompany();
            c1.setCompanyName("AutoCare Precision Services");
            c1.setEmail("autocare@precisionfleet.com");
            c1.setContactNumber("0112894567");
            c1.setAddress("45 Station Road, Colombo 03");
            c1.setSpeciality("Engine & Transmission Overhaul");
            maintenanceCompanyRepository.save(c1);

            MaintenanceCompany c2 = new MaintenanceCompany();
            c2.setCompanyName("Apex Fleet Mechanics & Bodywork");
            c2.setEmail("service@apexfleet.com");
            c2.setContactNumber("0114567890");
            c2.setAddress("122 Baseline Highway, Colombo 08");
            c2.setSpeciality("Bodywork, Paint & Structural Repairs");
            maintenanceCompanyRepository.save(c2);

            MaintenanceCompany c3 = new MaintenanceCompany();
            c3.setCompanyName("VoltTech Hybrid & EV Diagnostics");
            c3.setEmail("support@volttechfleet.com");
            c3.setContactNumber("0113456789");
            c3.setAddress("88 High Level Road, Nugegoda");
            c3.setSpeciality("Hybrid & Electric Vehicle Servicing");
            maintenanceCompanyRepository.save(c3);
        }
        return maintenanceCompanyRepository.findAll();
    }

    private List<Vehicle> seedVehicles(List<Branch> branches) {
        Branch b1 = branches.size() > 0 ? branches.get(0) : null;
        Branch b2 = branches.size() > 1 ? branches.get(1) : b1;
        Branch b3 = branches.size() > 2 ? branches.get(2) : b1;
        Branch b4 = branches.size() > 3 ? branches.get(3) : b1;
        Branch b5 = branches.size() > 4 ? branches.get(4) : b1;

        record VehicleSeed(String model, String regNo, String color, int mileage, Branch branch, Long preferredId) {}

        List<VehicleSeed> targetCatalog = List.of(
            // Toyota (3)
            new VehicleSeed("Toyota Prius 2024", "WP CA-1020", "Pearl White", 35000, b1, 1L),
            new VehicleSeed("Toyota Axio Hybrid", "CP KA-3040", "Silver", 42000, b2, 3L),
            new VehicleSeed("Toyota RAV4 Prime", "WP NC-3344", "Midnight Blue", 18000, b4, 16L),

            // Suzuki (3)
            new VehicleSeed("Suzuki Swift Sport", "SP GA-4050", "Burning Red", 19000, b3, 4L),
            new VehicleSeed("Suzuki Vitara AllGrip", "NP JC-5566", "Cool Black", 25000, b5, 17L),
            new VehicleSeed("Suzuki Jimny 4x4", "WP SJ-2024", "Kinetic Yellow", 12000, b1, null),

            // Honda (3)
            new VehicleSeed("Honda Vezel e:HEV", "WP CB-2030", "Crystal Black", 28000, b1, 2L),
            new VehicleSeed("Honda Civic Turbo", "CP KB-1122", "Rallye Red", 21000, b2, 15L),
            new VehicleSeed("Honda CR-V Elegance", "WP HC-7788", "Platinum White", 16000, b4, null),

            // Tesla (3)
            new VehicleSeed("Tesla Model 3 Dual Motor", "WP TM-3001", "Deep Metallic Blue", 8000, b1, 5L),
            new VehicleSeed("Tesla Model Y Long Range", "WP TY-3002", "Solid Black", 9500, b2, 7L),
            new VehicleSeed("Tesla Model S Plaid", "WP TS-3003", "Red Multi-Coat", 11000, b3, null),

            // Benz (3)
            new VehicleSeed("Benz C-Class C200", "WP BC-5001", "Obsidian Black", 14000, b1, 6L),
            new VehicleSeed("Benz E-Class E300", "WP BE-5002", "Iridium Silver", 17500, b2, 8L),
            new VehicleSeed("Benz GLC 300 4MATIC", "WP BG-5003", "Polar White", 13000, b3, 14L)
        );

        List<Vehicle> result = new ArrayList<>();
        for (VehicleSeed seed : targetCatalog) {
            Optional<Vehicle> existing = Optional.empty();
            if (seed.preferredId != null) {
                existing = vehicleRepository.findById(seed.preferredId);
            }
            if (existing.isEmpty()) {
                existing = vehicleRepository.findAll().stream()
                        .filter(v -> seed.regNo.equalsIgnoreCase(v.getRegNo()))
                        .findFirst();
            }

            Vehicle v = existing.orElseGet(Vehicle::new);
            v.setModel(seed.model);
            v.setRegNo(seed.regNo);
            v.setColor(seed.color);
            v.setMileage(seed.mileage);
            v.setBranch(seed.branch != null ? seed.branch : b1);
            if (v.getStatus() == null) {
                v.setStatus("AVAILABLE");
            }
            v.setQuantity(1);
            result.add(vehicleRepository.save(v));
        }

        System.out.println(">> Seeded and synchronized exactly 15 vehicles (Toyota, Suzuki, Honda, Tesla, Benz; 3 each). Total count: " + vehicleRepository.count());
        return result;
    }

    private void seedPromotions() {
        if (promotionRepository.count() == 0) {
            LocalDate now = LocalDate.now();

            Promotion p1 = new Promotion();
            p1.setTitle("Summer Park Super Saver");
            p1.setCouponCode("SUMMER15");
            p1.setDiscountRate(new BigDecimal("15.00"));
            p1.setStartDate(now.minusDays(15));
            p1.setEndDate(now.plusDays(90));
            p1.setStatus("ACTIVE");
            promotionRepository.save(p1);

            Promotion p2 = new Promotion();
            p2.setTitle("Weekend Escape Discount");
            p2.setCouponCode("ESCAPE10");
            p2.setDiscountRate(new BigDecimal("10.00"));
            p2.setStartDate(now.minusDays(10));
            p2.setEndDate(now.plusDays(60));
            p2.setStatus("ACTIVE");
            promotionRepository.save(p2);

            Promotion p3 = new Promotion();
            p3.setTitle("DriveFlow VIP Promo");
            p3.setCouponCode("DRIVEFLOW20");
            p3.setDiscountRate(new BigDecimal("20.00"));
            p3.setStartDate(now.minusDays(5));
            p3.setEndDate(now.plusDays(120));
            p3.setStatus("ACTIVE");
            promotionRepository.save(p3);

            Promotion p4 = new Promotion();
            p4.setTitle("Sri Lanka Highway Explorer");
            p4.setCouponCode("LANKARIDE");
            p4.setDiscountRate(new BigDecimal("25.00"));
            p4.setStartDate(now.minusDays(2));
            p4.setEndDate(now.plusDays(180));
            p4.setStatus("ACTIVE");
            promotionRepository.save(p4);
        }
    }

    private List<Booking> seedBookings(List<Customer> customers, List<Vehicle> vehicles, List<Branch> branches) {
        if (customers.isEmpty() || vehicles.isEmpty() || branches.isEmpty()) {
            return bookingRepository.findAll();
        }

        Customer c1 = customers.get(0); // John Customer
        Customer c2 = customers.size() > 1 ? customers.get(1) : c1; // Alice Smith
        Customer c3 = customers.size() > 2 ? customers.get(2) : c1; // Kasun Perera
        Customer c4 = customers.size() > 3 ? customers.get(3) : c1; // Nilmini Fernando

        Vehicle vToyota = vehicles.stream().filter(v -> v.getModel().contains("Prius")).findFirst().orElse(vehicles.get(0));
        Vehicle vBenz = vehicles.stream().filter(v -> v.getModel().contains("C-Class")).findFirst().orElse(vehicles.get(0));
        Vehicle vSuzuki = vehicles.stream().filter(v -> v.getModel().contains("Swift")).findFirst().orElse(vehicles.get(0));
        Vehicle vTesla = vehicles.stream().filter(v -> v.getModel().contains("Model 3")).findFirst().orElse(vehicles.get(0));
        Vehicle vHonda = vehicles.stream().filter(v -> v.getModel().contains("Vezel")).findFirst().orElse(vehicles.get(0));
        Vehicle vRAV4 = vehicles.stream().filter(v -> v.getModel().contains("RAV4")).findFirst().orElse(vehicles.get(0));

        Branch bColombo = branches.get(0);
        Branch bKandy = branches.size() > 1 ? branches.get(1) : bColombo;
        Branch bGalle = branches.size() > 2 ? branches.get(2) : bColombo;
        Branch bAirport = branches.size() > 3 ? branches.get(3) : bColombo;

        // If no bookings exist yet, seed a rich realistic set
        if (bookingRepository.count() == 0) {
            LocalDate now = LocalDate.now();

            // 1. COMPLETED Booking for John Customer (Toyota Prius)
            Booking b1 = new Booking();
            b1.setCustomer(c1);
            b1.setVehicle(vToyota);
            b1.setPickupBranch(bColombo);
            b1.setReturnBranch(bColombo);
            b1.setBookingDate(now.minusDays(25));
            b1.setEndDate(now.minusDays(20));
            b1.setChargedRate(new BigDecimal("12500.00"));
            b1.setQuantity(1);
            b1.setStatus("COMPLETED");
            bookingRepository.save(b1);

            // 2. COMPLETED Booking for Alice Smith (Benz C-Class)
            Booking b2 = new Booking();
            b2.setCustomer(c2);
            b2.setVehicle(vBenz);
            b2.setPickupBranch(bAirport);
            b2.setReturnBranch(bColombo);
            b2.setBookingDate(now.minusDays(18));
            b2.setEndDate(now.minusDays(13));
            b2.setChargedRate(new BigDecimal("35000.00"));
            b2.setQuantity(1);
            b2.setStatus("COMPLETED");
            bookingRepository.save(b2);

            // 3. RETURNED Booking for Kasun Perera (Suzuki Swift Sport)
            Booking b3 = new Booking();
            b3.setCustomer(c3);
            b3.setVehicle(vSuzuki);
            b3.setPickupBranch(bGalle);
            b3.setReturnBranch(bGalle);
            b3.setBookingDate(now.minusDays(12));
            b3.setEndDate(now.minusDays(7));
            b3.setChargedRate(new BigDecimal("9500.00"));
            b3.setQuantity(1);
            b3.setStatus("RETURNED");
            bookingRepository.save(b3);

            // 4. COMPLETED Booking for Nilmini Fernando (Tesla Model 3)
            Booking b4 = new Booking();
            b4.setCustomer(c4);
            b4.setVehicle(vTesla);
            b4.setPickupBranch(bKandy);
            b4.setReturnBranch(bKandy);
            b4.setBookingDate(now.minusDays(8));
            b4.setEndDate(now.minusDays(4));
            b4.setChargedRate(new BigDecimal("38000.00"));
            b4.setQuantity(1);
            b4.setStatus("COMPLETED");
            bookingRepository.save(b4);

            // 5. CONFIRMED Booking for John Customer (Toyota RAV4)
            Booking b5 = new Booking();
            b5.setCustomer(c1);
            b5.setVehicle(vRAV4);
            b5.setPickupBranch(bColombo);
            b5.setReturnBranch(bAirport);
            b5.setBookingDate(now.plusDays(3));
            b5.setEndDate(now.plusDays(8));
            b5.setChargedRate(new BigDecimal("22000.00"));
            b5.setQuantity(1);
            b5.setStatus("CONFIRMED");
            bookingRepository.save(b5);

            // 6. CONFIRMED Booking for Alice Smith (Honda Vezel)
            Booking b6 = new Booking();
            b6.setCustomer(c2);
            b6.setVehicle(vHonda);
            b6.setPickupBranch(bColombo);
            b6.setReturnBranch(bColombo);
            b6.setBookingDate(now.plusDays(5));
            b6.setEndDate(now.plusDays(9));
            b6.setChargedRate(new BigDecimal("16500.00"));
            b6.setQuantity(1);
            b6.setStatus("CONFIRMED");
            bookingRepository.save(b6);

            System.out.println(">> Seeded rich set of completed and confirmed bookings.");
        }

        return bookingRepository.findAll();
    }

    private void seedInvoicesAndPayments(List<Booking> bookings) {
        for (Booking b : bookings) {
            if (b.getBookingId() == null) continue;
            Optional<Invoice> existingInvoice = invoiceRepository.findByBooking(b);
            if (existingInvoice.isEmpty()) {
                Invoice inv = new Invoice();
                inv.setBooking(b);
                inv.setInvoiceDate(b.getBookingDate() != null ? b.getBookingDate() : LocalDate.now());
                BigDecimal dailyRate = b.getChargedRate() != null ? b.getChargedRate() : BigDecimal.valueOf(15000.00);
                int days = b.getDuration() != null && b.getDuration() > 0 ? b.getDuration() : 3;
                inv.setRentalAmt(dailyRate.multiply(BigDecimal.valueOf(days)));
                inv.setLateFee(BigDecimal.ZERO);

                boolean isFinalized = "COMPLETED".equalsIgnoreCase(b.getStatus()) || "RETURNED".equalsIgnoreCase(b.getStatus());
                inv.setStatus(isFinalized ? "PAID" : "UNPAID");
                Invoice savedInvoice = invoiceRepository.save(inv);

                // If completed/returned, create verified credit card payment record
                if (isFinalized && paymentRepository.findByInvoice(savedInvoice).isEmpty()) {
                    CreditCardPay pay = new CreditCardPay();
                    pay.setInvoice(savedInvoice);
                    pay.setPaymentDate(b.getEndDate() != null ? b.getEndDate() : LocalDate.now());
                    pay.setAmountPaid(savedInvoice.getTotalAmt());
                    pay.setRefNo("PAY-SL-" + savedInvoice.getInvoiceId() + "-" + (1000 + savedInvoice.getInvoiceId()));
                    pay.setBankName("Commercial Bank of Ceylon");
                    pay.setCardNo("4111222233334444");
                    pay.setStatus("COMPLETED");
                    paymentRepository.save(pay);
                }
            }
        }
    }

    private void seedFeedback(List<Customer> customers, List<Booking> bookings) {
        if (feedbackRepository.count() == 0 && !bookings.isEmpty()) {
            LocalDate now = LocalDate.now();

            // Find completed bookings to link reviews
            Booking completedToyota = bookings.stream()
                    .filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus()) && b.getVehicle() != null && b.getVehicle().getModel().contains("Prius"))
                    .findFirst().orElse(null);

            Booking completedBenz = bookings.stream()
                    .filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus()) && b.getVehicle() != null && b.getVehicle().getModel().contains("C-Class"))
                    .findFirst().orElse(null);

            Booking completedSuzuki = bookings.stream()
                    .filter(b -> ("COMPLETED".equalsIgnoreCase(b.getStatus()) || "RETURNED".equalsIgnoreCase(b.getStatus())) 
                            && b.getVehicle() != null && b.getVehicle().getModel().contains("Swift"))
                    .findFirst().orElse(null);

            Booking completedTesla = bookings.stream()
                    .filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus()) && b.getVehicle() != null && b.getVehicle().getModel().contains("Model 3"))
                    .findFirst().orElse(null);

            // 1. Toyota Prius Review (Approved & Published to Catalog)
            if (completedToyota != null) {
                Feedback f1 = new Feedback();
                f1.setCustomer(completedToyota.getCustomer());
                f1.setBooking(completedToyota);
                f1.setCategory("VEHICLE");
                f1.setMessage("The Toyota Prius was exceptionally clean, highly fuel-efficient, and drove smoothly from Colombo to Kandy. Flawless vehicle condition.");
                f1.setDate(now.minusDays(19));
                f1.setStatus("RESOLVED");
                f1.setApprovalStatus("APPROVED");
                f1.setPublicVisibility(true);
                feedbackRepository.save(f1);
            }

            // 2. Benz C-Class Review (Approved & Published to Catalog)
            if (completedBenz != null) {
                Feedback f2 = new Feedback();
                f2.setCustomer(completedBenz.getCustomer());
                f2.setBooking(completedBenz);
                f2.setCategory("SERVICE");
                f2.setMessage("Outstanding airport concierge service and prompt vehicle handover at Bandaranaike terminal. The Mercedes-Benz was in showroom condition.");
                f2.setDate(now.minusDays(12));
                f2.setStatus("RESOLVED");
                f2.setApprovalStatus("APPROVED");
                f2.setPublicVisibility(true);
                feedbackRepository.save(f2);
            }

            // 3. Suzuki Swift Review (Approved & Published to Catalog)
            if (completedSuzuki != null) {
                Feedback f3 = new Feedback();
                f3.setCustomer(completedSuzuki.getCustomer());
                f3.setBooking(completedSuzuki);
                f3.setCategory("PRICING");
                f3.setMessage("Best rental rates for a sporty hatchback in the Southern Province! Transparent pricing in Sri Lankan Rupees with zero hidden extras.");
                f3.setDate(now.minusDays(6));
                f3.setStatus("RESOLVED");
                f3.setApprovalStatus("APPROVED");
                f3.setPublicVisibility(true);
                feedbackRepository.save(f3);
            }

            // 4. Tesla Model 3 Review (Approved & Published to Catalog)
            if (completedTesla != null) {
                Feedback f4 = new Feedback();
                f4.setCustomer(completedTesla.getCustomer());
                f4.setBooking(completedTesla);
                f4.setCategory("STAFF");
                f4.setMessage("The staff at the Kandy Hub gave us a comprehensive demonstration of the EV charging system and fast charging stops. Superb customer care!");
                f4.setDate(now.minusDays(3));
                f4.setStatus("RESOLVED");
                f4.setApprovalStatus("APPROVED");
                f4.setPublicVisibility(true);
                feedbackRepository.save(f4);
            }

            // 5. Pending Review for John Customer (To allow testing Staff "Approve & Publish" button)
            if (completedToyota != null) {
                Feedback f5 = new Feedback();
                f5.setCustomer(completedToyota.getCustomer());
                f5.setBooking(completedToyota);
                f5.setCategory("STAFF");
                f5.setMessage("Courteous staff and quick digital handover on return. Appreciated the complimentary water and city road map.");
                f5.setDate(now.minusDays(1));
                f5.setStatus("OPEN");
                f5.setApprovalStatus("PENDING");
                f5.setPublicVisibility(false);
                feedbackRepository.save(f5);
            }

            // 6. Approved but Private Review (To test "Publish" toggle)
            if (completedBenz != null) {
                Feedback f6 = new Feedback();
                f6.setCustomer(completedBenz.getCustomer());
                f6.setBooking(completedBenz);
                f6.setCategory("PRICING");
                f6.setMessage("Corporate settlement invoice was issued immediately with GST tax breakdown. Very convenient for business accounting.");
                f6.setDate(now.minusDays(2));
                f6.setStatus("RESOLVED");
                f6.setApprovalStatus("APPROVED");
                f6.setPublicVisibility(false);
                feedbackRepository.save(f6);
            }

            System.out.println(">> Seeded rich feedback records: 4 published catalog reviews, 1 pending review, and 1 private review.");
        }
    }

    private void seedMaintenance(List<Vehicle> vehicles, List<MaintenanceCompany> companies) {
        if (maintenanceRepository.count() == 0 && !vehicles.isEmpty() && !companies.isEmpty()) {
            Vehicle vCivic = vehicles.stream().filter(v -> v.getModel().contains("Civic")).findFirst().orElse(vehicles.get(0));
            MaintenanceCompany comp = companies.get(0);

            Maintenance m1 = new Maintenance();
            m1.setVehicle(vCivic);
            m1.setMaintenanceCompany(comp);
            m1.setServiceDate(LocalDate.now().minusDays(14));
            m1.setApproximatedCost(new BigDecimal("25000.00"));
            m1.setCost(new BigDecimal("24500.00"));
            maintenanceRepository.save(m1);

            if (vehicles.size() > 1 && companies.size() > 1) {
                Vehicle vTesla = vehicles.stream().filter(v -> v.getModel().contains("Model Y")).findFirst().orElse(vehicles.get(1));
                Maintenance m2 = new Maintenance();
                m2.setVehicle(vTesla);
                m2.setMaintenanceCompany(companies.get(1));
                m2.setServiceDate(LocalDate.now().minusDays(7));
                m2.setApproximatedCost(new BigDecimal("18000.00"));
                m2.setCost(new BigDecimal("17500.00"));
                maintenanceRepository.save(m2);
            }
        }
    }

    private void seedIncidents(List<Customer> customers, List<Vehicle> vehicles) {
        if (incidentRepository.count() == 0 && !customers.isEmpty() && !vehicles.isEmpty()) {
            Customer c1 = customers.get(0);
            Vehicle v1 = vehicles.get(0);

            Incident inc = new Incident();
            inc.setCustomer(c1);
            inc.setVehicle(v1);
            inc.setDate(LocalDate.now().minusDays(9));
            inc.setSeverity("LOW");
            inc.setStatus("RESOLVED");
            inc.setDescription("Low tyre pressure indicator illuminated on Southern Expressway E01. Inspected and refilled at expressway service area.");
            inc.setStaffMessage("Tire checked, valve inspected, pressure normal. Cleared for operation.");
            incidentRepository.save(inc);
        }
    }
}
