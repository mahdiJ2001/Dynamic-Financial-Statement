package com.pfe.DFinancialStatement.auth.controller;

import com.pfe.DFinancialStatement.auth.dto.LoginRequestDTO;
import com.pfe.DFinancialStatement.auth.dto.LoginResponseDTO;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Authentification d'un utilisateur et génération d'un JWT
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);

        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Unauthorized if no token
        }

        String token = tokenOptional.get();
        return ResponseEntity.ok(new LoginResponseDTO(token)); // Return the generated token
    }

    // Valider un token JWT
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {
        // Authorization: Bearer <token>
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Unauthorized if header is missing or invalid
        }

        boolean isValid = authService.validateToken(authHeader.substring(7)); // Check token validity
        return isValid ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Return appropriate status
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        Optional<User> newUserOptional = authService.register(user);

        if (newUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Bad Request if user already exists
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(newUserOptional.get()); // Return the newly created user
    }
}
