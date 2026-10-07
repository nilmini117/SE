package com.driveflow.demo_driveflow.payment.strategy;

public class PaymentContext {
    private PaymentStrategy paymentStrategy;

    // Allows swapping the strategy dynamically at runtime
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void checkout(double amount, String bookingId) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not set!");
        }
        paymentStrategy.pay(amount, bookingId);
    }

    public void checkout(double amount, String bookingId, String payerEmail) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not set!");
        }
        if (payerEmail != null && !payerEmail.isBlank()) {
            paymentStrategy.pay(amount, bookingId, payerEmail);
        } else {
            paymentStrategy.pay(amount, bookingId);
        }
    }
}
