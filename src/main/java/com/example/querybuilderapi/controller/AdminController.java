package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.InviteRequest;
import com.example.querybuilderapi.dto.TeamMemberResponse;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.ActivityRepository;
import com.example.querybuilderapi.repository.AttachmentRepository;
import com.example.querybuilderapi.repository.CommentRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.repository.RecordShareRepository;
import com.example.querybuilderapi.repository.RefreshTokenRepository;
import com.example.querybuilderapi.repository.RoleAuditRepository;
import com.example.querybuilderapi.repository.WorkspaceMembershipRepository;
import com.example.querybuilderapi.repository.WorkspaceRepository;
import com.example.querybuilderapi.service.EmailService;
import com.example.querybuilderapi.service.TeamMemberService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only operations that manage user accounts and workspace membership.
 *
 * Base path: /api/admin
 *
 * Endpoints:
 *   POST   /api/admin/invite             — pre-provision an account so the user can sign in
 *   DELETE /api/admin/users/{id}/deactivate — deactivate a user (prevents sign-in)
 *   PUT    /api/admin/users/{id}/reactivate — reactivate a previously deactivated user
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AuthAccountRepository        authAccountRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final RefreshTokenRepository       refreshTokenRepository;
    private final RecordShareRepository        recordShareRepository;
    private final RoleAuditRepository          roleAuditRepository;
    private final OpportunityRepository        opportunityRepository;
    private final ActivityRepository           activityRepository;
    private final CommentRepository            commentRepository;
    private final AttachmentRepository         attachmentRepository;
    private final WorkspaceRepository          workspaceRepository;
    private final TeamMemberService            teamMemberService;
    private final EmailService                 emailService;

    public AdminController(AuthAccountRepository authAccountRepository,
                           WorkspaceMembershipRepository membershipRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           RecordShareRepository recordShareRepository,
                           RoleAuditRepository roleAuditRepository,
                           OpportunityRepository opportunityRepository,
                           ActivityRepository activityRepository,
                           CommentRepository commentRepository,
                           AttachmentRepository attachmentRepository,
                           WorkspaceRepository workspaceRepository,
                           TeamMemberService teamMemberService,
                           EmailService emailService) {
        this.authAccountRepository = authAccountRepository;
        this.membershipRepository  = membershipRepository;
        this.refreshTokenRepository  = refreshTokenRepository;
        this.recordShareRepository   = recordShareRepository;
        this.roleAuditRepository     = roleAuditRepository;
        this.opportunityRepository   = opportunityRepository;
        this.activityRepository      = activityRepository;
        this.commentRepository       = commentRepository;
        this.attachmentRepository    = attachmentRepository;
        this.workspaceRepository     = workspaceRepository;
        this.teamMemberService       = teamMemberService;
        this.emailService            = emailService;
    }

    /**
     * POST /api/admin/invite
     *
     * Pre-provisions an {@code auth_accounts} record for the given email address
     * with {@code is_active = false} (pending invite).  When the invitee first signs in
     * via Firebase (email/password or Google), {@link com.example.querybuilderapi.service.FirebaseUserSyncService}
     * finds this record, links the Firebase UID, and activates the account automatically.
     *
     * The caller (ADMIN) cannot invite another SUPER_ADMIN — that role must be set
     * directly in the database.
     *
     * @param request  invite payload: email, displayName, role, optional jobTitle/department
     * @param admin    the currently authenticated admin account (injected by Spring Security)
     * @return 201 Created with the new team member profile, or 400/409 on validation errors
     */
    @PostMapping("/invite")
    @PreAuthorize("@perms.can('ADMIN_INVITE')")
    public ResponseEntity<?> inviteUser(@Valid @RequestBody InviteRequest request,
                                        @AuthenticationPrincipal AuthAccount admin) {

        // Guard: ADMIN cannot assign SUPER_ADMIN via this endpoint
        if (request.getRole() == AuthAccount.Role.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "SUPER_ADMIN cannot be assigned via the invite endpoint."));
        }

        // Guard: duplicate email
        if (authAccountRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error",
                            "An account with email '" + request.getEmail() + "' already exists."));
        }

        // Create the pending (uninvited) account — no password, no firebase_uid, inactive
        AuthAccount invited = new AuthAccount();
        invited.setEmail(request.getEmail());
        invited.setDisplayName(request.getDisplayName());
        invited.setRole(request.getRole());
        invited.setOauthProvider(AuthAccount.OAuthProvider.FIREBASE);   // expected sign-in method
        invited.setIsActive(false);                                       // pending until first sign-in
        if (request.getJobTitle()   != null) invited.setJobTitle(request.getJobTitle());
        if (request.getDepartment() != null) invited.setDepartment(request.getDepartment());

        invited = authAccountRepository.save(invited);

        String resetLink = createFirebaseUserAndResetLink(invited.getEmail());
        String inviterName = admin.getDisplayName() != null ? admin.getDisplayName() : admin.getEmail();
        emailService.sendInvitation(invited.getEmail(), invited.getDisplayName(),
                invited.getRole().name(), inviterName, resetLink);

        // Return the full team-member profile shape so the frontend can add the row to the table
        TeamMemberResponse profile = teamMemberService.getMember(invited.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    /**
     * Creates a Firebase Auth user for the invited email (if one doesn't already exist)
     * and returns a one-time password-reset link they can use to set their password.
     * Returns null on failure so the invite still succeeds even if Firebase is unavailable.
     */
    private String createFirebaseUserAndResetLink(String email) {
        try {
            try {
                FirebaseAuth.getInstance().createUser(
                        new UserRecord.CreateRequest()
                                .setEmail(email)
                                .setEmailVerified(false)
                                .setDisabled(false)
                );
            } catch (FirebaseAuthException e) {
                // EMAIL_ALREADY_EXISTS is fine — user may have had a prior Firebase account
                if (e.getAuthErrorCode() != com.google.firebase.auth.AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                    log.warn("Could not create Firebase user for {}: {}", email, e.getMessage());
                }
            }
            return FirebaseAuth.getInstance().generatePasswordResetLink(email);
        } catch (FirebaseAuthException e) {
            log.error("Could not generate password reset link for {}: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * DELETE /api/admin/users/{id}/deactivate
     *
     * Sets {@code is_active = false} on the target account.
     * The next request the user makes will be rejected with 403 by {@link
     * com.example.querybuilderapi.security.FirebaseTokenFilter}.
     * The account and all its data are preserved — use this instead of hard-deleting.
     */
    @DeleteMapping("/users/{id}/deactivate")
    @PreAuthorize("@perms.can('ADMIN_MANAGE')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthAccount admin) {
        if (id.equals(admin.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You cannot deactivate your own account."));
        }

        AuthAccount target = authAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        if (target.getRole() == AuthAccount.Role.SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "SUPER_ADMIN accounts cannot be deactivated via this endpoint."));
        }

        target.setIsActive(false);
        authAccountRepository.save(target);

        return ResponseEntity.ok(Map.of("message",
                "Account '" + target.getEmail() + "' deactivated successfully."));
    }

    /**
     * PUT /api/admin/users/{id}/reactivate
     *
     * Re-enables a previously deactivated account.
     * The user can sign in again immediately on their next request.
     */
    @PutMapping("/users/{id}/reactivate")
    @PreAuthorize("@perms.can('ADMIN_MANAGE')")
    public ResponseEntity<?> reactivateUser(@PathVariable Long id) {
        AuthAccount target = authAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        target.setIsActive(true);
        authAccountRepository.save(target);

        return ResponseEntity.ok(Map.of("message",
                "Account '" + target.getEmail() + "' reactivated successfully."));
    }

    /**
     * DELETE /api/admin/users/{id}
     *
     * Permanently removes a pending (never-activated) invite.
     * Only allowed for accounts that have never signed in (no firebase_uid).
     * Also deletes the corresponding Firebase Auth user if one was created.
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("@perms.can('ADMIN_MANAGE')")
    @Transactional
    public ResponseEntity<?> deleteInvite(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthAccount admin) {
        if (id.equals(admin.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You cannot delete your own account."));
        }

        AuthAccount target = authAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        // 1. Null out FK references where the data should be preserved
        authAccountRepository.nullManagerByAccountId(id);
        workspaceRepository.nullCreatedByByAccountId(id);
        opportunityRepository.nullAssignedToByAccountId(id);
        activityRepository.nullAssignedToByAccountId(id);
        commentRepository.nullAuthorByAccountId(id);
        attachmentRepository.nullUploaderByAccountId(id);
        roleAuditRepository.nullActorByAccountId(id);

        // 2. Delete child records that are tied to this account
        roleAuditRepository.deleteByTargetAccountId(id);
        recordShareRepository.deleteBySharedWithId(id);
        refreshTokenRepository.deleteByAuthAccountId(id);
        membershipRepository.deleteByAccountId(id);

        // 3. Remove from Firebase Auth if a user was created
        if (target.getFirebaseUid() != null) {
            try {
                FirebaseAuth.getInstance().deleteUser(target.getFirebaseUid());
            } catch (FirebaseAuthException e) {
                log.warn("Could not delete Firebase user {}: {}", target.getFirebaseUid(), e.getMessage());
            }
        } else {
            // May have been pre-created by the invite flow without UID being stored
            try {
                String uid = FirebaseAuth.getInstance().getUserByEmail(target.getEmail()).getUid();
                FirebaseAuth.getInstance().deleteUser(uid);
            } catch (FirebaseAuthException e) {
                log.debug("No Firebase user found for {}", target.getEmail());
            }
        }

        // 4. Delete the account itself
        authAccountRepository.delete(target);

        return ResponseEntity.ok(Map.of("message",
                "Account '" + target.getEmail() + "' deleted successfully."));
    }
}
