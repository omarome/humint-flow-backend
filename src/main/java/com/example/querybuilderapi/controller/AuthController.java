package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.*;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication: register, login, refresh, logout, me.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register — create a new local account.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login — authenticate with email + password.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/refresh — exchange refresh token for a new token pair.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            AuthResponse response = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/logout — revoke the refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/oauth2/google — Redirect to the social login provider.
     */
    @GetMapping("/oauth2/google")
    public void redirectToGoogle(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        // In a real Spring Security OAuth2 setup, this is usually handled by the filter chain 
        // at /oauth2/authorization/google. This endpoint provides a consistent /api/* interface.
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * GET /api/auth/oauth2/callback — Example callback if using manual flow.
     * Note: Spring Security's OAuth2 client usually handles the callback at /login/oauth2/code/* automatically.
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam String code) {
        // This is a placeholder for custom callback logic if not using Spring's default handler.
        return ResponseEntity.ok(Map.of("message", "Callback received with code: " + code));
    }

    /**
     * GET /api/auth/me — return the currently authenticated user's info.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthAccount account) {
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole().name()
        );
        return ResponseEntity.ok(userInfo);
    }
}
