package com.driveflow.demo_driveflow.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends User {

    @Pattern(
        regexp = "^[A-Za-z]\\d{6}$",
        message = "Driving license must be exactly 7 characters long: the first character must be an English letter (A-Z or a-z) followed by 6 numeric digits."
    )
    @Column(name = "driving_license")
    private String drivingLicense;
}
