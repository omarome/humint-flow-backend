package com.example.querybuilderapi.service;

import com.example.querybuilderapi.exception.AccountNotInvitedException;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Workspace;
import com.example.querybuilderapi.model.WorkspaceMembership;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.WorkspaceMembershipRepository;
import com.example.querybuilderapi.repository.WorkspaceRepository;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Syncs Firebase users into the local {@code auth_accounts} table.
 *
 * On every verified Firebase request the lookup order is:
 *   1. {@code firebase_uid} already linked  → return the existing account (fast path).
 *   2. Email matches a pre-provisioned (invited) account  → link the UID, activate the
 *      account, and return it.  This is how an invited user gets their first session.
 *   3. No match, but the token is a Google sign-in  → auto-provision a read-only
 *      {@code VIEWER} account in the {@code default} workspace. Anyone can arrive with
 *      a Google account, so this is the "look around" tier, not a write-capable one.
 *   4. No match, any other sign-in method (email/password, etc.)  → throw
 *      {@link AccountNotInvitedException}. Self-service account creation via anything
 *      other than Google is still invite-only.
 */
@Service
public class FirebaseUserSyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseUserSyncService.class);

    private static final String DEFAULT_WORKSPACE_SLUG = "default";

    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final FirebaseClaimsService firebaseClaimsService;

    public FirebaseUserSyncService(AuthAccountRepository authAccountRepository,
                                   WorkspaceRepository workspaceRepository,
                                   WorkspaceMembershipRepository workspaceMembershipRepository,
                                   FirebaseClaimsService firebaseClaimsService) {
        this.authAccountRepository = authAccountRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.firebaseClaimsService  = firebaseClaimsService;
    }

    /**
     * Resolves the {@link AuthAccount} for the given verified Firebase ID token.
     *
     * @param token the verified Firebase ID token (never null)
     * @return the linked, newly activated, or newly auto-provisioned AuthAccount
     * @throws AccountNotInvitedException if no pre-provisioned account exists for the
     *         email and the sign-in method isn't Google
     */
    @Transactional
    public AuthAccount syncUser(FirebaseToken token) {
        String firebaseUid = token.getUid();

        // Fast path — already linked by UID
        return authAccountRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> linkOrProvisionAccount(token));
    }

    // ─── Private helpers ─────────────────────────────────────────────────

    private AuthAccount linkOrProvisionAccount(FirebaseToken token) {
        String email = token.getEmail();

        return authAccountRepository.findByEmail(email)
                .map(account -> linkInvitedAccount(account, token))
                .orElseGet(() -> {
                    if (isGoogleSignIn(token)) {
                        return provisionViewerAccount(token);
                    }
                    log.warn("Firebase sign-in blocked for '{}' — no invited account found.", email);
                    throw new AccountNotInvitedException(email);
                });
    }

    /**
     * Links a pre-provisioned account by email to the Firebase UID that just
     * signed in with it, and activates it.
     */
    private AuthAccount linkInvitedAccount(AuthAccount account, FirebaseToken token) {
        String firebaseUid = token.getUid();
        String photoUrl    = token.getPicture();

        log.info("Linking invited account '{}' to Firebase UID {}", account.getEmail(), firebaseUid);

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

    /**
     * Auto-provisions a read-only VIEWER account (+ a matching {@code default}-workspace
     * membership, since {@code WorkspaceResolutionFilter} rejects any non-SUPER_ADMIN
     * account with no membership row) for a first-time Google sign-in with no
     * pre-existing invite.
     */
    private AuthAccount provisionViewerAccount(FirebaseToken token) {
        String email = token.getEmail();
        log.info("Auto-provisioning VIEWER account for first-time Google sign-in: {}", email);

        AuthAccount account = new AuthAccount();
        account.setEmail(email);
        account.setDisplayName(token.getName() != null ? token.getName() : email);
        account.setRole(AuthAccount.Role.VIEWER);
        account.setOauthProvider(AuthAccount.OAuthProvider.FIREBASE);
        account.setIsActive(true);
        account.setFirebaseUid(token.getUid());
        account.setPhotoUrl(token.getPicture());
        account = authAccountRepository.save(account);

        Workspace defaultWorkspace = workspaceRepository.findBySlug(DEFAULT_WORKSPACE_SLUG)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot auto-provision — '" + DEFAULT_WORKSPACE_SLUG + "' workspace not found"));
        workspaceMembershipRepository.save(
                new WorkspaceMembership(defaultWorkspace, account, AuthAccount.Role.VIEWER));

        firebaseClaimsService.syncClaims(account.getId());

        return account;
    }

    /**
     * True when the verified token's sign-in method was Google (as opposed to
     * email/password, GitHub, etc.) — read from the standard Firebase
     * {@code firebase.sign_in_provider} claim.
     */
    private boolean isGoogleSignIn(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof Map<?, ?> map) {
            return "google.com".equals(map.get("sign_in_provider"));
        }
        return false;
    }
}
