package com.driveflow.demo_driveflow.vehicle;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Data Transfer Object for vehicle registration / creation payload.
 * Accepts brand, model, registration number, color, mileage, etc.
 * Note: Status and Quantity inputs are accepted in the DTO schema to handle client payloads,
 * but business logic strictly enforces status = AVAILABLE and quantity = 1 upon persistence.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistrationDto {

    @NotBlank(message = "Vehicle brand is mandatory (Toyota, Suzuki, Honda, Tesla, or Benz)")
    private String brand;

    @NotBlank(message = "Registration number is required")
    private String regNo;

    @NotBlank(message = "Vehicle make / model is required")
    private String model;

    private String color;

    private Integer mileage;

    /**
     * Operational status sent from payload (e.g. UNAVAILABLE, MAINTENANCE).
     * The service layer will ignore incoming values and enforce AVAILABLE.
     */
    private String status;

    /**
     * Inventory quantity sent from payload (e.g. 5).
     * The service layer will ignore incoming values and enforce 1.
     */
    private Integer quantity;

    private Long branchId;

    public Vehicle toEntity() {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(this.brand);
        vehicle.setRegNo(this.regNo);
        vehicle.setModel(this.model);
        vehicle.setColor(this.color);
        vehicle.setMileage(this.mileage);
        vehicle.setStatus(this.status);
        vehicle.setQuantity(this.quantity);
        return vehicle;
    }
}
