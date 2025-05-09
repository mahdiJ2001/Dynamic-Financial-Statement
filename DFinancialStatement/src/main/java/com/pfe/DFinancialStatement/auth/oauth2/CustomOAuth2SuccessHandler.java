package com.pfe.DFinancialStatement.auth.oauth2;

import com.pfe.DFinancialStatement.auth.entity.Role;
import com.pfe.DFinancialStatement.auth.util.JwtUtil;
import com.pfe.DFinancialStatement.auth.entity.User;
import com.pfe.DFinancialStatement.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CustomOAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        System.out.println("OAuth2 Authentication Success Handler triggered");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        System.out.println("Email extracted: " + email);

        Role role = Role.USER;

        // Create a new user if they don't exist
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            System.out.println("Creating new user for email: " + email);
            String username = generateUniqueUsernameFromEmail(email);
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setRole(role);
            return userRepository.save(newUser);
        });

        String token = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getRole().name());
        System.out.println("Generated JWT token");

        // Method 1: Use a cookie for token storage - more secure
        Cookie tokenCookie = new Cookie("auth_token", token);
        tokenCookie.setPath("/");
        tokenCookie.setHttpOnly(false); // Set to false so JavaScript can access it
        tokenCookie.setMaxAge(24 * 60 * 60); // 24 hours
        tokenCookie.setSecure(false); // Set to true in production with HTTPS
        response.addCookie(tokenCookie);

        // Method 2: Also include as URL parameter for immediate access
        // IMPORTANT: Redirect to the oauth-callback route instead of dashboard directly
        String redirectUrl = "http://localhost:4200/oauth-callback?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        System.out.println("Redirecting to: " + redirectUrl);

        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT");
        response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

        response.sendRedirect(redirectUrl);
        System.out.println("Redirect sent to client with token in cookie and URL");
    }

    private String generateUniqueUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}