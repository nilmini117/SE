package com.driveflow.demo_driveflow;

import com.driveflow.demo_driveflow.branch.Branch;
import com.driveflow.demo_driveflow.branch.BranchRepository;
import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.users.CustomerRepository;
import com.driveflow.demo_driveflow.users.Staff;
import com.driveflow.demo_driveflow.users.StaffRepository;
import com.driveflow.demo_driveflow.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private com.driveflow.demo_driveflow.booking.BookingRepository bookingRepository;

    @Autowired
    private com.driveflow.demo_driveflow.payment.InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default Staff account if not present
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

        // Seed default Customer account if not present
        if (userRepository.findByEmail("customer@driveflow.com").isEmpty()) {
            Customer customer = new Customer();
            customer.setFirstName("John");
            customer.setLastName("Customer");
            customer.setNic("NIC2002");
            customer.setEmail("customer@driveflow.com");
            customer.setContactNumber("0719876543");
            customer.setDob(LocalDate.of(1995, 5, 15));
            customer.setDrivingLicense("B11112222");
            customer.setPassword(passwordEncoder.encode("Customer123!"));
            customerRepository.save(customer);
            System.out.println(">> Seeded default Customer account: customer@driveflow.com / Customer123!");
        }

        // Ensure active bookings have invoices available for payment
        for (com.driveflow.demo_driveflow.booking.Booking b : bookingRepository.findAll()) {
            if (b.getBookingId() != null && invoiceRepository.findByBooking(b).isEmpty()
                    && !"CANCELLED".equalsIgnoreCase(b.getStatus())) {
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
    }
}
