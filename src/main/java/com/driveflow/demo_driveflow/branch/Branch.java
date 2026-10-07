package com.driveflow.demo_driveflow.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "branch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "contact_number")
    private String contactNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "branch_contact_number", joinColumns = @JoinColumn(name = "branch_id"))
    @Column(name = "contact_number", length = 20)
    private java.util.List<String> contactNumbers = new java.util.ArrayList<>();

    @Column(name = "email")
    private String email;

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
