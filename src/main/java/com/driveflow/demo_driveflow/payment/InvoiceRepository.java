package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Query("SELECT i FROM Invoice i WHERE i.booking.customer = :customer")
    List<Invoice> findByBookingCustomer(@Param("customer") Customer customer);

    List<Invoice> findByStatus(String status);

    Optional<Invoice> findByBooking(Booking booking);

    Optional<Invoice> findByBookingBookingId(Long bookingId);
}

