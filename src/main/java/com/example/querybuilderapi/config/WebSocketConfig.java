package com.example.querybuilderapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket configuration.
 *
 * Clients connect to: ws://host/ws  (SockJS fallback: http://host/ws)
 *
 * Topics published by the server:
 *   /topic/opportunities   — Opportunity create/update/delete events
 *   /topic/contacts        — Contact change events
 *   /topic/organizations   — Organization change events
 *   /topic/notifications   — User-specific push notification payloads
 *
 * Application prefix for client→server messages: /app
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Use the in-memory broker for /topic and /queue destinations
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow the React dev server and production origins
                .setAllowedOriginPatterns("*")
                // SockJS fallback for browsers without WebSocket support
                .withSockJS();
    }
}
