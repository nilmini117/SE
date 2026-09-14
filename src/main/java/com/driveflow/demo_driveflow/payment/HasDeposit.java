package com.driveflow.demo_driveflow.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

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

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "status")
    private String status;

    @OneToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;
}
