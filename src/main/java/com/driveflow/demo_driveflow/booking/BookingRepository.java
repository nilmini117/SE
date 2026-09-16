package com.driveflow.demo_driveflow.booking;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatus(String status);

    @Query("SELECT b FROM Booking b WHERE b.customer.systemId = :customerId")
    List<Booking> findByCustomerId(@Param("customerId") Long customerId);

    default List<Booking> findByCustomer(Customer customer) {
        if (customer == null || customer.getSystemId() == null) {
            return List.of();
        }
        return findByCustomerId(customer.getSystemId());
    }
}
