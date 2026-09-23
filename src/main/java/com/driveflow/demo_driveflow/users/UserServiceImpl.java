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

    @Override
    @Transactional
    public Customer registerCustomer(CustomerRegistrationDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("This email address is already registered. Please sign in instead.");
        }

        String nic = dto.getNic().trim();
        if (userRepository.existsByNic(nic)) {
            throw new IllegalArgumentException("An account with this NIC already exists.");
        }

        String license = dto.getDrivingLicense().trim().toUpperCase();
        if (customerRepository.existsByDrivingLicense(license)) {
            throw new IllegalArgumentException("This driving license number is already registered.");
        }

        if (dto.getConfirmPassword() != null && !dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match.");
        }

        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName().trim());
        customer.setLastName(dto.getLastName().trim());
        customer.setNic(nic);
        customer.setEmail(email);
        customer.setContactNumber(dto.getContactNumber().trim());
        customer.setDob(dto.getDob());
        customer.setDrivingLicense(license);
        customer.setPassword(passwordEncoder.encode(dto.getPassword()));

        return customerRepository.save(customer);
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
        if (!license.equalsIgnoreCase(customer.getDrivingLicense()) && customerRepository.existsByDrivingLicense(license)) {
            throw new IllegalArgumentException("This driving license number is already registered to another account.");
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
