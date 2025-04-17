package com.pfe.DFinancialStatement.auth.service;

import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.repository.UserRepository;
import com.pfe.DFinancialStatement.auth.dto.LoginRequestDTO;
import com.pfe.DFinancialStatement.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }


    // Authenticate the user and generate a JWT token
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequestDTO.getEmail()); // Using email for authentication

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
                // Password matches, generate JWT
                // Use toString() to convert role to a String if it's not already
                String token = jwtUtil.generateToken(user.getEmail(), user.getRole().toString());  // Ensuring the role is a String
                return Optional.of(token);
            }
        }

        return Optional.empty(); // Return empty if authentication fails
    }

    // Validate a given JWT token
    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token); // This will verify the token's validity
            return true;
        } catch (JwtException e) {
            return false; // Token validation failed
        }
    }

    // Register a new user (includes checking if the user already exists)
    public Optional<User> register(User user) {
        // Check if user already exists by email
        if (userService.getUserByEmail(user.getEmail()).isPresent()) {
            return Optional.empty(); // Return empty if user already exists
        }

        // Encrypt the password before saving it
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return Optional.of(userRepository.save(user)); // Return saved user if registration successful
    }
}
