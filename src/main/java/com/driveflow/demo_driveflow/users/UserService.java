package com.driveflow.demo_driveflow.users;

import java.util.Optional;

public interface UserService {
    Customer registerCustomer(CustomerRegistrationDto dto);
    Optional<Customer> findCustomerByEmail(String email);
    Optional<Staff> findStaffByEmail(String email);
    Optional<User> findUserByEmail(String email);
}
