package com.driveflow.demo_driveflow.vehicle;

import com.driveflow.demo_driveflow.branch.Branch;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "color")
    private String color;

    @Column(name = "mileage")
    private Integer mileage;

    @Column(name = "status")
    private String status; // AVAILABLE, BOOKED, MAINTENANCE, DECOMMISSIONED

    @Column(name = "brand")
    private String brand; // Toyota, Suzuki, Honda, Tesla, Benz

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    public Vehicle(Long vehicleId, String regNo, String model, Integer quantity, String color, Integer mileage, String status, Branch branch) {
        this.vehicleId = vehicleId;
        this.regNo = regNo;
        this.model = model;
        this.quantity = quantity;
        this.color = color;
        this.mileage = mileage;
        this.status = status;
        this.branch = branch;
        if (model != null && model.contains(" ")) {
            this.brand = model.substring(0, model.indexOf(" "));
        }
    }

    public Vehicle(Long vehicleId, String regNo, String model, Integer quantity, String color, Integer mileage, String status, Branch branch, String brand) {
        this.vehicleId = vehicleId;
        this.regNo = regNo;
        this.model = model;
        this.quantity = quantity;
        this.color = color;
        this.mileage = mileage;
        this.status = status;
        this.branch = branch;
        this.brand = brand;
    }

    public String getBrand() {
        if (brand != null && !brand.isBlank()) {
            return brand;
        }
        if (model != null && model.contains(" ")) {
            return model.substring(0, model.indexOf(" "));
        }
        return model != null ? model : "";
    }

    public String getVehicleType() {
        return "Standard";
    }

    public String getDisplayName() {
        return (model != null ? model : "Vehicle") + (regNo != null ? " (" + regNo + ")" : "");
    }
}
