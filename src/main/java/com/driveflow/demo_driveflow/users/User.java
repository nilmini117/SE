package com.driveflow.demo_driveflow.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_id")
    private Long systemId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "nic", unique = true, length = 12)
    private String nic;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "contact_number", length = 10)
    private String contactNumber;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "password")
    private String password;

    public String getNicNumber() {
        return nic;
    }

    public void setNicNumber(String nicNumber) {
        this.nic = nicNumber;
    }

    public String getMobileNumber() {
        return contactNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.contactNumber = mobileNumber;
    }

    public String getEmail() {
        return email != null ? email.trim() : null;
    }

    public String getName() {
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? getEmail() : combined;
    }
}
