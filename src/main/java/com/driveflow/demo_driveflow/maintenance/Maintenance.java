package com.driveflow.demo_driveflow.maintenance;

import com.driveflow.demo_driveflow.vehicle.Vehicle;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Long maintenanceId;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "cost")
    private BigDecimal cost;

    @NotNull(message = "Approximated cost is mandatory for service scheduling.")
    @DecimalMin(value = "0.00", message = "Approximated cost must be a non-negative numerical value.")
    @Column(name = "approximated_cost", precision = 10, scale = 2)
    private BigDecimal approximatedCost;

    @NotNull(message = "Vehicle selection is mandatory.")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "An outsourced maintenance company must be selected.")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", nullable = false)
    private MaintenanceCompany maintenanceCompany;
}
