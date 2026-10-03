package com.driveflow.demo_driveflow.feedback;

import com.driveflow.demo_driveflow.booking.Booking;
import com.driveflow.demo_driveflow.users.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "category")
    private String category; // SERVICE, VEHICLE, PRICING, STAFF

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "status")
    private String status = "OPEN"; // OPEN, RESOLVED

    @Column(name = "approval_status")
    private String approvalStatus = "PENDING"; // PENDING, APPROVED

    @Column(name = "public_visibility")
    private Boolean publicVisibility = false;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(approvalStatus);
    }

    public boolean isPubliclyVisible() {
        return Boolean.TRUE.equals(publicVisibility);
    }

    public boolean isPublicVisibility() {
        return Boolean.TRUE.equals(publicVisibility);
    }
}
