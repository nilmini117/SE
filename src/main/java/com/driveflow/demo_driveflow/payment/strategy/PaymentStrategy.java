package com.driveflow.demo_driveflow.payment.strategy;

public interface PaymentStrategy {
    void pay(double amount, String bookingId);

    default void pay(double amount, String bookingId, String payerEmail) {
        pay(amount, bookingId);
    }
}
