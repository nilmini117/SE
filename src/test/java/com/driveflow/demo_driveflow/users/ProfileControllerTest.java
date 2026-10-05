package com.driveflow.demo_driveflow.users;

import com.driveflow.demo_driveflow.otp.OtpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private ProfileController profileController;

    private Authentication createAuth(String email) {
        return new UsernamePasswordAuthenticationToken(
                email,
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should display change password form and populate email in model for authenticated user")
    void shouldShowChangePasswordForm_WhenAuthenticated() throws Exception {
        Authentication auth = createAuth("user@driveflow.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/profile/password").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/password-change"))
                .andExpect(model().attribute("email", "user@driveflow.com"));
    }

    @Test
    @DisplayName("Should dispatch password OTP via API for authenticated user")
    void shouldSendPasswordChangeOtpApi_WhenAuthenticated() throws Exception {
        Authentication auth = createAuth("user@driveflow.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/profile/api/profile/password/send-otp")
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("user@driveflow.com")));

        verify(otpService, times(1)).generatePasswordOtp("user@driveflow.com");
    }

    @Test
    @DisplayName("Should reject API OTP request with 401 when unauthenticated and no email provided")
    void shouldRejectSendOtpApi_WhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/profile/api/profile/password/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.success").value(false));

        verify(otpService, never()).generatePasswordOtp(any());
    }

    @Test
    @DisplayName("Should update password successfully when inputs are valid and OTP is provided")
    void shouldChangePassword_WhenValidInputs() throws Exception {
        Authentication auth = createAuth("user@driveflow.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/profile/password")
                .principal(auth)
                .param("oldPassword", "OldPass123")
                .param("newPassword", "NewPass123")
                .param("confirmPassword", "NewPass123")
                .param("otp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Password updated successfully!"));

        verify(userService, times(1)).changePassword("user@driveflow.com", "OldPass123", "NewPass123", "123456");
    }

    @Test
    @DisplayName("Should redirect back with error when passwords do not match")
    void shouldRejectChangePassword_WhenPasswordsDoNotMatch() throws Exception {
        Authentication auth = createAuth("user@driveflow.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/profile/password")
                .principal(auth)
                .param("oldPassword", "OldPass123")
                .param("newPassword", "NewPass123")
                .param("confirmPassword", "MismatchPass")
                .param("otp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/password"))
                .andExpect(flash().attribute("errorMessage", "New password and confirm password do not match."));

        verify(userService, never()).changePassword(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should redirect back with error when OTP is missing")
    void shouldRejectChangePassword_WhenOtpIsMissing() throws Exception {
        Authentication auth = createAuth("user@driveflow.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/profile/password")
                .principal(auth)
                .param("oldPassword", "OldPass123")
                .param("newPassword", "NewPass123")
                .param("confirmPassword", "NewPass123")
                .param("otp", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/password"))
                .andExpect(flash().attribute("errorMessage", "6-digit OTP verification code is required."));

        verify(userService, never()).changePassword(any(), any(), any(), any());
    }
}
