package com.driveflow.demo_driveflow;

import com.driveflow.demo_driveflow.users.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/vehicles/api/**", "/maintenance/documents", "/maintenance/documents/**"))
                .authorizeHttpRequests(auth -> auth
                        // Public Read-Only Endpoints: Anyone (unauthenticated or customer) can view
                        // static assets, login, and vehicle catalog (GET only)
                        .requestMatchers(
                                "/",
                                "/booking",
                                "/incident",
                                "/login",
                                "/register",
                                "/register/**",
                                "/forgot-password",
                                "/forgot-password/**",
                                "/api/auth/**",
                                "/api/register",
                                "/api/profile/password",
                                "/api/profile/password/send-otp",
                                "/api/promotions",
                                "/api/promotions/**",
                                "/api/maintenance-companies",
                                "/api/maintenance-partners",
                                "/api/feedback/approved",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/favicon.ico")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/incidents/report").permitAll()
                        // Sales / payment data: staff only (previously public)
                        .requestMatchers("/api/company-sales", "/api/company-sales/**", "/payments/api",
                                "/payments/api/**")
                        .hasRole("STAFF")
                        // Vehicle catalog: Public strictly read-only access (GET only)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/vehicles", "/vehicles/**",
                                "/api/vehicles", "/api/vehicles/**")
                        .permitAll()
                        // Vehicle management (Staff Only): PUT, DELETE, POST
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/vehicles", "/api/vehicles/**", "/vehicles", "/vehicles/**")
                        .hasRole("STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/vehicles", "/api/vehicles/**", "/vehicles", "/vehicles/**")
                        .hasRole("STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/vehicles", "/api/vehicles/**", "/vehicles",
                                "/vehicles/**")
                        .hasRole("STAFF")
                        // Customer profile and invoices-payments (authenticated)
                        .requestMatchers("/profile/**", "/invoices-payments", "/invoices-payments/**").authenticated()
                        // Staff-only booking approval
                        .requestMatchers("/bookings/*/approve").hasRole("STAFF")
                        // Booking creation strictly requires authenticated user (Customer or Staff)
                        .requestMatchers("/bookings/new", "/bookings", "/bookings/**").authenticated()
                        // Incident logging restriction: Staff users cannot write or submit incidents.
                        // Strictly CUSTOMER only.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/incidents/report", "/incidents")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/incidents/new").hasRole("CUSTOMER")
                        .requestMatchers("/incidents/api/**").hasRole("CUSTOMER")
                        // Staff-only modules & endpoints (incident management, maintenance, payments, finance, staff, branches)
                        .requestMatchers("/incidents/**").hasRole("STAFF")
                        .requestMatchers("/maintenance/**").hasRole("STAFF")
                        .requestMatchers("/finance/**", "/finance").hasRole("STAFF")
                        .requestMatchers("/staff/**", "/staff").hasRole("STAFF")
                        .requestMatchers("/branches/**", "/branches").hasRole("STAFF")
                        .requestMatchers("/payments/new", "/payments/*/refund", "/payments/*/cancel",
                                "/payments/*/delete", "/payments/invoices/**")
                        .hasRole("STAFF")
                        .requestMatchers("/payments").authenticated()
                        // Staff Promotions Management: strictly staff-only
                        .requestMatchers("/promotions", "/promotions/**").hasRole("STAFF")
                        // Staff-only feedback visibility toggles / approval / resolve
                        .requestMatchers("/feedback/*/resolve", "/feedback/*/toggle-visibility", "/feedback/*/approve",
                                "/api/feedback/*/toggle-visibility", "/api/feedback/*/approve")
                        .hasRole("STAFF")
                        // Staff Permissions (Immutability): Staff must have strictly read-only access
                        // to feedback text.
                        // PUT requests to alter feedback text are strictly restricted to CUSTOMER role
                        // (Staff is rejected with 403 Forbidden).
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/feedback/**", "/api/feedback/**")
                        .hasRole("CUSTOMER")
                        // Feedback views and actions
                        .requestMatchers("/feedback/new", "/feedback", "/feedback/**", "/api/feedback/**")
                        .authenticated()
                        // Other views can be browsed by authenticated users
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .userDetailsService(customUserDetailsService);

        return http.build();
    }
}
