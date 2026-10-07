package com.driveflow.demo_driveflow.payment;

import com.driveflow.demo_driveflow.booking.Booking;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "has_deposit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HasDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_id")
    private Long depositId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status = "HELD"; // HELD, REFUNDED, FORFEITED

    @Column(name = "deposit_date")
    private LocalDate depositDate = LocalDate.now();

    @Column(name = "payment_method")
    private String paymentMethod; // CASH, CARD, BANK_TRANSFER

    @Column(name = "notes")
    private String notes;

    @OneToOne
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
}
