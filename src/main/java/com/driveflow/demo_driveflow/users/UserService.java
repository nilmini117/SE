package com.driveflow.demo_driveflow.users;

import java.util.Optional;

public interface UserService {
    Customer registerCustomer(CustomerRegistrationDto dto);
    Optional<Customer> findCustomerByEmail(String email);
    Optional<Staff> findStaffByEmail(String email);
    Optional<User> findUserByEmail(String email);
    Customer updateCustomerProfile(String email, String firstName, String lastName, String contactNumber, String drivingLicense);
    void changePassword(String email, String oldPassword, String newPassword);
}
