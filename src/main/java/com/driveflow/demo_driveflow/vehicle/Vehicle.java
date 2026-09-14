package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.Branch;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "vehicle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "reg_no", unique = true)
    private String regNo;

    @Column(name = "model")
    private String model;

    @Column(name = "color")
    private String color;

    @Column(name = "mileage")
    private Integer mileage;

    @Column(name = "status")
    private String status; // AVAILABLE, BOOKED, MAINTENANCE, DECOMMISSIONED

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;
}
