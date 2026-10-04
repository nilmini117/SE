package com.driveflow.demo_driveflow.users;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private com.driveflow.demo_driveflow.otp.OtpService otpService;

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "bookingRequired", required = false) String bookingRequired,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email address or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been signed out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! You can now log in and book your vehicle.");
        }
        if (bookingRequired != null) {
            model.addAttribute("infoMessage", "Please register or sign in as a customer before reserving a vehicle.");
        }

        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }

        if (!model.containsAttribute("customerDto")) {
            model.addAttribute("customerDto", new CustomerRegistrationDto());
        }
        return "auth/register";
    }

    /**
     * Web Form Registration Handler (HTML / Form Post).
     * Validates input fields and re-renders form with inline error bindings if invalid.
     */
    @PostMapping(value = "/register", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public String processRegistration(
            @Valid @ModelAttribute("customerDto") CustomerRegistrationDto customerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerCustomer(customerDto);
            redirectAttributes.addFlashAttribute("successMessage", "Your account has been created! Please log in to start booking.");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An unexpected error occurred during registration. Please check your details.");
            return "auth/register";
        }
    }

    /**
     * Fallback standard POST /register for form submissions when content-type is unspecified.
     */
    @PostMapping(value = "/register")
    public String processRegistrationDefault(
            @Valid @ModelAttribute("customerDto") CustomerRegistrationDto customerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        return processRegistration(customerDto, bindingResult, model, redirectAttributes);
    }

    /**
     * REST API Registration Endpoint (/api/auth/register, /api/register, or /register with JSON).
     * Enforces strict regex validation on NIC (12 digits), Mobile (10 digits), and Password (min 8 chars, letters & numbers).
     * Automatically triggers welcome email upon database insertion and returns HTTP 201 Created.
     * Rejects invalid input with HTTP 400 Bad Request.
     */
    @PostMapping(value = {"/api/auth/register", "/api/register"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerCustomerApi(
            @Valid @RequestBody CustomerRegistrationDto customerDto) {

        // Strict OTP Security: If an OTP is provided in the registration payload, verify it
        if (customerDto.getOtp() != null && !customerDto.getOtp().isBlank() && otpService != null) {
            boolean valid = otpService.verifyRegistrationOtp(customerDto.getEmail(), customerDto.getOtp())
                    || otpService.verifyOtp(customerDto.getEmail(), customerDto.getOtp());
            if (!valid) {
                Map<String, Object> err = new HashMap<>();
                err.put("status", HttpStatus.BAD_REQUEST.value());
                err.put("error", "Bad Request");
                err.put("message", "Invalid or expired OTP. Please verify the 6-digit code or request a new one.");
                return ResponseEntity.badRequest().body(err);
            }
        }

        Customer savedCustomer = userService.registerCustomer(customerDto);
        if (otpService != null && customerDto.getEmail() != null) {
            otpService.clearOtp(customerDto.getEmail(), com.driveflow.demo_driveflow.otp.OtpType.REGISTRATION);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CREATED.value());
        response.put("success", true);
        response.put("message", "log in succes welcome to drive flow");
        response.put("customerId", savedCustomer.getSystemId());
        response.put("email", savedCustomer.getEmail());
        response.put("nicNumber", savedCustomer.getNic());
        response.put("mobileNumber", savedCustomer.getContactNumber());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/otp/send
     * Accepts an email address, generates the OTP via OtpService, and dispatches the email.
     */
    @PostMapping(value = "/api/auth/otp/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendOtp(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "email", required = false) String emailParam) {

        String email = null;
        if (body != null && body.containsKey("email")) {
            email = body.get("email");
        }
        if ((email == null || email.isBlank()) && emailParam != null) {
            email = emailParam;
        }

        if (email == null || email.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Email address is required.");
            return ResponseEntity.badRequest().body(err);
        }

        String trimmedEmail = email.trim();
        if (otpService != null) {
            otpService.generateOtp(trimmedEmail);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("message", "A 6-digit OTP verification code has been dispatched to " + trimmedEmail + ".");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/otp/verify
     * Accepts an email address and an OTP string. Checks the cache to see if the OTP matches and is not expired.
     * Returns a 200 OK or 400 Bad Request accordingly.
     */
    @PostMapping(value = "/api/auth/otp/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyOtpEndpoint(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "email", required = false) String emailParam,
            @RequestParam(value = "otp", required = false) String otpParam) {

        String email = null;
        String otp = null;

        if (body != null) {
            if (body.containsKey("email")) email = body.get("email");
            if (body.containsKey("otp")) otp = body.get("otp");
        }
        if ((email == null || email.isBlank()) && emailParam != null) email = emailParam;
        if ((otp == null || otp.isBlank()) && otpParam != null) otp = otpParam;

        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Email address and OTP code are required.");
            return ResponseEntity.badRequest().body(err);
        }

        boolean isValid = (otpService != null) && otpService.verifyOtp(email.trim(), otp.trim());

        if (!isValid) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Invalid or expired OTP. Please verify the 6-digit code or request a new one.");
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("message", "OTP verified successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * OTP Generation Trigger for Registration:
     * Generates a secure 6-digit OTP, caches the pending registration data with a 10-minute expiry,
     * and sends the OTP to the applicant's email address.
     */
    @PostMapping(value = {"/api/auth/register/request-otp", "/api/auth/send-registration-otp"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> requestRegistrationOtp(
            @Valid @RequestBody CustomerRegistrationDto customerDto) {

        if (userService.findUserByEmail(customerDto.getEmail()).isPresent()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Email address is already registered.");
            return ResponseEntity.badRequest().body(err);
        }

        if (otpService != null) {
            otpService.generateRegistrationOtp(customerDto.getEmail(), customerDto);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("message", "A 6-digit OTP verification code has been dispatched to " + customerDto.getEmail() + ".");
        return ResponseEntity.ok(response);
    }

    /**
     * Follow-up OTP Verification Endpoint:
     * Verifies the 6-digit OTP. The user account is NOT created in the database until this exact OTP is verified.
     */
    @PostMapping(value = "/api/auth/register/verify-otp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyRegistrationOtpAndCreate(
            @RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        if (email == null || otp == null || otpService == null || !otpService.verifyRegistrationOtp(email, otp)) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Invalid or expired OTP. Please verify the 6-digit code or request a new one.");
            return ResponseEntity.badRequest().body(err);
        }

        CustomerRegistrationDto pending = otpService.getPendingRegistration(email);
        if (pending == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "No pending registration found for this email. Please submit registration details first.");
            return ResponseEntity.badRequest().body(err);
        }

        Customer savedCustomer = userService.registerCustomer(pending);
        otpService.clearOtp(email, com.driveflow.demo_driveflow.otp.OtpType.REGISTRATION);

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CREATED.value());
        response.put("success", true);
        response.put("message", "log in succes welcome to drive flow");
        response.put("customerId", savedCustomer.getSystemId());
        response.put("email", savedCustomer.getEmail());
        response.put("nicNumber", savedCustomer.getNic());
        response.put("mobileNumber", savedCustomer.getContactNumber());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Handle JSON registration submitted to /register with Content-Type: application/json.
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerCustomerJson(
            @Valid @RequestBody CustomerRegistrationDto customerDto) {
        return registerCustomerApi(customerDto);
    }

    // --- Forgot / Reset Password Endpoints (Unauthenticated OTP Flow) ---

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(@RequestParam(value = "step", required = false) String step, Model model) {
        model.addAttribute("step", step != null ? step : "email");
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/send-otp")
    public String sendForgotPasswordOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email is required.");
            return "redirect:/forgot-password";
        }
        if (userService.findUserByEmail(email.trim()).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No account found with email: " + email);
            return "redirect:/forgot-password";
        }
        if (otpService != null) {
            otpService.generatePasswordOtp(email.trim());
        }
        redirectAttributes.addFlashAttribute("email", email.trim());
        redirectAttributes.addFlashAttribute("successMessage", "A 6-digit OTP verification code has been dispatched to " + email + ". Valid for 10 minutes.");
        return "redirect:/forgot-password?step=verify";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPasswordWithOtp(@RequestParam("email") String email,
                                       @RequestParam("otp") String otp,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/forgot-password?step=verify";
        }
        if (otpService != null && !otpService.verifyPasswordOtp(email, otp)) {
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired OTP code (10-minute window). Password reset blocked.");
            return "redirect:/forgot-password?step=verify";
        }
        try {
            userService.resetPassword(email, newPassword, otp);
            redirectAttributes.addFlashAttribute("successMessage", "Your password has been reset successfully! Please log in.");
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/forgot-password?step=verify";
        }
    }

    /**
     * REST API Password Reset Endpoint:
     * Accepts email, newPassword, and 6-digit OTP code in JSON.
     * Enforces OTP gating via UserService.resetPassword(email, newPassword, otp).
     */
    @PostMapping(value = {"/api/auth/password/reset", "/api/auth/reset-password"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetPasswordApi(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        String otp = body.get("otp");

        if (email == null || email.isBlank() || newPassword == null || newPassword.isBlank() || otp == null || otp.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Email, new password, and OTP code are required.");
            return ResponseEntity.badRequest().body(err);
        }

        userService.resetPassword(email.trim(), newPassword, otp.trim());

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("message", "Password has been reset successfully. You can now log in with your new password.");
        return ResponseEntity.ok(response);
    }

    /**
     * REST API Authenticated / API Password Change Endpoint:
     * Accepts email (or current authenticated principal), oldPassword, newPassword, and 6-digit OTP code in JSON.
     * Enforces OTP gating and old password verification.
     */
    @PostMapping(value = {"/api/auth/password/change", "/api/auth/change-password", "/api/profile/password"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePasswordAuthApi(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String email = (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken))
                ? authentication.getName()
                : (body != null ? body.get("email") : null);

        if (email == null || email.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Email is required to change password.");
            return ResponseEntity.badRequest().body(err);
        }

        String oldPassword = body != null ? body.get("oldPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        String otp = body != null ? body.get("otp") : null;

        if (oldPassword == null || oldPassword.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "Current password is required.");
            return ResponseEntity.badRequest().body(err);
        }

        if (newPassword == null || newPassword.length() < 6) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "New password must be at least 6 characters long.");
            return ResponseEntity.badRequest().body(err);
        }

        if (otp == null || otp.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", "6-digit OTP verification code is required.");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            userService.changePassword(email.trim(), oldPassword, newPassword, otp.trim());
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", HttpStatus.OK.value());
            resp.put("success", true);
            resp.put("message", "Password updated successfully!");
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", HttpStatus.BAD_REQUEST.value());
            err.put("error", "Bad Request");
            err.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    /**
     * Exception Handler for Bean Validation errors in REST requests (@Valid on @RequestBody).
     * Returns HTTP 400 Bad Request with field-level error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed for registration input fields");
        body.put("fieldErrors", fieldErrors);
        body.put("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Exception Handler for business logic / regex validation failures.
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
