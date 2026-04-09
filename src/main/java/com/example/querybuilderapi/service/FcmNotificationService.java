package com.example.querybuilderapi.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service to handle Firebase Cloud Messaging (FCM) operations natively
 * using the Firebase Admin SDK.
 * Note: FirebaseApp initialization is assumed to be handled elsewhere 
 * (e.g. FirebaseConfig or similar).
 */
import com.google.firebase.messaging.MessagingErrorCode;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class FcmNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationService.class);

    private final AuthAccountRepository authAccountRepository;

    @Autowired
    public FcmNotificationService(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    /**
     * Send a notification to a specific device token.
     *
     * @param token The FCM device token
     * @param title The notification title
     * @param body  The notification body
     * @param data  Optional key-value pairs to send directly to the app
     */
    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        log.info("Sending FCM message to token: {}", token);

        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token: {}", token, e);
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.info("Token {} is unregistered. Removing from database.", token);
                authAccountRepository.findByFcmToken(token).ifPresent(account -> {
                    account.setFcmToken(null);
                    authAccountRepository.save(account);
                });
            }
        }
    }

    /**
     * Send a notification to all devices subscribed to a topic.
     *
     * @param topic The topic name (e.g., "all-users", "manager-alerts")
     * @param title The notification title
     * @param body  The notification body
     * @param data  Optional key-value pairs
     */
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        log.info("Sending FCM message to topic: {}", topic);

        Message.Builder messageBuilder = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Successfully sent message to topic {}: {}", topic, response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to topic: {}", topic, e);
        }
    }

    /**
     * Send a batch of notifications to multiple tokens simultaneously.
     *
     * @param tokens List of FCM device tokens
     * @param title  The notification title
     * @param body   The notification body
     * @param data   Optional key-value pairs
     */
    public void sendBatch(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.warn("Cannot send batch FCM message. Token list is empty.");
            return;
        }

        log.info("Sending batch FCM message to {} tokens", tokens.size());

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build());
            log.info("Successfully sent {} active messages. Failed: {}", response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send batch FCM messages.", e);
        }
    }
}
