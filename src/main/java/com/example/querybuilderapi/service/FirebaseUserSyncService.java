package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Syncs Firebase users into the local auth_accounts table.
 *
 * On every verified Firebase request:
 *  - If the firebase_uid already exists → return the existing account.
 *  - If the email exists (legacy JWT account) → link it by setting firebase_uid.
 *  - Otherwise → create a new SALES_REP auth_account tied to the Firebase UID.
 *
 * This approach means legacy JWT/OAuth2 users are automatically upgraded when
 * they first sign in via Firebase, without losing any of their existing CRM data.
 */
@Service
public class FirebaseUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseUserSyncService.class);

    private final AuthAccountRepository authAccountRepository;

    public FirebaseUserSyncService(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    /**
     * Finds or creates an AuthAccount for the given verified Firebase token.
     *
     * @param token the verified Firebase ID token (never null)
     * @return the existing or newly created AuthAccount
     */
    @Transactional
    public AuthAccount syncUser(FirebaseToken token) {
        String firebaseUid = token.getUid();
        String email       = token.getEmail();
        String name        = token.getName() != null ? token.getName() : email;
        String photoUrl    = token.getPicture();

        // 1. Already linked?
        return authAccountRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> linkOrCreateAccount(firebaseUid, email, name, photoUrl));
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private AuthAccount linkOrCreateAccount(String firebaseUid, String email,
                                             String name, String photoUrl) {
        // 2. Legacy account with same email? Link it.
        return authAccountRepository.findByEmail(email)
                .map(existing -> linkFirebaseUid(existing, firebaseUid, photoUrl))
                .orElseGet(() -> createFirebaseAccount(firebaseUid, email, name, photoUrl));
    }

    private AuthAccount linkFirebaseUid(AuthAccount account, String firebaseUid, String photoUrl) {
        log.info("Linking existing account {} to Firebase UID {}", account.getEmail(), firebaseUid);
        account.setFirebaseUid(firebaseUid);
        account.setOauthProvider(AuthAccount.OAuthProvider.FIREBASE);
        if (photoUrl != null && account.getPhotoUrl() == null) {
            account.setPhotoUrl(photoUrl);
        }
        return authAccountRepository.save(account);
    }

    private AuthAccount createFirebaseAccount(String firebaseUid, String email,
                                               String name, String photoUrl) {
        log.info("Creating new AuthAccount for Firebase UID {} ({})", firebaseUid, email);
        AuthAccount account = new AuthAccount(email, firebaseUid, name, photoUrl);
        return authAccountRepository.save(account);
    }
}
