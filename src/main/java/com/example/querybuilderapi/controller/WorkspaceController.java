package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Workspace;
import com.example.querybuilderapi.model.WorkspaceMembership;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.security.WorkspaceContext;
import com.example.querybuilderapi.service.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for workspace lifecycle operations.
 *
 * All endpoints require authentication. The {@code @PreAuthorize} on each
 * method is a coarse, cheap gate (does the caller hold this permission
 * *anywhere*, per their X-Workspace-Id-resolved context?). For any endpoint
 * that targets a specific workspace via a path variable, the authoritative
 * check is inside {@link WorkspaceService} — it verifies the caller actually
 * holds the permission *in that workspace*, not merely in whichever workspace
 * their header happens to resolve to.
 */
@RestController
@RequestMapping("/api")
public class WorkspaceController {

    private final WorkspaceService       workspaceService;
    private final AuthAccountRepository  accountRepository;
    private final WorkspaceContext       workspaceContext;

    public WorkspaceController(WorkspaceService workspaceService,
                               AuthAccountRepository accountRepository,
                               WorkspaceContext workspaceContext) {
        this.workspaceService  = workspaceService;
        this.accountRepository = accountRepository;
        this.workspaceContext  = workspaceContext;
    }

    // ── GET /api/workspaces — list my workspaces ──────────────────────────

    /**
     * Returns all workspaces the authenticated user belongs to.
     * Used by the frontend WorkspaceSwitcher component.
     * Any authenticated role (GUEST and above) can list their own memberships.
     */
    @GetMapping("/workspaces")
    @PreAuthorize("@perms.can('TEAM_READ')")
    public ResponseEntity<List<Map<String, Object>>> getMyWorkspaces(Authentication auth) {
        AuthAccount account = resolveAccount(auth);
        List<WorkspaceMembership> memberships = workspaceService.getMyWorkspaces(account.getId());

        List<Map<String, Object>> result = memberships.stream().map(m -> Map.<String, Object>of(
                "id",        m.getWorkspace().getId(),
                "name",      m.getWorkspace().getName(),
                "slug",      m.getWorkspace().getSlug(),
                "isPublic",  m.getWorkspace().isPublic(),
                "role",      m.getRole().name(),
                "joinedAt",  m.getJoinedAt().toString()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── POST /api/workspaces — create workspace ───────────────────────────

    /**
     * Creates a new workspace. The caller becomes its WORKSPACE_OWNER.
     * Restricted to SUPER_ADMIN and existing WORKSPACE_OWNERs — everyone else
     * (ADMIN and below) cannot spin up new tenants.
     */
    @PostMapping("/workspaces")
    @PreAuthorize("@perms.can('WORKSPACE_CREATE')")
    public ResponseEntity<Map<String, Object>> createWorkspace(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String name = body.get("name");
        String slug = body.get("slug");
        if (name == null || name.isBlank() || slug == null || slug.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name and slug are required"));
        }

        AuthAccount creator = resolveAccount(auth);
        Workspace ws = workspaceService.createWorkspace(name, slug, creator.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id",       ws.getId(),
                "name",     ws.getName(),
                "slug",     ws.getSlug(),
                "isPublic", ws.isPublic()
        ));
    }

    // ── PATCH /api/workspaces/{id} — rename / re-slug / visibility ────────

    /**
     * Updates a workspace's name, slug, and/or public/private visibility.
     * Only non-null fields in the request body are applied.
     *
     * Requires WORKSPACE_UPDATE *in this specific workspace* (SUPER_ADMIN,
     * or WORKSPACE_OWNER/ADMIN of this workspace) — enforced in
     * {@link WorkspaceService#updateWorkspace}, not by this method's
     * {@code @PreAuthorize} alone.
     */
    @PatchMapping("/workspaces/{workspaceId}")
    @PreAuthorize("@perms.can('WORKSPACE_UPDATE')")
    public ResponseEntity<Map<String, Object>> updateWorkspace(
            @PathVariable Long workspaceId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        String name = (String) body.get("name");
        String slug = (String) body.get("slug");
        Boolean isPublic = body.get("isPublic") == null ? null : Boolean.valueOf(body.get("isPublic").toString());

        AuthAccount caller = resolveAccount(auth);
        Workspace ws = workspaceService.updateWorkspace(workspaceId, name, slug, isPublic, caller.getId());

        return ResponseEntity.ok(Map.of(
                "id",       ws.getId(),
                "name",     ws.getName(),
                "slug",     ws.getSlug(),
                "isPublic", ws.isPublic()
        ));
    }

    // ── DELETE /api/workspaces/{id} — permanently delete a workspace ──────

    /**
     * Permanently deletes a workspace. Refused if it still has any
     * organizations, contacts, opportunities, or activities — remove or
     * reassign that data first.
     *
     * Requires WORKSPACE_DELETE *in this specific workspace* (SUPER_ADMIN, or
     * the WORKSPACE_OWNER of this workspace) — enforced in
     * {@link WorkspaceService#deleteWorkspace}, not by this method's
     * {@code @PreAuthorize} alone.
     */
    @DeleteMapping("/workspaces/{workspaceId}")
    @PreAuthorize("@perms.can('WORKSPACE_DELETE')")
    public ResponseEntity<Map<String, String>> deleteWorkspace(
            @PathVariable Long workspaceId,
            Authentication auth) {

        AuthAccount caller = resolveAccount(auth);
        workspaceService.deleteWorkspace(workspaceId, caller.getId());

        return ResponseEntity.ok(Map.of("message", "Workspace deleted successfully"));
    }

    // ── GET /api/workspaces/{id}/members ─────────────────────────────────

    @GetMapping("/workspaces/{workspaceId}/members")
    @PreAuthorize("@perms.can('TEAM_READ_ALL')")
    public ResponseEntity<List<Map<String, Object>>> getWorkspaceMembers(
            @PathVariable Long workspaceId,
            Authentication auth) {

        AuthAccount caller = resolveAccount(auth);
        List<WorkspaceMembership> members = workspaceService.getWorkspaceMembers(workspaceId, caller.getId());

        List<Map<String, Object>> result = members.stream().map(m -> {
            AuthAccount a = m.getAccount();
            return Map.<String, Object>of(
                    "accountId",   a.getId(),
                    "displayName", a.getDisplayName(),
                    "email",       a.getEmail(),
                    "role",        m.getRole().name(),
                    "joinedAt",    m.getJoinedAt().toString(),
                    "isActive",    Boolean.TRUE.equals(a.getIsActive())
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── POST /api/workspaces/{id}/members — add member ────────────────────

    @PostMapping("/workspaces/{workspaceId}/members")
    @PreAuthorize("@perms.can('ADMIN_INVITE')")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable Long workspaceId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        Long accountId = Long.parseLong(body.getOrDefault("accountId", "0"));
        AuthAccount.Role role;
        try {
            role = AuthAccount.Role.valueOf(body.getOrDefault("role", "SALES_REP"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + body.get("role")));
        }

        AuthAccount caller = resolveAccount(auth);
        WorkspaceMembership membership = workspaceService.addMember(workspaceId, accountId, role, caller.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "workspaceId", workspaceId,
                "accountId",   accountId,
                "role",        membership.getRole().name()
        ));
    }

    // ── DELETE /api/workspaces/{id}/members/{accountId} — remove member ──

    @DeleteMapping("/workspaces/{workspaceId}/members/{accountId}")
    @PreAuthorize("@perms.can('ADMIN_MANAGE')")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long accountId,
            Authentication auth) {

        AuthAccount caller = resolveAccount(auth);
        workspaceService.removeMember(workspaceId, accountId, caller.getId());
        return ResponseEntity.ok(Map.of("message", "Member removed from workspace"));
    }

    // ── PATCH /api/workspaces/{id}/members/{accountId}/role — change role ─

    @PatchMapping("/workspaces/{workspaceId}/members/{accountId}/role")
    @PreAuthorize("@perms.can('TEAM_ROLE_ASSIGN')")
    public ResponseEntity<Map<String, Object>> changeMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long accountId,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        AuthAccount.Role newRole;
        try {
            newRole = AuthAccount.Role.valueOf(body.getOrDefault("role", ""));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + body.get("role")));
        }

        AuthAccount caller = resolveAccount(auth);
        WorkspaceMembership updated = workspaceService.changeMemberRole(workspaceId, accountId, newRole, caller.getId());
        return ResponseEntity.ok(Map.of(
                "workspaceId", workspaceId,
                "accountId",   accountId,
                "role",        updated.getRole().name()
        ));
    }

    // ── POST /api/me/active-workspace — switch active workspace ──────────

    /**
     * Updates the user's activeWorkspaceId Firebase custom claim.
     * The frontend must force-refresh the ID token after calling this.
     * Any authenticated role can switch their own active workspace.
     */
    @PostMapping("/me/active-workspace")
    @PreAuthorize("@perms.can('TEAM_READ')")
    public ResponseEntity<Map<String, Object>> switchActiveWorkspace(
            @RequestBody Map<String, Long> body,
            Authentication auth) {

        Long workspaceId = body.get("workspaceId");
        if (workspaceId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "workspaceId is required"));
        }

        AuthAccount account = resolveAccount(auth);
        workspaceService.setActiveWorkspace(account.getId(), workspaceId);

        return ResponseEntity.ok(Map.of(
                "activeWorkspaceId", workspaceId,
                "message", "Active workspace updated. Refresh your token to apply."
        ));
    }

    // ── Private utils ─────────────────────────────────────────────────────

    private AuthAccount resolveAccount(Authentication auth) {
        return accountRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated account not found: " + auth.getName()));
    }
}
