package com.driveflow.demo_driveflow;

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
    private CustomerRepository customerRepository;

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
            customer.setDrivingLicense("B98765432");
            customer.setPassword(passwordEncoder.encode("Customer123!"));
            customerRepository.save(customer);
            System.out.println(">> Seeded default Customer account: customer@driveflow.com / Customer123!");
        }
    }
}
