package com.driveflow.demo_driveflow.promotion;

import com.driveflow.demo_driveflow.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "title")
    private String title;

    @Column(name = "coupon_id", length = 50, unique = true)
    private String couponId;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "vehicle_season", length = 50)
    private String vehicleSeason; // e.g. SUMMER, WINTER, PEAK, OFF_PEAK, MONSOON, ALL_SEASONS

    @Column(name = "vehicle_category", length = 50)
    private String vehicleCategory; // e.g. SEDAN, SUV, LUXURY, COMPACT, HYBRID, ALL

    @Column(name = "discount_rate")
    private BigDecimal discountRate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status")
    private String status; // ACTIVE, EXPIRED

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @PrePersist
    @PreUpdate
    public void ensureCouponIdAndSync() {
        if (this.couponId == null || this.couponId.trim().isEmpty()) {
            if (this.couponCode != null && !this.couponCode.trim().isEmpty()) {
                this.couponId = this.couponCode.trim().toUpperCase();
            } else {
                this.couponId = "CPN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }
        } else {
            this.couponId = this.couponId.trim().toUpperCase();
        }
        if (this.couponCode == null || this.couponCode.trim().isEmpty()) {
            this.couponCode = this.couponId;
        }
        if (this.status == null || this.status.trim().isEmpty()) {
            this.status = "ACTIVE";
        }
        if (this.vehicleSeason == null || this.vehicleSeason.trim().isEmpty()) {
            this.vehicleSeason = "ALL_SEASONS";
        }
        if (this.vehicleCategory == null || this.vehicleCategory.trim().isEmpty()) {
            this.vehicleCategory = "ALL";
        }
    }

    public String getCouponId() {
        return couponId != null ? couponId : couponCode;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
        if (this.couponCode == null) {
            this.couponCode = couponId;
        }
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
        if (this.couponId == null) {
            this.couponId = couponCode;
        }
    }

    public String getCategory() {
        return (vehicleCategory != null && !vehicleCategory.isBlank()) ? vehicleCategory : "ALL";
    }

    public void setCategory(String category) {
        this.vehicleCategory = category;
    }
}
