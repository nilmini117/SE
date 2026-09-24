package com.driveflow.demo_driveflow.incident;

import com.driveflow.demo_driveflow.users.Customer;
import com.driveflow.demo_driveflow.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "severity")
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status")
    private String status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "staff_message", length = 1000)
    private String staffMessage;
}
