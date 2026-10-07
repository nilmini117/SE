package com.driveflow.demo_driveflow.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "paypal_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaypalPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "paypal_transaction_id", nullable = false, unique = true, length = 100)
    private String paypalTransactionId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "payer_email", nullable = false, length = 150)
    private String payerEmail;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;
}
