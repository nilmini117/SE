package com.driveflow.demo_driveflow.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaypalPaymentRepository extends JpaRepository<PaypalPayment, Long> {
    Optional<PaypalPayment> findByPaypalTransactionId(String paypalTransactionId);
    List<PaypalPayment> findByBookingId(Long bookingId);
    List<PaypalPayment> findByPayerEmail(String payerEmail);
}
