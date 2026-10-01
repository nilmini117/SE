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

        Customer savedCustomer = userService.registerCustomer(customerDto);

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
