package com.driveflow.demo_driveflow.payment.strategy;

import org.springframework.stereotype.Component;

@Component
public class CreditCardPayment implements PaymentStrategy {
    @Override
    public void pay(double amount, String bookingId) {
        System.out.println("Processing Rs." + amount + " via Credit Card for booking: " + bookingId);
        // Add specific credit card API logic here
    }
}
