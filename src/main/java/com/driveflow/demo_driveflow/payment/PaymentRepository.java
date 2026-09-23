package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p WHERE p.invoice.booking.customer = :customer")
    List<Payment> findByInvoiceBookingCustomer(@Param("customer") Customer customer);
}
