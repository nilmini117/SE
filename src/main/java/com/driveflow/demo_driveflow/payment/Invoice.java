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
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "rental_amt")
    private BigDecimal rentalAmt;

    @Column(name = "late_fee")
    private BigDecimal lateFee;

    @Column(name = "total_amt")
    private BigDecimal totalAmt;

    @Column(name = "status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
