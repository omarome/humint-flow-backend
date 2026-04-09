package com.example.querybuilderapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises the Firebase Admin SDK exactly once at application startup.
 *
 * Credential resolution order:
 *  1. GOOGLE_APPLICATION_CREDENTIALS env var pointing to a service account JSON file (local dev).
 *  2. Application Default Credentials (ADC) — automatic on Google Cloud Run.
 *  3. If neither is available (unit tests / CI without Firebase), logs a warning and skips init.
 *     The FirebaseTokenFilter will then skip token verification gracefully.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initializeFirebase() {
        // Only initialize once — guards against double-init in dev with hot reload.
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase Admin SDK already initialized — skipping.");
            return;
        }

        try {
            GoogleCredentials credentials = resolveCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully.");
        } catch (IOException e) {
            // Non-fatal: allows the app to start without Firebase (useful for local dev / tests).
            log.warn("Firebase Admin SDK could not be initialized — Firebase auth will be disabled. " +
                     "Set GOOGLE_APPLICATION_CREDENTIALS to enable it. Reason: {}", e.getMessage());
        }
    }

    /**
     * Resolves Firebase credentials:
     *  - If GOOGLE_APPLICATION_CREDENTIALS points to a file, load it directly.
     *  - Otherwise, fall back to ADC (works automatically on Cloud Run).
     */
    private GoogleCredentials resolveCredentials() throws IOException {
        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credPath != null && !credPath.isBlank()) {
            log.info("Loading Firebase credentials from: {}", credPath);
            try (InputStream serviceAccount = new FileInputStream(credPath)) {
                return GoogleCredentials.fromStream(serviceAccount)
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }
        }
        log.info("No GOOGLE_APPLICATION_CREDENTIALS set — using Application Default Credentials (ADC).");
        return GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }
}
