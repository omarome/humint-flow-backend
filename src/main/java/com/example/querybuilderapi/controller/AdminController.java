package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.InviteRequest;
import com.example.querybuilderapi.dto.TeamMemberResponse;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.service.TeamMemberService;
import jakarta.validation.Valid;
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

    private final AuthAccountRepository authAccountRepository;
    private final TeamMemberService     teamMemberService;

    public AdminController(AuthAccountRepository authAccountRepository,
                           TeamMemberService teamMemberService) {
        this.authAccountRepository = authAccountRepository;
        this.teamMemberService     = teamMemberService;
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

        // Return the full team-member profile shape so the frontend can add the row to the table
        TeamMemberResponse profile = teamMemberService.getMember(invited.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
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
}
