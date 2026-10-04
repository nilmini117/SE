package com.driveflow.demo_driveflow.validation;

import com.driveflow.demo_driveflow.maintenance.MaintenanceCompany;
import com.driveflow.demo_driveflow.users.CustomerRegistrationDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ValidDomainEmailValidatorTest {

    private static Validator validator;
    private final ValidDomainEmailValidator customValidator = new ValidDomainEmailValidator();

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Definition of Done: Entering a fake domain (user@thisdomaindoesnotexist123.com) is rejected by MX check")
    void shouldReject_WhenDomainIsFakeOrInactive() {
        boolean valid = customValidator.isValid("user@thisdomaindoesnotexist123.com", null);
        assertFalse(valid, "Fake domain without MX records must be rejected");
    }

    @Test
    @DisplayName("Should accept valid real email domain with active MX records (e.g. user@gmail.com)")
    void shouldAccept_WhenDomainHasActiveMxRecords() {
        boolean valid = customValidator.isValid("user@gmail.com", null);
        assertTrue(valid, "Domain with active MX records (gmail.com) must be accepted");
    }

    @Test
    @DisplayName("Should accept whitelisted test / internal domain (e.g. user@driveflow.com)")
    void shouldAccept_WhenDomainIsWhitelistedTestDomain() {
        boolean valid = customValidator.isValid("user@driveflow.com", null);
        assertTrue(valid, "Whitelisted internal test domain must be accepted");
    }

    @Test
    @DisplayName("Should reject email when syntax is invalid")
    void shouldReject_WhenEmailSyntaxIsInvalid() {
        assertFalse(customValidator.isValid("plainaddress", null));
        assertFalse(customValidator.isValid("@missingusername.com", null));
        assertFalse(customValidator.isValid("username@.com", null));
        assertFalse(customValidator.isValid("username@com", null));
        assertFalse(customValidator.isValid("user name@domain.com", null));
    }

    @Test
    @DisplayName("Bean Validation: CustomerRegistrationDto with fake domain fails @ValidDomainEmail constraint")
    void shouldFailBeanValidation_ForCustomerRegistrationDto_WithFakeDomain() {
        CustomerRegistrationDto dto = new CustomerRegistrationDto();
        dto.setFirstName("Alice");
        dto.setLastName("Smith");
        dto.setNicNumber("200012345678");
        dto.setMobileNumber("0771234567");
        dto.setDrivingLicense("B123456");
        dto.setPassword("DriveFlow2026");
        dto.setEmail("user@thisdomaindoesnotexist123.com");

        Set<ConstraintViolation<CustomerRegistrationDto>> violations = validator.validate(dto);
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));

        assertTrue(hasEmailViolation, "Validation must flag email property for fake domain");
    }

    @Test
    @DisplayName("Bean Validation: MaintenanceCompany with fake domain fails @ValidDomainEmail constraint")
    void shouldFailBeanValidation_ForMaintenanceCompany_WithFakeDomain() {
        MaintenanceCompany company = new MaintenanceCompany();
        company.setCompanyName("FixIt Garage");
        company.setContactNumber("0112345678");
        company.setEmail("service@thisdomaindoesnotexist123.com");

        Set<ConstraintViolation<MaintenanceCompany>> violations = validator.validate(company);
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));

        assertTrue(hasEmailViolation, "Validation must flag company email property for fake domain");
    }
}
