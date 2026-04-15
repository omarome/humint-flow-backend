package com.example.querybuilderapi.service;

import com.example.querybuilderapi.exception.AccountNotInvitedException;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Syncs Firebase users into the local {@code auth_accounts} table.
 *
 * On every verified Firebase request the lookup order is:
 *   1. {@code firebase_uid} already linked  → return the existing account (fast path).
 *   2. Email matches a pre-provisioned (invited) account  → link the UID, activate the
 *      account, and return it.  This is how an invited user gets their first session.
 *   3. No match at all  → throw {@link AccountNotInvitedException}.
 *      Auto-creating SALES_REP accounts on first sign-in is intentionally disabled.
 *      An ADMIN must call {@code POST /api/admin/invite} first.
 */
@Service
public class FirebaseUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseUserSyncService.class);

    private final AuthAccountRepository authAccountRepository;
    private final FirebaseClaimsService firebaseClaimsService;

    public FirebaseUserSyncService(AuthAccountRepository authAccountRepository,
                                   FirebaseClaimsService firebaseClaimsService) {
        this.authAccountRepository = authAccountRepository;
        this.firebaseClaimsService  = firebaseClaimsService;
    }

    /**
     * Resolves the {@link AuthAccount} for the given verified Firebase ID token.
     *
     * @param token the verified Firebase ID token (never null)
     * @return the linked or newly activated AuthAccount
     * @throws AccountNotInvitedException if no pre-provisioned account exists for the email
     */
    @Transactional
    public AuthAccount syncUser(FirebaseToken token) {
        String firebaseUid = token.getUid();
        String email       = token.getEmail();
        String photoUrl    = token.getPicture();

        // Fast path — already linked by UID
        return authAccountRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> linkInvitedAccount(firebaseUid, email, photoUrl));
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    /**
     * Finds a pre-provisioned account by email and links it to the Firebase UID.
     * Throws {@link AccountNotInvitedException} if no matching invited account exists —
     * we no longer auto-create accounts on first sign-in.
     */
    private AuthAccount linkInvitedAccount(String firebaseUid, String email, String photoUrl) {
        AuthAccount account = authAccountRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Firebase sign-in blocked for '{}' — no invited account found.", email);
                    return new AccountNotInvitedException(email);
                });

        log.info("Linking invited account '{}' to Firebase UID {}", email, firebaseUid);

        account.setFirebaseUid(firebaseUid);
        account.setOauthProvider(AuthAccount.OAuthProvider.FIREBASE);
        account.setIsActive(true);          // activate the pending invite
        if (photoUrl != null && account.getPhotoUrl() == null) {
            account.setPhotoUrl(photoUrl);
        }
        account = authAccountRepository.save(account);

        // Push the role into Firebase custom claims now that we have the UID
        firebaseClaimsService.syncClaims(account.getId());

        return account;
    }
}
