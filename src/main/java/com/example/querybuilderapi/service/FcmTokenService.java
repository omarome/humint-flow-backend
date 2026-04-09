package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Manages FCM registration tokens for push notification delivery.
 *
 * Responsibilities:
 *  - Register / refresh a token for an authenticated user (called on app start and onTokenRefresh)
 *  - Remove a token on logout or when FCM returns UNREGISTERED
 *  - Flag tokens not updated in 30+ days as potentially stale (scheduled job)
 *  - Auto-subscribe users to FCM topics that match their role and team
 */
@Service
public class FcmTokenService {

    private static final Logger log = LoggerFactory.getLogger(FcmTokenService.class);
    private static final int STALE_DAYS = 30;

    private final AuthAccountRepository authAccountRepository;

    public FcmTokenService(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    // ── Token Registration ────────────────────────────────────────────────

    /**
     * Registers or refreshes the FCM token for an authenticated user.
     * Also subscribes the token to topic channels for the user's role and team.
     *
     * @param firebaseUid the Firebase UID from the verified JWT
     * @param token       the FCM registration token sent by the client
     */
    @Transactional
    public void registerToken(String firebaseUid, String token) {
        authAccountRepository.findByFirebaseUid(firebaseUid).ifPresentOrElse(account -> {
            String previous = account.getFcmToken();
            account.setFcmToken(token);
            account.setFcmTokenUpdatedAt(Instant.now());
            authAccountRepository.save(account);

            // Unsubscribe old token from topics if it changed
            if (previous != null && !previous.equals(token)) {
                unsubscribeFromTopics(previous, account);
            }
            subscribeToTopics(token, account);

            log.info("FCM token registered for uid={}", firebaseUid);
        }, () -> log.warn("registerToken: no account found for uid={}", firebaseUid));
    }

    /**
     * Removes the FCM token on logout or after an UNREGISTERED error from FCM.
     */
    @Transactional
    public void removeToken(String firebaseUid) {
        authAccountRepository.findByFirebaseUid(firebaseUid).ifPresent(account -> {
            String token = account.getFcmToken();
            if (token != null) {
                unsubscribeFromTopics(token, account);
                account.setFcmToken(null);
                account.setFcmTokenUpdatedAt(null);
                authAccountRepository.save(account);
                log.info("FCM token removed for uid={}", firebaseUid);
            }
        });
    }

    // ── Topic Management ──────────────────────────────────────────────────

    /**
     * Subscribes a token to role- and team-based FCM topics.
     * Topics: /topics/role_{ROLE}, /topics/all_users
     */
    public void subscribeToTopics(String token, AuthAccount account) {
        try {
            String roleTopic = "role_" + account.getRole().name().toLowerCase();
            FirebaseMessaging.getInstance().subscribeToTopic(List.of(token), roleTopic);
            FirebaseMessaging.getInstance().subscribeToTopic(List.of(token), "all_users");
            log.debug("Subscribed token to topics: {}, all_users", roleTopic);
        } catch (Exception e) {
            log.warn("Failed to subscribe token to topics: {}", e.getMessage());
        }
    }

    public void unsubscribeFromTopics(String token, AuthAccount account) {
        try {
            String roleTopic = "role_" + account.getRole().name().toLowerCase();
            FirebaseMessaging.getInstance().unsubscribeFromTopic(List.of(token), roleTopic);
            FirebaseMessaging.getInstance().unsubscribeFromTopic(List.of(token), "all_users");
        } catch (Exception e) {
            log.debug("Failed to unsubscribe from topics (token may already be invalid): {}", e.getMessage());
        }
    }

    // ── Stale Token Cleanup ───────────────────────────────────────────────

    /**
     * Runs nightly at 02:00. Logs a warning for FCM tokens not refreshed in 30+ days.
     * Stale tokens are not automatically removed — they remain until FCM returns
     * UNREGISTERED on next send, at which point removeToken() should be called.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void flagStaleTokens() {
        Instant cutoff = Instant.now().minus(STALE_DAYS, ChronoUnit.DAYS);
        List<AuthAccount> stale = authAccountRepository.findAll().stream()
                .filter(a -> a.getFcmToken() != null)
                .filter(a -> a.getFcmTokenUpdatedAt() == null || a.getFcmTokenUpdatedAt().isBefore(cutoff))
                .toList();

        if (!stale.isEmpty()) {
            log.warn("FCM stale tokens: {} account(s) have not refreshed their FCM token in {}+ days",
                    stale.size(), STALE_DAYS);
            stale.forEach(a -> log.warn("  Stale token account: id={} email={}", a.getId(), a.getEmail()));
        }
    }
}
