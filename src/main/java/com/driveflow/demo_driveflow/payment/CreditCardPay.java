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

    public void setCardNo(String rawCardNo) {
        if (rawCardNo == null || rawCardNo.isBlank()) {
            this.cardNo = null;
        } else {
            String digits = rawCardNo.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) {
                this.cardNo = "**** **** **** " + digits.substring(digits.length() - 4);
            } else {
                this.cardNo = "**** " + rawCardNo;
            }
        }
    }
}
