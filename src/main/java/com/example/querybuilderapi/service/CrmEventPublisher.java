package com.example.querybuilderapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Broadcasts real-time entity change events to WebSocket subscribers.
 *
 * Events are sent to /topic/{entityType} (e.g. /topic/opportunities).
 * Each payload includes the entity type, the changed record's ID,
 * and the change action (CREATED | UPDATED | DELETED).
 *
 * Call these methods from the service layer after successful DB writes.
 */
@Service
public class CrmEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CrmEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public CrmEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishCreated(String entityType, Object id) {
        publish(entityType, id, "CREATED");
    }

    public void publishUpdated(String entityType, Object id) {
        publish(entityType, id, "UPDATED");
    }

    public void publishDeleted(String entityType, Object id) {
        publish(entityType, id, "DELETED");
    }

    private void publish(String entityType, Object id, String action) {
        String destination = "/topic/" + entityType.toLowerCase() + "s";
        Map<String, Object> payload = Map.of(
                "entityType", entityType,
                "id",         id,
                "action",     action
        );
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("WS event: {} {} → {}", action, entityType, id);
        } catch (Exception e) {
            log.warn("Failed to publish WS event: {}", e.getMessage());
        }
    }
}
