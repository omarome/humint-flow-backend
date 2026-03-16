package com.example.querybuilderapi.config;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
                // Note: createdAt and updatedAt are handled by @PrePersist in the entity
                repository.save(admin);
            } else {
                log.info("Admin user {} already exists, skipping seeding.", adminEmail);
            }
        };
    }
}
