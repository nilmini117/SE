package com.driveflow.demo_driveflow.users;

import java.util.Optional;

public interface UserService {
    Customer registerCustomer(CustomerRegistrationDto dto);
    Optional<Customer> findCustomerByEmail(String email);
    Optional<Staff> findStaffByEmail(String email);
    Optional<User> findUserByEmail(String email);
    Customer updateCustomerProfile(String email, String firstName, String lastName, String contactNumber, String drivingLicense);
    Customer updateCustomerProfile(String email, String firstName, String lastName, String contactNumber, java.util.List<String> contactNumbers, String drivingLicense);
    void changePassword(String email, String oldPassword, String newPassword);
    void changePassword(String email, String oldPassword, String newPassword, String otp);
    void resetPassword(String email, String newPassword);
    void resetPassword(String email, String newPassword, String otp);
}
