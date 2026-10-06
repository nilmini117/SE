package com.driveflow.demo_driveflow.payment.strategy;

import com.driveflow.demo_driveflow.payment.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentStrategyTest {

    @Mock
    private CreditCardPayment mockCreditCardPayment;

    @Mock
    private PayPalPayment mockPayPalPayment;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentContext paymentContext;

    @BeforeEach
    void setUp() {
        paymentContext = new PaymentContext();
    }

    @Test
    void testCreditCardPaymentExecution() {
        CreditCardPayment realCreditCardPayment = new CreditCardPayment();
        assertDoesNotThrow(() -> realCreditCardPayment.pay(15000.0, "BK-101"));
    }

    @Test
    void testPayPalPaymentExecution() {
        PayPalPayment realPayPalPayment = new PayPalPayment();
        assertDoesNotThrow(() -> realPayPalPayment.pay(25000.0, "BK-102"));
    }

    @Test
    void testPaymentContextWithoutStrategyThrowsException() {
        assertThrows(IllegalStateException.class, () -> paymentContext.checkout(1000.0, "BK-103"));
    }

    @Test
    void testPaymentContextExecutesStrategy() {
        paymentContext.setPaymentStrategy(mockCreditCardPayment);
        paymentContext.checkout(5000.0, "BK-104");

        verify(mockCreditCardPayment).pay(5000.0, "BK-104");
    }

    @Test
    void testPaymentServiceImplCreditCardSelection() {
        paymentService.processCustomerPayment("CREDIT_CARD", 12000.0, "BK-105");

        verify(mockCreditCardPayment).pay(12000.0, "BK-105");
    }

    @Test
    void testPaymentServiceImplPayPalSelection() {
        paymentService.processCustomerPayment("PAYPAL", 18000.0, "BK-106");

        verify(mockPayPalPayment).pay(18000.0, "BK-106");
    }

    @Test
    void testPaymentServiceImplUnknownStrategyThrowsException() {
        assertThrows(IllegalStateException.class, () -> 
            paymentService.processCustomerPayment("UNKNOWN_METHOD", 18000.0, "BK-107"));
    }
}
