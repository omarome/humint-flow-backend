package com.example.querybuilderapi.config;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Notification;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds initial data for the hobby project:
 * - A default admin user based on environment variables
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initData(AuthAccountRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (repository.findByEmail(adminEmail).isEmpty()) {
                log.info("Seeding default admin user: {}", adminEmail);
                AuthAccount admin = new AuthAccount();
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setDisplayName("Hobby Admin");
                admin.setRole(AuthAccount.Role.ADMIN);
                admin.setOauthProvider(AuthAccount.OAuthProvider.LOCAL);
                repository.save(admin);
            } else {
                log.info("Admin user {} already exists, skipping seeding.", adminEmail);
            }
        };
    }

    @Bean
    public CommandLineRunner initNotifications(NotificationRepository notificationRepository) {
        return args -> {
            if (notificationRepository.count() == 0) {
                log.info("Seeding initial notifications...");
                notificationRepository.saveAll(List.of(
                    buildNotification("New Feature Available",
                        "Check out the new Saved Filters sidebar to quickly access your favorite queries.",
                        "info", false, LocalDateTime.now().minusMinutes(30)),
                    buildNotification("Authentication Required",
                        "Your session expires in 2 hours. Please re-authenticate if you plan to continue working.",
                        "warning", false, LocalDateTime.now().minusDays(1)),
                    buildNotification("Welcome to Smart Filter Hub",
                        "Explore the advanced query builder to construct complex filters visually.",
                        "info", true, LocalDateTime.now().minusDays(2))
                ));
            } else {
                log.info("Notifications already seeded, skipping.");
            }
        };
    }

    private Notification buildNotification(String title, String message, String type, boolean isRead, LocalDateTime timestamp) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setIsRead(isRead);
        n.setTimestamp(timestamp);
        return n;
    }
}
