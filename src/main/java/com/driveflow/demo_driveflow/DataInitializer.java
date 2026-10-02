package com.driveflow.demo_driveflow;

import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.maintenance.MaintenanceCompany;
import com.driveflow.demo_driveflow.maintenance.MaintenanceCompanyRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.Staff;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.users.UserRepository;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import com.driveflow.demo_driveflow.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceCompanyRepository maintenanceCompanyRepository;

    @Autowired
    private com.driveflow.demo_driveflow.booking.BookingRepository bookingRepository;

    @Autowired
    private com.driveflow.demo_driveflow.payment.InvoiceRepository invoiceRepository;

    @Autowired
    private com.driveflow.demo_driveflow.promotion.PromotionRepository promotionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // Ensure SQL Server database constraint accepts 'UNAVAILABLE' status
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE vehicle DROP CONSTRAINT IF EXISTS chk_vehicle_status;");
                jdbcTemplate.execute("ALTER TABLE vehicle ADD CONSTRAINT chk_vehicle_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE', 'DECOMMISSIONED', 'UNAVAILABLE'));");
            } catch (Exception e) {
                // Table might not exist or running on in-memory DB in tests
            }
        }

        // 1. Seed default Staff account if not present
        if (userRepository.findByEmail("staff@driveflow.com").isEmpty()) {
            Staff staff = new Staff();
            staff.setFirstName("Alex");
            staff.setLastName("Staff");
            staff.setNic("STAFF1001");
            staff.setEmail("staff@driveflow.com");
            staff.setContactNumber("0771234567");
            staff.setDob(LocalDate.of(1990, 1, 1));
            staff.setPassword(passwordEncoder.encode("Staff123!"));
            staff.setSalary(new BigDecimal("85000.00"));

            Branch defaultBranch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
                Branch b = new Branch();
                b.setBranchName("Head Office");
                b.setStreet("Main Street");
                b.setCity("Colombo");
                b.setContactNumber("0112345678");
                b.setEmail("headoffice@driveflow.com");
                return branchRepository.save(b);
            });
            staff.setBranch(defaultBranch);

            staffRepository.save(staff);
            System.out.println(">> Seeded default Staff account: staff@driveflow.com / Staff123!");
        }

        // 2. Seed or sync default Customer account
        userRepository.findByEmail("customer@driveflow.com").ifPresentOrElse(
            existingCustomer -> {
                existingCustomer.setPassword(passwordEncoder.encode("Customer123!"));
                if (existingCustomer instanceof Customer c) {
                    if (c.getDrivingLicense() == null || !c.getDrivingLicense().matches("^[A-Za-z]\\d{6}$")) {
                        c.setDrivingLicense("B111122");
                    }
                }
                userRepository.save(existingCustomer);
            },
            () -> {
                Customer customer = new Customer();
                customer.setFirstName("John");
                customer.setLastName("Customer");
                customer.setNic("NIC2002");
                customer.setEmail("customer@driveflow.com");
                customer.setContactNumber("0719876543");
                customer.setDob(LocalDate.of(1995, 5, 15));
                customer.setDrivingLicense("B111122");
                customer.setPassword(passwordEncoder.encode("Customer123!"));
                customerRepository.save(customer);
                System.out.println(">> Seeded default Customer account: customer@driveflow.com / Customer123!");
            }
        );

        // 3. Ensure confirmed bookings have invoices available for payment
        for (com.driveflow.demo_driveflow.booking.Booking b : bookingRepository.findAll()) {
            if (b.getBookingId() != null && invoiceRepository.findByBooking(b).isEmpty()
                    && "CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                try {
                    com.driveflow.demo_driveflow.payment.Invoice inv = new com.driveflow.demo_driveflow.payment.Invoice();
                    inv.setBooking(b);
                    inv.setInvoiceDate(LocalDate.now());
                    BigDecimal rate = b.getChargedRate() != null ? b.getChargedRate() : BigDecimal.valueOf(75.00);
                    int dur = b.getDuration() != null ? b.getDuration() : 1;
                    inv.setRentalAmt(rate.multiply(BigDecimal.valueOf(dur > 0 ? dur : 1)));
                    inv.setLateFee(BigDecimal.ZERO);
                    inv.setStatus("UNPAID");
                    invoiceRepository.save(inv);
                } catch (Exception ignored) {}
            }
        }

        // 4. Seed Active Seasonal Promotions if empty
        if (promotionRepository.count() == 0) {
            LocalDate now = LocalDate.now();

            com.driveflow.demo_driveflow.promotion.Promotion p1 = new com.driveflow.demo_driveflow.promotion.Promotion();
            p1.setTitle("Summer Park Super Saver");
            p1.setCouponCode("SUMMER15");
            p1.setDiscountRate(new BigDecimal("15.00"));
            p1.setStartDate(now.minusDays(15));
            p1.setEndDate(now.plusDays(90));
            p1.setStatus("ACTIVE");
            promotionRepository.save(p1);

            com.driveflow.demo_driveflow.promotion.Promotion p2 = new com.driveflow.demo_driveflow.promotion.Promotion();
            p2.setTitle("Weekend Escape Discount");
            p2.setCouponCode("ESCAPE10");
            p2.setDiscountRate(new BigDecimal("10.00"));
            p2.setStartDate(now.minusDays(10));
            p2.setEndDate(now.plusDays(60));
            p2.setStatus("ACTIVE");
            promotionRepository.save(p2);

            com.driveflow.demo_driveflow.promotion.Promotion p3 = new com.driveflow.demo_driveflow.promotion.Promotion();
            p3.setTitle("DriveFlow VIP Promo");
            p3.setCouponCode("DRIVEFLOW20");
            p3.setDiscountRate(new BigDecimal("20.00"));
            p3.setStartDate(now.minusDays(5));
            p3.setEndDate(now.plusDays(120));
            p3.setStatus("ACTIVE");
            promotionRepository.save(p3);

            System.out.println(">> Seeded active seasonal promotions: SUMMER15, ESCAPE10, DRIVEFLOW20");
        }

        // 5. Seed 3 Demo Maintenance Companies if empty or missing
        seedMaintenanceCompanies();

        // 6. Seed exactly 5 brands with exactly 3 distinct vehicles each (15 vehicles total)
        seedVehicles();
    }

    private void seedMaintenanceCompanies() {
        if (maintenanceCompanyRepository.count() == 0) {
            MaintenanceCompany c1 = new MaintenanceCompany();
            c1.setCompanyName("AutoCare Precision Services");
            c1.setEmail("autocare@precisionfleet.com");
            c1.setContactNumber("011-2894567");
            c1.setAddress("45 Station Road, Colombo 03");
            c1.setSpeciality("Engine & Transmission Overhaul");
            maintenanceCompanyRepository.save(c1);

            MaintenanceCompany c2 = new MaintenanceCompany();
            c2.setCompanyName("Apex Fleet Mechanics & Bodywork");
            c2.setEmail("service@apexfleet.com");
            c2.setContactNumber("011-4567890");
            c2.setAddress("122 Baseline Highway, Colombo 08");
            c2.setSpeciality("Bodywork, Paint & Structural Repairs");
            maintenanceCompanyRepository.save(c2);

            MaintenanceCompany c3 = new MaintenanceCompany();
            c3.setCompanyName("VoltTech Hybrid & EV Diagnostics");
            c3.setEmail("support@volttechfleet.com");
            c3.setContactNumber("011-3456789");
            c3.setAddress("88 High Level Road, Nugegoda");
            c3.setSpeciality("Hybrid & Electric Vehicle Servicing");
            maintenanceCompanyRepository.save(c3);

            System.out.println(">> Seeded 3 demo Maintenance Companies: AutoCare, Apex Fleet, VoltTech.");
        }
    }

    private void seedVehicles() {
        List<Branch> branches = branchRepository.findAll();
        Branch b1 = branches.size() > 0 ? branches.get(0) : null;
        Branch b2 = branches.size() > 1 ? branches.get(1) : b1;
        Branch b3 = branches.size() > 2 ? branches.get(2) : b1;
        Branch b4 = branches.size() > 3 ? branches.get(3) : b1;
        Branch b5 = branches.size() > 4 ? branches.get(4) : b1;

        // Specific 15 target vehicles: 5 brands * 3 distinct records
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

        for (VehicleSeed seed : targetCatalog) {
            // Find existing vehicle by preferred ID or regNo
            Optional<Vehicle> existing = Optional.empty();
            if (seed.preferredId != null) {
                existing = vehicleRepository.findById(seed.preferredId);
            }
            if (existing.isEmpty()) {
                existing = vehicleRepository.findAll().stream()
                        .filter(v -> seed.regNo.equalsIgnoreCase(v.getRegNo()))
                        .findFirst();
            }

            if (existing.isPresent()) {
                Vehicle v = existing.get();
                v.setModel(seed.model);
                v.setRegNo(seed.regNo);
                v.setColor(seed.color);
                v.setMileage(seed.mileage);
                if (seed.branch != null) v.setBranch(seed.branch);
                v.setStatus("AVAILABLE");
                v.setQuantity(1);
                vehicleRepository.save(v);
            } else {
                Vehicle v = new Vehicle();
                v.setModel(seed.model);
                v.setRegNo(seed.regNo);
                v.setColor(seed.color);
                v.setMileage(seed.mileage);
                v.setBranch(seed.branch != null ? seed.branch : b1);
                v.setStatus("AVAILABLE");
                v.setQuantity(1);
                vehicleRepository.save(v);
            }
        }

        System.out.println(">> Seeded and synchronized exactly 15 vehicles (Toyota, Suzuki, Honda, Tesla, Benz; 3 each). Total count: " + vehicleRepository.count());
    }
}
