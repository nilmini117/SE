package com.driveflow.demo_driveflow.payment.strategy;

import org.springframework.stereotype.Component;

@Component
public class PayPalPayment implements PaymentStrategy {
    @Override
    public void pay(double amount, String bookingId) {
        System.out.println("Processing Rs." + amount + " via PayPal for booking: " + bookingId);
        // Add specific PayPal API logic here
    }
}
