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

    @Column(name = "transmission")
    private String transmission; // e.g. Automatic (CVT), Manual, Single-Speed EV

    @Column(name = "capacity")
    private String capacity; // e.g. 5 Seats, 4 Seats, 7 Seats

    @Column(name = "fuel")
    private String fuel; // e.g. Hybrid 24 km/L, Turbo Petrol, Electric (490 km)

    @Column(name = "daily_rate")
    private java.math.BigDecimal dailyRate; // e.g. 12500.00

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_registered")
    private Boolean isRegistered = true;

    @Column(name = "service_end_date")
    private java.time.LocalDate serviceEndDate;

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

    public String getOperationalStatus() {
        return this.status;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.status = operationalStatus;
    }

    public String getDisplayName() {
        return (model != null ? model : "Vehicle") + (regNo != null ? " (" + regNo + ")" : "");
    }

    public String getTransmission() {
        if (transmission != null && !transmission.isBlank()) {
            return transmission;
        }
        return "Automatic (CVT)";
    }

    public String getCapacity() {
        if (capacity != null && !capacity.isBlank()) {
            return capacity;
        }
        return "5 Seats";
    }

    public String getFuel() {
        if (fuel != null && !fuel.isBlank()) {
            return fuel;
        }
        return "Hybrid 24 km/L";
    }

    public java.math.BigDecimal getDailyRate() {
        if (dailyRate != null && dailyRate.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return dailyRate;
        }
        return new java.math.BigDecimal("12500.00");
    }

    public String getImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        String m = (model != null ? model : "").toLowerCase();
        if (m.contains("model y")) return "/images/car_tesla_modely.jpg";
        if (m.contains("tesla")) return "/images/car_tesla_model3.jpg";
        if (m.contains("swift")) return "/images/car_suzuki_swift.jpg";
        if (m.contains("glc")) return "/images/car_benz_glc.jpg";
        if (m.contains("e-class") || m.contains("e300")) return "/images/car_benz_eclass.jpg";
        if (m.contains("c-class") || m.contains("c200")) return "/images/car_benz_cclass.jpg";
        if (m.contains("benz")) return "/images/car_benz_cclass.jpg";
        if (m.contains("prius")) return "/images/car_toyota_prius.jpg";
        if (m.contains("axio")) return "/images/car_toyota_axio.jpg";
        if (m.contains("vezel")) return "/images/car_honda_vezel.jpg";
        if (m.contains("prado") || m.contains("rav4") || m.contains("cr-v") || m.contains("vitara") || m.contains("jimny") || m.contains("suv")) return "/images/category_suv.jpg";
        if (m.contains("civic")) return "/images/category_economy.jpg";
        return "/images/lifestyle_car.jpg";
    }

    public boolean isUnderMaintenance() {
        return "MAINTENANCE".equalsIgnoreCase(status) || (status != null && status.toUpperCase().contains("SERVICE"));
    }

    public String getFormattedServiceEndDate() {
        if (serviceEndDate != null) {
            return serviceEndDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return java.time.LocalDate.now().plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public Boolean getIsRegistered() {
        return isRegistered != null ? isRegistered : true;
    }

    public boolean isRegistered() {
        return isRegistered != null ? isRegistered : true;
    }

    public String getCategoryBadge() {
        String m = (model != null ? model : "").toLowerCase();
        if (m.contains("tesla")) return "Electric Vehicle";
        if (m.contains("benz") || m.contains("c-class") || m.contains("e-class")) return "Executive Luxury";
        if (m.contains("prado") || m.contains("jimny") || m.contains("rav4") || m.contains("glc")) return "Premium SUV";
        if (m.contains("prius") || m.contains("hybrid") || m.contains("vezel") || m.contains("axio")) return "Hybrid Efficient";
        if (m.contains("swift")) return "City Hatchback";
        return "Passenger Car";
    }
}
