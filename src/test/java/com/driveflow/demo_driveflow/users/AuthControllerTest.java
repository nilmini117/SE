package com.driveflow.demo_driveflow.users;

import com.driveflow.demo_driveflow.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private com.driveflow.demo_driveflow.otp.OtpService otpService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private String buildJson(String nic, String mobile, String password, String drivingLicense) {
        return """
            {
                "firstName": "John",
                "lastName": "Doe",
                "email": "john.doe@driveflow.com",
                "nic_number": "%s",
                "mobile_number": "%s",
                "drivingLicense": "%s",
                "password": "%s",
                "confirmPassword": "%s"
            }
            """.formatted(nic, mobile, drivingLicense, password, password);
    }

    private String buildJson(String nic, String mobile, String password) {
        return buildJson(nic, mobile, password, "B123456");
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when NIC is 11 digits")
    void shouldRejectRegistration_WhenNicIs11Digits() throws Exception {
        // 11 digits (invalid: must be exactly 12 integers)
        String payload = buildJson("19951234567", "0771234567", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when password has no numbers")
    void shouldRejectRegistration_WhenPasswordLacksNumbers() throws Exception {
        // Password with letters only, no numbers (invalid: must contain letters and numbers)
        String payload = buildJson("199512345678", "0771234567", "PasswordOnly");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when NIC contains alphabetic characters")
    void shouldRejectRegistration_WhenNicContainsAlphabeticCharacters() throws Exception {
        // 12 chars but contains letter 'V' (invalid: integers only)
        String payload = buildJson("19951234567V", "0771234567", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when mobile number is not 10 digits")
    void shouldRejectRegistration_WhenMobileIsNot10Digits() throws Exception {
        // 6 digits (invalid: must be exactly 10 digits)
        String payload = buildJson("199512345678", "077123", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when mobile number has 9 digits")
    void shouldRejectRegistration_WhenMobileNumberHas9Digits() throws Exception {
        // 9 digits (invalid: must be exactly 10 digits)
        String payload = buildJson("199512345678", "077123456", "SecurePass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should accept valid registration with 201 Created and return welcome message")
    void shouldAcceptRegistration_WhenAllFieldsAreValid() throws Exception {
        // Valid 12-digit NIC, 10-digit mobile, alphanumeric password, valid driving license
        String payload = buildJson("199512345678", "0771234567", "SecurePass123", "B123456");

        Customer customer = new Customer();
        customer.setSystemId(101L);
        customer.setEmail("john.doe@driveflow.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setNic("199512345678");
        customer.setContactNumber("0771234567");
        customer.setDrivingLicense("B123456");

        when(userService.registerCustomer(any(CustomerRegistrationDto.class))).thenReturn(customer);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("log in succes welcome to drive flow"))
                .andExpect(jsonPath("$.email").value("john.doe@driveflow.com"));

        verify(userService, times(1)).registerCustomer(any(CustomerRegistrationDto.class));
    }

    @Test
    @DisplayName("Should accept registration when driving license format like B123456 is provided")
    void shouldAcceptRegistration_WhenDrivingLicenseFormatIsValid() throws Exception {
        // Valid license: 1 letter followed by 6 digits
        String payload = buildJson("199512345678", "0771234567", "SecurePass123", "B123456");

        Customer customer = new Customer();
        customer.setSystemId(102L);
        customer.setEmail("john.doe@driveflow.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setNic("199512345678");
        customer.setContactNumber("0771234567");
        customer.setDrivingLicense("B123456");

        when(userService.registerCustomer(any(CustomerRegistrationDto.class))).thenReturn(customer);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.success").value(true));

        verify(userService, times(1)).registerCustomer(any(CustomerRegistrationDto.class));
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when driving license has no letter (e.g. 1234567)")
    void shouldRejectRegistration_WhenDrivingLicenseHasNoLetter() throws Exception {
        // 7 digits, no leading English letter
        String payload = buildJson("199512345678", "0771234567", "SecurePass123", "1234567");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when driving license has too many letters (e.g. AB12345)")
    void shouldRejectRegistration_WhenDrivingLicenseHasTooManyLetters() throws Exception {
        // 2 letters, 5 digits
        String payload = buildJson("199512345678", "0771234567", "SecurePass123", "AB12345");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Definition of Done: Should reject registration with 400 Bad Request when email domain is fake or inactive (MX lookup check)")
    void shouldRejectRegistration_WhenEmailDomainIsFakeOrInactive() throws Exception {
        String payload = """
            {
                "firstName": "John",
                "lastName": "Doe",
                "email": "user@thisdomaindoesnotexist123.com",
                "nic_number": "199512345678",
                "mobile_number": "0771234567",
                "drivingLicense": "B123456",
                "password": "SecurePass123",
                "confirmPassword": "SecurePass123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject registration with 400 Bad Request when email format is syntactically invalid")
    void shouldRejectRegistration_WhenEmailFormatIsInvalid() throws Exception {
        String payload = """
            {
                "firstName": "John",
                "lastName": "Doe",
                "email": "not-a-valid-email",
                "nic_number": "199512345678",
                "mobile_number": "0771234567",
                "drivingLicense": "B123456",
                "password": "SecurePass123",
                "confirmPassword": "SecurePass123"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).registerCustomer(any());
    }

    @Test
    @DisplayName("Should reject send OTP with 400 Bad Request when no email is provided and unauthenticated")
    void shouldRejectSendOtp_WhenNoEmailProvidedAndUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email address is required."));

        verify(otpService, never()).generateOtp(any());
        verify(otpService, never()).generatePasswordOtp(any());
    }

    @Test
    @DisplayName("Should dispatch OTP with 200 OK when email is provided in body")
    void shouldDispatchOtp_WhenEmailIsProvidedInBody() throws Exception {
        mockMvc.perform(post("/api/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"customer@driveflow.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true));

        verify(otpService, times(1)).generateOtp("customer@driveflow.com");
    }

    @Test
    @DisplayName("Should dispatch password OTP with 200 OK when authenticated principal is present and body has no email")
    void shouldDispatchOtp_WhenAuthenticatedAndBodyHasNoEmail() throws Exception {
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "user@driveflow.com",
                        "pass",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            mockMvc.perform(post("/api/auth/otp/send")
                    .principal(auth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.success").value(true));

            verify(otpService, times(1)).generatePasswordOtp("user@driveflow.com");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
