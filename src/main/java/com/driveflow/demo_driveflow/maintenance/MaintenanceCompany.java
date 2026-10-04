package com.driveflow.demo_driveflow.maintenance;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "maintenance_company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long companyId;

    @NotBlank(message = "Company name is mandatory.")
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotBlank(message = "Company email is mandatory.")
    @com.driveflow.demo_driveflow.validation.ValidDomainEmail(message = "Please provide a valid company email address with an active domain.")
    @Column(name = "email", nullable = false)
    private String email;

    @NotBlank(message = "Contact number is mandatory.")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits long.")
    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "speciality")
    private String speciality;

    public String getDisplayName() {
        return companyName + (speciality != null && !speciality.isBlank() ? " (" + speciality + ")" : "");
    }
}
