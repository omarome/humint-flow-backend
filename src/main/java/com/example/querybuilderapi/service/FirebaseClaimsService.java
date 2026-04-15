package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages Firebase Custom Claims so that the client's ID token always
 * reflects the role stored in PostgreSQL.
 *
 * Custom claims are used in:
 *   - Spring Security (backend): FirebaseTokenFilter reads the verified token's claims
 *   - Firestore Security Rules (client): request.auth.token.role
 *
 * Claims are intentionally minimal — only {@code role} and {@code teamId}.
 * Never put sensitive data (passwords, PII) in claims; they are readable
 * by anyone who holds the ID token.
 *
 * After updating claims the client must call
 * {@code auth.currentUser.getIdToken(true)} to pick up the new claims
 * without re-logging in.
 */
@Service
public class FirebaseClaimsService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseClaimsService.class);

    private final AuthAccountRepository authAccountRepository;

    public FirebaseClaimsService(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    /**
     * Sets custom claims on the Firebase user whose UID matches the given
     * {@code authAccount.firebaseUid}, then persists the role change to
     * PostgreSQL in the same operation.
     *
     * @param accountId  The PostgreSQL {@code auth_accounts.id} to update.
     * @param newRole    The new role to assign (ADMIN | MANAGER | SALES_REP | USER).
     * @param teamId     Optional team identifier (null is fine).
     * @throws IllegalArgumentException if account not found.
     * @throws IllegalStateException    if the account has no Firebase UID.
     */
    public void setUserRole(Long accountId, AuthAccount.Role newRole, Long teamId) {
        AuthAccount account = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("AuthAccount not found: " + accountId));

        // Persist role change to PostgreSQL first (source of truth)
        account.setRole(newRole);
        authAccountRepository.save(account);

        // Push claims to Firebase (best-effort — Firebase may be unavailable in dev)
        pushClaimsToFirebase(account.getFirebaseUid(), newRole, teamId, null);

        log.info("Updated role for account {} ({}) to {}", accountId, account.getEmail(), newRole);
    }

    /**
     * Re-syncs the Firebase custom claims for an account whose role
     * already matches the PostgreSQL record. Useful after a migration or
     * when claims are suspected to be stale.
     */
    public void syncClaims(Long accountId) {
        AuthAccount account = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("AuthAccount not found: " + accountId));
        pushClaimsToFirebase(account.getFirebaseUid(), account.getRole(), null, null);
    }

    /**
     * Re-syncs Firebase claims including the active workspace ID.
     * Called after workspace creation, member role change, and workspace switch.
     *
     * @param accountId   The auth_accounts.id to update.
     * @param workspaceId The workspace to set as activeWorkspaceId in claims.
     */
    public void syncClaimsWithWorkspace(Long accountId, Long workspaceId) {
        AuthAccount account = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("AuthAccount not found: " + accountId));
        pushClaimsToFirebase(account.getFirebaseUid(), account.getRole(), null, workspaceId);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private void pushClaimsToFirebase(String firebaseUid, AuthAccount.Role role, Long teamId, Long workspaceId) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            log.debug("Skipping Firebase claims update — no Firebase UID on account.");
            return;
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        if (teamId != null)      claims.put("teamId", teamId);
        if (workspaceId != null) claims.put("activeWorkspaceId", workspaceId);

        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                FirebaseAuth.getInstance().setCustomUserClaims(firebaseUid, claims);
                log.info("Firebase custom claims updated for UID {}: role={}, activeWorkspaceId={}",
                         firebaseUid, role, workspaceId);
            } else {
                log.warn("Firebase Admin SDK not initialized — skipping claims update for UID {}", firebaseUid);
            }
        } catch (FirebaseAuthException e) {
            log.error("Failed to update Firebase custom claims for UID {}: {}", firebaseUid, e.getMessage());
        }
    }
}
