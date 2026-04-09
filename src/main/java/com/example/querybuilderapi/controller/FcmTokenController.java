package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.service.FcmTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * FCM token registration endpoint.
 *
 * POST /api/fcm/register   — store / refresh a device's FCM token
 * DELETE /api/fcm/register — remove the token on logout
 *
 * The Firebase UID is extracted from the authenticated principal name,
 * which is set by FirebaseTokenFilter after verifying the ID token.
 */
@RestController
@RequestMapping("/api/fcm")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    public FcmTokenController(FcmTokenService fcmTokenService) {
        this.fcmTokenService = fcmTokenService;
    }

    public record RegisterTokenRequest(String token) {}

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> register(@RequestBody RegisterTokenRequest body,
                                         Authentication auth) {
        if (body.token() == null || body.token().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        fcmTokenService.registerToken(auth.getName(), body.token());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unregister(Authentication auth) {
        fcmTokenService.removeToken(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
