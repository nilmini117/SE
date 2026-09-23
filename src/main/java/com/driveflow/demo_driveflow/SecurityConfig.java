package com.driveflow.demo_driveflow;

import com.driveflow.demo_driveflow.users.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
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
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/vehicles",
                    "/promotions",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                // Customer profile (authenticated; staff is redirected to / in ProfileController)
                .requestMatchers("/profile/**").authenticated()
                // Staff-only booking approval
                .requestMatchers("/bookings/*/approve").hasRole("STAFF")
                // Booking creation strictly requires authenticated user (Customer or Staff)
                .requestMatchers("/bookings/new", "/bookings", "/bookings/**").authenticated()
                // Staff-only modules & endpoints
                .requestMatchers("/incidents/**").hasRole("STAFF")
                .requestMatchers("/maintenance/**").hasRole("STAFF")
                .requestMatchers("/payments/new", "/payments/*/refund", "/payments/*/delete", "/payments/invoices/**").hasRole("STAFF")
                .requestMatchers("/payments").authenticated()
                .requestMatchers("/vehicles/new", "/vehicles/*/edit", "/vehicles/*/delete").hasRole("STAFF")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/promotions", "/promotions/**").hasRole("STAFF")
                .requestMatchers("/promotions/new", "/promotions/*/edit", "/promotions/*/delete").hasRole("STAFF")
                .requestMatchers("/feedback/*/resolve", "/feedback/*/delete").hasRole("STAFF")
                // Other views can be browsed by authenticated users
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .userDetailsService(customUserDetailsService);

        return http.build();
    }
}
