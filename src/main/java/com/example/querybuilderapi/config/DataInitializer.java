package com.example.querybuilderapi.config;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds initial data for the hobby project:
 * - A default admin user (admin@example.com / password)
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(AuthAccountRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (!repository.existsByEmail("admin@example.com")) {
                AuthAccount admin = new AuthAccount(
                    "admin@example.com",
                    encoder.encode("password"),
                    "Hobby Admin",
                    AuthAccount.Role.ADMIN,
                    AuthAccount.OAuthProvider.LOCAL,
                    null
                );
                repository.save(admin);
                log.info("✅ Created default admin user: admin@example.com / password");
            } else {
                log.info("ℹ️ Default admin user already exists.");
            }
        };
    }
}
