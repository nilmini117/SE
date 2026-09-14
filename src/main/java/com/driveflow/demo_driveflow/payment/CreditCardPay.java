package com.driveflow.demo_driveflow.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "credit_card_pay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardPay extends Payment {

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "card_no")
    private String cardNo;
}
