package com.driveflow.demo_driveflow.users;

import com.driveflow.demo_driveflow.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    private CustomerRegistrationDto validDto;

    @BeforeEach
    void setUp() {
        validDto = new CustomerRegistrationDto();
        validDto.setFirstName("Alice");
        validDto.setLastName("Wonder");
        validDto.setEmail("alice@driveflow.com");
        validDto.setNicNumber("200012345678"); // 12 digits
        validDto.setMobileNumber("0719876543"); // 10 digits
        validDto.setPassword("DriveFlow2026");  // >= 8 chars, letters & numbers
        validDto.setConfirmPassword("DriveFlow2026");
        validDto.setDob(LocalDate.of(2000, 1, 1));
        validDto.setDrivingLicense("B123456");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when NIC has 11 digits")
    void shouldThrowException_WhenNicIs11Digits() {
        validDto.setNicNumber("20001234567"); // 11 digits

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });

        assertTrue(ex.getMessage().contains("NIC number must be exactly 12 characters"));
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when password has no numbers")
    void shouldThrowException_WhenPasswordLacksNumbers() {
        validDto.setPassword("JustLettersHere");
        validDto.setConfirmPassword("JustLettersHere");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });

        assertTrue(ex.getMessage().contains("Password must be at least 8 characters long and contain a combination of both letters and numbers"));
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when mobile number is not 10 digits")
    void shouldThrowException_WhenMobileIsNot10Digits() {
        validDto.setMobileNumber("0719876"); // 7 digits

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });

        assertTrue(ex.getMessage().contains("Mobile number must be exactly 10 characters long"));
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when driving license has no leading letter (e.g. 1234567)")
    void shouldThrowException_WhenDrivingLicenseHasNoLetter() {
        validDto.setDrivingLicense("1234567"); // 7 digits, no letter

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });

        assertTrue(ex.getMessage().contains("Driving license must be exactly 7 characters"));
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when driving license has too many letters (e.g. AB12345)")
    void shouldThrowException_WhenDrivingLicenseHasTooManyLetters() {
        validDto.setDrivingLicense("AB12345"); // 2 letters, 5 digits

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });

        assertTrue(ex.getMessage().contains("Driving license must be exactly 7 characters"));
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when driving license length is invalid")
    void shouldThrowException_WhenDrivingLicenseLengthIsInvalid() {
        validDto.setDrivingLicense("B12345"); // 6 chars (too short)

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });
        assertTrue(ex1.getMessage().contains("Driving license must be exactly 7 characters"));

        validDto.setDrivingLicense("B1234567"); // 8 chars (too long)
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerCustomer(validDto);
        });
        assertTrue(ex2.getMessage().contains("Driving license must be exactly 7 characters"));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should save customer and trigger welcome email when all inputs are valid")
    void shouldSaveCustomerAndTriggerWelcomeEmail_WhenInputsAreValid() {
        when(userRepository.existsByEmail("alice@driveflow.com")).thenReturn(false);
        when(userRepository.existsByNic("200012345678")).thenReturn(false);
        when(customerRepository.existsByDrivingLicense("B123456")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setSystemId(99L);
            return c;
        });

        Customer registered = userService.registerCustomer(validDto);

        assertNotNull(registered);
        assertEquals("alice@driveflow.com", registered.getEmail());
        assertEquals("200012345678", registered.getNic());
        assertEquals("0719876543", registered.getContactNumber());
        assertEquals("B123456", registered.getDrivingLicense());

        // Verify that database insertion occurred
        verify(customerRepository, times(1)).save(any(Customer.class));

        // Verify that post-registration automated welcome email was triggered
        verify(emailService, times(1)).sendWelcomeEmail("alice@driveflow.com", "Alice Wonder");
    }
}
