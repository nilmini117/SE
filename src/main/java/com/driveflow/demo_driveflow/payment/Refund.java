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
@Table(name = "refund")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "reason")
    private String reason; // Canceled Booking, Schedule Modified, Goodwill, Deposit Return

    @Column(name = "refund_date")
    private LocalDate refundDate = LocalDate.now();

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = true)
    private Booking booking;
}
