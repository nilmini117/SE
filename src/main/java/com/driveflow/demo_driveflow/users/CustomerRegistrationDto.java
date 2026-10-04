package com.driveflow.demo_driveflow.users;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegistrationDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * NIC Number: Must be exactly 12 characters long and contain only integers.
     * Blocks any alphabetic or special characters.
     */
    @NotBlank(message = "NIC number is required")
    @Pattern(
        regexp = "^[0-9]{12}$",
        message = "NIC number must be exactly 12 characters long and contain only integers."
    )
    @JsonProperty("nic_number")
    @JsonAlias({"nic", "nic_number", "nicNumber"})
    private String nicNumber;

    @NotBlank(message = "Email is required")
    @com.driveflow.demo_driveflow.validation.ValidDomainEmail(message = "Please enter a valid email address with an active domain.")
    private String email;

    /**
     * Mobile Number: Must be exactly 10 characters long and contain only integers.
     */
    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9]{10}$",
        message = "Mobile number must be exactly 10 characters long and contain only integers."
    )
    @JsonProperty("mobile_number")
    @JsonAlias({"contactNumber", "mobile_number", "mobileNumber", "contact_number"})
    private String mobileNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    /**
     * Driving License Number: Must be exactly 7 characters long.
     * The first character must be an English letter (A-Z or a-z).
     * The remaining six characters must be numeric digits (0-9).
     */
    @NotBlank(message = "Driving license is required")
    @Pattern(
        regexp = "^[A-Za-z]\\d{6}$",
        message = "Driving license must be exactly 7 characters long: the first character must be an English letter (A-Z or a-z) followed by 6 numeric digits."
    )
    @JsonProperty("drivingLicense")
    @JsonAlias({"driving_license", "drivingLicense", "license"})
    private String drivingLicense;

    /**
     * Password: Minimum length of 8 characters. Must contain a combination of both letters and numbers.
     */
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$",
        message = "Password must be at least 8 characters long and contain a combination of both letters and numbers."
    )
    private String password;

    private String confirmPassword;

    @JsonProperty("otp")
    private String otp;

    // --- Property Aliases & Backward-Compatible Accessors ---

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getDriving_license() {
        return drivingLicense;
    }

    public void setDriving_license(String driving_license) {
        this.drivingLicense = driving_license;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getNic() {
        return nicNumber;
    }

    public void setNic(String nic) {
        this.nicNumber = nic;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getNic_number() {
        return nicNumber;
    }

    public void setNic_number(String nic_number) {
        this.nicNumber = nic_number;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getContactNumber() {
        return mobileNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.mobileNumber = contactNumber;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getMobile_number() {
        return mobileNumber;
    }

    public void setMobile_number(String mobile_number) {
        this.mobileNumber = mobile_number;
    }
}
