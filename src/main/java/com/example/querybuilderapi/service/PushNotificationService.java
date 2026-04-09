package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Async push notification delivery via FCM.
 *
 * Three delivery modes:
 *  - Individual  — by user ID, looks up the stored FCM token
 *  - Role-based  — by role name, sends to /topics/role_{role}
 *  - Broadcast   — to /topics/all_users
 *
 * All sends are @Async to avoid blocking the caller's thread.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final AuthAccountRepository authAccountRepository;
    private final FcmTokenService fcmTokenService;

    public PushNotificationService(AuthAccountRepository authAccountRepository,
                                   FcmTokenService fcmTokenService) {
        this.authAccountRepository = authAccountRepository;
        this.fcmTokenService       = fcmTokenService;
    }

    // ── Individual ────────────────────────────────────────────────────────

    /**
     * Sends a push notification to a specific user by their AuthAccount ID.
     * Uses the stored FCM registration token.
     *
     * @param accountId  the AuthAccount primary key
     * @param title      notification title
     * @param body       notification body
     * @param data       optional key-value data payload (may be null)
     */
    @Async
    public void sendToUser(Long accountId, String title, String body, Map<String, String> data) {
        authAccountRepository.findById(accountId).ifPresentOrElse(account -> {
            String token = account.getFcmToken();
            if (token == null || token.isBlank()) {
                log.debug("sendToUser: no FCM token for accountId={}", accountId);
                return;
            }

            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null) builder.putAllData(data);

            try {
                FirebaseMessaging.getInstance().send(builder.build());
                log.info("Push sent to accountId={} title='{}'", accountId, title);
            } catch (Exception e) {
                handleSendError(e, account);
            }
        }, () -> log.warn("sendToUser: account {} not found", accountId));
    }

    // ── Role-based (topic) ────────────────────────────────────────────────

    /**
     * Sends a push notification to all users with the given role via FCM topic.
     *
     * @param role  the role name (e.g. "ADMIN", "MANAGER", "SALES_REP")
     */
    @Async
    public void sendToRole(String role, String title, String body, Map<String, String> data) {
        String topic = "role_" + role.toLowerCase();
        sendToTopic(topic, title, body, data);
    }

    // ── Broadcast ─────────────────────────────────────────────────────────

    /** Broadcasts to every registered device via the all_users FCM topic. */
    @Async
    public void broadcast(String title, String body, Map<String, String> data) {
        sendToTopic("all_users", title, body, data);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null) builder.putAllData(data);

        try {
            FirebaseMessaging.getInstance().send(builder.build());
            log.info("Push sent to topic='{}' title='{}'", topic, title);
        } catch (Exception e) {
            log.error("Failed to send push to topic='{}': {}", topic, e.getMessage());
        }
    }

    /**
     * When FCM returns UNREGISTERED, the token is no longer valid — remove it
     * so we don't attempt to send to it again.
     */
    private void handleSendError(Exception e, AuthAccount account) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("UNREGISTERED") || msg.contains("registration-token-not-registered")) {
            log.warn("FCM token unregistered for accountId={} — removing", account.getId());
            if (account.getFirebaseUid() != null) {
                fcmTokenService.removeToken(account.getFirebaseUid());
            }
        } else {
            log.error("Failed to send push to accountId={}: {}", account.getId(), msg);
        }
    }
}
