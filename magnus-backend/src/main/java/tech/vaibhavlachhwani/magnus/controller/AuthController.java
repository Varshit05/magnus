package tech.vaibhavlachhwani.magnus.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import tech.vaibhavlachhwani.magnus.dto.AuthRequest;
import tech.vaibhavlachhwani.magnus.dto.AuthResponse;
import tech.vaibhavlachhwani.magnus.service.UserService;
import tech.vaibhavlachhwani.magnus.util.JwtUtil;

/**
 * REST controller exposing the login endpoint.
 * It authenticates the user via Spring Security's AuthenticationManager,
 * then issues a JWT using the existing JwtUtil helper.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /auth/login
     * Body: { "username": "...", "password": "..." }
     * Returns: { "token": "<jwt>", "roles": "ROLE_ADMIN,ROLE_USER" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // Spring will throw AuthenticationException if credentials are invalid
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            var userOpt = userService.findByUsername(request.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }

            // Generate JWT containing username and roles
            String token = jwtUtil.generateToken(request.getUsername(), userOpt.get().getRoles());
            // Join role names into a CSV string for the front‑end convenience
            String rolesCsv = userOpt.get().getRoles()
                                   .stream()
                                   .map(Enum::name)
                                   .reduce((a, b) -> a + "," + b)
                                   .orElse("");

            AuthResponse body = new AuthResponse();
            body.setToken(token);
            body.setUsername(request.getUsername());
            body.setRoles(rolesCsv.isEmpty() ? new String[0] : rolesCsv.split(","));
            return ResponseEntity.ok(body);
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }
}
