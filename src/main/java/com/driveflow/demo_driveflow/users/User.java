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

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_contact_number", joinColumns = @JoinColumn(name = "system_id"))
    @Column(name = "contact_number", length = 20)
    private java.util.List<String> contactNumbers = new java.util.ArrayList<>();

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "password")
    private String password;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public Boolean getIsActive() {
        return isActive == null || isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

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

    public java.util.List<String> getContactNumbers() {
        if (contactNumbers == null) {
            contactNumbers = new java.util.ArrayList<>();
        }
        return contactNumbers;
    }

    public void setContactNumbers(java.util.List<String> contactNumbers) {
        if (contactNumbers == null) {
            this.contactNumbers = new java.util.ArrayList<>();
        } else {
            this.contactNumbers = contactNumbers.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .distinct()
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }
    }
}
