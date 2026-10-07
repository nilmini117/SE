package com.driveflow.demo_driveflow.payment.strategy;

import com.driveflow.demo_driveflow.payment.PaypalPayment;
import com.driveflow.demo_driveflow.payment.PaypalPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class PayPalPayment implements PaymentStrategy {

    @Autowired(required = false)
    private PaypalPaymentRepository paypalPaymentRepository;

    @Override
    public void pay(double amount, String bookingId) {
        pay(amount, bookingId, "customer@driveflow.com");
    }

    @Override
    public void pay(double amount, String bookingId, String payerEmail) {
        String cleanEmail = (payerEmail != null && !payerEmail.isBlank()) ? payerEmail.trim() : "customer@driveflow.com";
        String txnId = "PAYPAL-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        System.out.println("Processing Rs. " + amount + " via PayPal for booking: " + bookingId + " [Txn: " + txnId + ", Email: " + cleanEmail + "]");

        if (paypalPaymentRepository != null) {
            PaypalPayment logEntry = new PaypalPayment();
            logEntry.setPaypalTransactionId(txnId);
            try {
                if (bookingId != null && !bookingId.isBlank()) {
                    logEntry.setBookingId(Long.parseLong(bookingId.replaceAll("[^0-9]", "")));
                }
            } catch (Exception ignored) {}
            logEntry.setPayerEmail(cleanEmail);
            logEntry.setAmount(BigDecimal.valueOf(amount));
            logEntry.setPaymentDate(LocalDate.now());
            paypalPaymentRepository.save(logEntry);
        }
    }
}
