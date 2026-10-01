package com.driveflow.demo_driveflow.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.driveflow.demo_driveflow.email.EmailService emailService;

    public static final String NIC_REGEX = "^[0-9]{12}$";
    public static final String MOBILE_REGEX = "^[0-9]{10}$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$";
    public static final String DRIVING_LICENSE_REGEX = "^[A-Za-z]\\d{6}$";

    @Override
    @Transactional
    public Customer registerCustomer(CustomerRegistrationDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Registration data cannot be null.");
        }

        // 1. Strict Validation: NIC Number (Must be exactly 12 integers, no letters or special chars)
        String nic = dto.getNicNumber() != null ? dto.getNicNumber().trim() : "";
        if (!nic.matches(NIC_REGEX)) {
            throw new IllegalArgumentException("NIC number must be exactly 12 characters long and contain only integers.");
        }

        // 2. Strict Validation: Mobile Number (Must be exactly 10 integers)
        String mobile = dto.getMobileNumber() != null ? dto.getMobileNumber().trim() : "";
        if (!mobile.matches(MOBILE_REGEX)) {
            throw new IllegalArgumentException("Mobile number must be exactly 10 characters long and contain only integers.");
        }

        // 3. Strict Validation: Password (Minimum 8 chars, letters and numbers combination)
        String rawPassword = dto.getPassword() != null ? dto.getPassword() : "";
        if (!rawPassword.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain a combination of both letters and numbers.");
        }

        // 4. Strict Validation: Driving License (Must be exactly 7 characters: 1 letter followed by 6 digits)
        String license = dto.getDrivingLicense() != null ? dto.getDrivingLicense().trim().toUpperCase() : "";
        if (!license.matches(DRIVING_LICENSE_REGEX)) {
            throw new IllegalArgumentException("Driving license must be exactly 7 characters long: the first character must be an English letter (A-Z or a-z) followed by 6 numeric digits.");
        }

        String email = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email address is already registered. Please sign in instead.");
        }

        if (userRepository.existsByNic(nic)) {
            throw new IllegalArgumentException("An account with this NIC already exists.");
        }

        if (customerRepository.existsByDrivingLicense(license)) {
            throw new IllegalArgumentException("This driving license number is already registered.");
        }

        if (dto.getConfirmPassword() != null && !dto.getConfirmPassword().isBlank() && !rawPassword.equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match.");
        }

        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName() != null ? dto.getFirstName().trim() : "");
        customer.setLastName(dto.getLastName() != null ? dto.getLastName().trim() : "");
        customer.setNic(nic);
        customer.setEmail(email);
        customer.setContactNumber(mobile);
        customer.setDob(dto.getDob());
        customer.setDrivingLicense(license);
        customer.setPassword(passwordEncoder.encode(rawPassword));

        // Successful database insertion
        Customer savedCustomer = customerRepository.save(customer);

        // Post-Registration Automation: Automatically trigger welcome email with exact required phrase
        emailService.sendWelcomeEmail(savedCustomer.getEmail(), savedCustomer.getName());

        return savedCustomer;
    }

    @Override
    public Optional<Customer> findCustomerByEmail(String email) {
        if (email == null) return Optional.empty();
        return customerRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public Optional<Staff> findStaffByEmail(String email) {
        if (email == null) return Optional.empty();
        return staffRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional
    public Customer updateCustomerProfile(String email, String firstName, String lastName, String contactNumber, String drivingLicense) {
        Customer customer = findCustomerByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found for: " + email));

        String license = drivingLicense != null ? drivingLicense.trim().toUpperCase() : "";
        if (!license.isBlank()) {
            if (!license.matches(DRIVING_LICENSE_REGEX)) {
                throw new IllegalArgumentException("Driving license must be exactly 7 characters long: the first character must be an English letter (A-Z or a-z) followed by 6 numeric digits.");
            }
            if (!license.equalsIgnoreCase(customer.getDrivingLicense()) && customerRepository.existsByDrivingLicense(license)) {
                throw new IllegalArgumentException("This driving license number is already registered to another account.");
            }
        }

        if (firstName != null && !firstName.isBlank()) {
            customer.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            customer.setLastName(lastName.trim());
        }
        if (contactNumber != null && !contactNumber.isBlank()) {
            customer.setContactNumber(contactNumber.trim());
        }
        if (!license.isBlank()) {
            customer.setDrivingLicense(license);
        }

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for: " + email));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
