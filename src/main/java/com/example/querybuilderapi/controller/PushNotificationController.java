package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for sending push notifications via FCM.
 *
 * POST /api/notifications/push/user/{accountId}  — to a specific user
 * POST /api/notifications/push/role/{role}        — to all users with a role
 * POST /api/notifications/push/broadcast          — to all registered users
 *
 * All sends are async (non-blocking). Restricted to ADMIN and MANAGER roles.
 */
@RestController
@RequestMapping("/api/notifications/push")
public class PushNotificationController {

    private final PushNotificationService pushService;

    public PushNotificationController(PushNotificationService pushService) {
        this.pushService = pushService;
    }

    public record PushRequest(String title, String body, Map<String, String> data) {}

    @PostMapping("/user/{accountId}")
    @PreAuthorize("@perms.can('PUSH_SEND')")
    public ResponseEntity<Void> pushToUser(@PathVariable Long accountId,
                                           @RequestBody PushRequest req) {
        pushService.sendToUser(accountId, req.title(), req.body(), req.data());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/role/{role}")
    @PreAuthorize("@perms.can('PUSH_SEND')")
    public ResponseEntity<Void> pushToRole(@PathVariable String role,
                                           @RequestBody PushRequest req) {
        pushService.sendToRole(role, req.title(), req.body(), req.data());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/broadcast")
    @PreAuthorize("@perms.can('NOTIFICATIONS_MANAGE')")
    public ResponseEntity<Void> broadcast(@RequestBody PushRequest req) {
        pushService.broadcast(req.title(), req.body(), req.data());
        return ResponseEntity.accepted().build();
    }
}
