package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.Notification;
import com.example.querybuilderapi.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoint for Notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("@perms.can('NOTIFICATIONS_READ')")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("@perms.can('NOTIFICATIONS_READ')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("@perms.can('NOTIFICATIONS_READ')")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.can('NOTIFICATIONS_READ')")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("@perms.can('NOTIFICATIONS_MANAGE')")
    public ResponseEntity<Void> deleteAllNotifications() {
        notificationService.deleteAllNotifications();
        return ResponseEntity.noContent().build();
    }
    
    // Helper endpoint for testing/seeding a new notification manually. Admin-only.
    @PostMapping
    @PreAuthorize("@perms.can('NOTIFICATIONS_MANAGE')")
    public ResponseEntity<Notification> createMockNotification(
            @RequestParam String title, 
            @RequestParam String message, 
            @RequestParam String type) {
        return ResponseEntity.ok(notificationService.createNotification(title, message, type));
    }
}
