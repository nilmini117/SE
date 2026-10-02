package com.driveflow.demo_driveflow.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing an approved/confirmed customer booking with its associated invoice and payment state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedBookingPaymentDto {
    private Long bookingId;
    private String vehicleModel;
    private String vehicleRegNo;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private Integer duration;
    private BigDecimal chargedRate;
    private String bookingStatus;
    private String staffMessage;

    // Associated Invoice & Billing details
    private Long invoiceId;
    private String invoiceStatus;
    private BigDecimal rentalAmount;
    private BigDecimal lateFee;
    private BigDecimal totalAmountDue;
    private boolean isPaid;
}
