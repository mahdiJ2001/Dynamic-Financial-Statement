package com.pfe.DFinancialStatement.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Allow all requests and disable CSRF for simplicity
        http.authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll() // Allow all requests
                )
                .csrf(AbstractHttpConfigurer::disable); // Disable CSRF protection

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Return BCryptPasswordEncoder for password hashing
        return new BCryptPasswordEncoder();
    }
}
