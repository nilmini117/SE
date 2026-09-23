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

    List<Booking> findByStatusOrderByBookingDateAsc(String status);

    @Query("SELECT b FROM Booking b ORDER BY CASE WHEN UPPER(b.status) = 'PENDING' THEN 1 WHEN UPPER(b.status) = 'CONFIRMED' THEN 2 WHEN UPPER(b.status) = 'COMPLETED' THEN 3 ELSE 4 END, b.bookingDate DESC")
    List<Booking> findAllSortedWithPendingFirst();

    @Query("SELECT b FROM Booking b WHERE b.customer.systemId = :customerId")
    List<Booking> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.systemId = :customerId ORDER BY CASE WHEN UPPER(b.status) = 'PENDING' THEN 1 WHEN UPPER(b.status) = 'CONFIRMED' THEN 2 WHEN UPPER(b.status) = 'COMPLETED' THEN 3 ELSE 4 END, b.bookingDate DESC")
    List<Booking> findByCustomerIdSorted(@Param("customerId") Long customerId);

    long countByStatusIgnoreCase(String status);

    default List<Booking> findByCustomer(Customer customer) {
        if (customer == null || customer.getSystemId() == null) {
            return List.of();
        }
        return findByCustomerIdSorted(customer.getSystemId());
    }
}
