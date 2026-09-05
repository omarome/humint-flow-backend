package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Workspace;
import com.example.querybuilderapi.model.WorkspaceMembership;
import com.example.querybuilderapi.model.RoleAudit;
import com.example.querybuilderapi.repository.ActivityRepository;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import com.example.querybuilderapi.repository.WorkspaceMembershipRepository;
import com.example.querybuilderapi.repository.WorkspaceRepository;
import com.example.querybuilderapi.repository.RoleAuditRepository;
import com.example.querybuilderapi.security.Permission;
import com.example.querybuilderapi.security.RolePermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for workspace lifecycle operations.
 *
 * All write operations are transactional. Coarse role checks are handled at
 * the controller layer via {@code @PreAuthorize("@perms.can(...)")}; the
 * authoritative check for anything that targets a *specific* workspace
 * (identified by a path variable) is {@link #requireWorkspacePermission},
 * because the controller-level check only proves the caller holds the
 * permission *somewhere* (whatever workspace their X-Workspace-Id header
 * resolves to) — not that they hold it in the workspace they're actually
 * targeting.
 */
@Service
@Transactional
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceRepository           workspaceRepo;
    private final WorkspaceMembershipRepository membershipRepo;
    private final AuthAccountRepository         accountRepo;
    private final OrganizationRepository        organizationRepo;
    private final ContactRepository             contactRepo;
    private final OpportunityRepository         opportunityRepo;
    private final ActivityRepository            activityRepo;
    private final FirebaseClaimsService         firebaseClaimsService;
    private final RoleAuditRepository           roleAuditRepo;
    private final AuditAwareService             auditAwareService;

    public WorkspaceService(WorkspaceRepository workspaceRepo,
                            WorkspaceMembershipRepository membershipRepo,
                            AuthAccountRepository accountRepo,
                            OrganizationRepository organizationRepo,
                            ContactRepository contactRepo,
                            OpportunityRepository opportunityRepo,
                            ActivityRepository activityRepo,
                            FirebaseClaimsService firebaseClaimsService,
                            RoleAuditRepository roleAuditRepo,
                            AuditAwareService auditAwareService) {
        this.workspaceRepo        = workspaceRepo;
        this.membershipRepo       = membershipRepo;
        this.accountRepo          = accountRepo;
        this.organizationRepo     = organizationRepo;
        this.contactRepo          = contactRepo;
        this.opportunityRepo      = opportunityRepo;
        this.activityRepo         = activityRepo;
        this.firebaseClaimsService = firebaseClaimsService;
        this.roleAuditRepo        = roleAuditRepo;
        this.auditAwareService    = auditAwareService;
    }

    // ── Create Workspace ─────────────────────────────────────────────────

    /**
     * Creates a new workspace and adds the creator as WORKSPACE_OWNER.
     *
     * @param name           Display name (e.g. "Acme Corp")
     * @param slug           URL-safe identifier (e.g. "acme-corp") — must be unique
     * @param creatorAccountId The auth_accounts.id of the creating user
     * @return the newly created {@link Workspace}
     */
    public Workspace createWorkspace(String name, String slug, Long creatorAccountId) {
        if (workspaceRepo.existsBySlug(slug)) {
            throw new IllegalArgumentException("Workspace slug '" + slug + "' is already taken.");
        }

        AuthAccount creator = accountRepo.findById(creatorAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + creatorAccountId));

        Workspace ws = new Workspace(slug, name, creator);
        ws = workspaceRepo.save(ws);
        log.info("Created workspace '{}' (id={}) by account {}", name, ws.getId(), creatorAccountId);

        // Auto-add creator as WORKSPACE_OWNER
        WorkspaceMembership ownership = new WorkspaceMembership(
                ws, creator, AuthAccount.Role.WORKSPACE_OWNER);
        membershipRepo.save(ownership);

        // Push activeWorkspaceId into Firebase claims for the creator
        firebaseClaimsService.syncClaimsWithWorkspace(creatorAccountId, ws.getId());

        return ws;
    }

    // ── Update Workspace ─────────────────────────────────────────────────

    /**
     * Renames a workspace, changes its slug, and/or toggles its public/private
     * visibility. Only fields that are non-null are applied.
     *
     * Requires {@link Permission#WORKSPACE_UPDATE} in the target workspace
     * specifically (SUPER_ADMIN, WORKSPACE_OWNER, or ADMIN of *that* workspace —
     * not just any workspace the caller happens to belong to).
     */
    public Workspace updateWorkspace(Long workspaceId, String name, String slug, Boolean isPublic,
                                     Long callerAccountId) {
        requireWorkspacePermission(workspaceId, callerAccountId, Permission.WORKSPACE_UPDATE);

        Workspace ws = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        if (name != null && !name.isBlank()) {
            ws.setName(name);
        }
        if (slug != null && !slug.isBlank() && !slug.equals(ws.getSlug())) {
            if (workspaceRepo.existsBySlugAndIdNot(slug, workspaceId)) {
                throw new IllegalArgumentException("Workspace slug '" + slug + "' is already taken.");
            }
            ws.setSlug(slug);
        }
        if (isPublic != null) {
            ws.setPublic(isPublic);
        }

        ws = workspaceRepo.save(ws);
        log.info("Updated workspace {} by account {}", workspaceId, callerAccountId);
        return ws;
    }

    // ── Delete Workspace ─────────────────────────────────────────────────

    /**
     * Permanently deletes a workspace. Refuses if the workspace still has any
     * non-deleted organizations, contacts, opportunities, or activities —
     * remove/reassign that data first. Membership rows and role-audit history
     * for the workspace are deleted as part of the same transaction (their
     * FK to {@code workspaces} is {@code NOT NULL}, so they cannot be orphaned).
     *
     * Requires {@link Permission#WORKSPACE_DELETE} in the target workspace
     * specifically (SUPER_ADMIN, or WORKSPACE_OWNER of *that* workspace).
     */
    public void deleteWorkspace(Long workspaceId, Long callerAccountId) {
        requireWorkspacePermission(workspaceId, callerAccountId, Permission.WORKSPACE_DELETE);

        Workspace ws = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        long orgCount      = organizationRepo.countByWorkspaceIdAndIsDeletedFalse(workspaceId);
        long contactCount  = contactRepo.countByWorkspaceIdAndIsDeletedFalse(workspaceId);
        long oppCount      = opportunityRepo.countByWorkspaceIdAndIsDeletedFalse(workspaceId);
        long activityCount = activityRepo.countByWorkspaceIdAndIsDeletedFalse(workspaceId);

        if (orgCount + contactCount + oppCount + activityCount > 0) {
            throw new IllegalStateException(
                    "Workspace still has data (" + orgCount + " organizations, " + contactCount +
                    " contacts, " + oppCount + " opportunities, " + activityCount +
                    " activities) — remove or reassign it before deleting the workspace.");
        }

        roleAuditRepo.deleteByWorkspaceId(workspaceId);
        membershipRepo.deleteByWorkspaceId(workspaceId);
        workspaceRepo.delete(ws);

        log.warn("Deleted workspace '{}' (id={}) by account {}", ws.getName(), workspaceId, callerAccountId);
    }

    // ── Member Management ────────────────────────────────────────────────

    /**
     * Adds an existing AuthAccount to a workspace with a given role.
     * No-op if already a member (idempotent).
     */
    public WorkspaceMembership addMember(Long workspaceId, Long accountId, AuthAccount.Role role,
                                         Long callerAccountId) {
        requireWorkspacePermission(workspaceId, callerAccountId, Permission.ADMIN_INVITE);

        if (membershipRepo.existsByWorkspaceIdAndAccountId(workspaceId, accountId)) {
            return membershipRepo.findByWorkspaceIdAndAccountId(workspaceId, accountId).orElseThrow();
        }

        Workspace ws = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        AuthAccount account = accountRepo.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        WorkspaceMembership membership = new WorkspaceMembership(ws, account, role);
        membership = membershipRepo.save(membership);
        logRoleAudit(ws, account, null, role, "Member added to workspace");
        return membership;
    }

    /**
     * Removes a member from a workspace.
     * Guards against removing the last WORKSPACE_OWNER.
     */
    public void removeMember(Long workspaceId, Long accountId, Long callerAccountId) {
        requireWorkspacePermission(workspaceId, callerAccountId, Permission.ADMIN_MANAGE);

        WorkspaceMembership membership = membershipRepo
                .findByWorkspaceIdAndAccountId(workspaceId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account " + accountId + " is not a member of workspace " + workspaceId));

        // Guard: cannot remove last owner
        if (membership.getRole() == AuthAccount.Role.WORKSPACE_OWNER) {
            long ownerCount = membershipRepo.countOwnersByWorkspaceId(workspaceId);
            if (ownerCount <= 1) {
                throw new IllegalStateException(
                        "Cannot remove the last WORKSPACE_OWNER. Transfer ownership first.");
            }
        }

        membershipRepo.delete(membership);
        logRoleAudit(membership.getWorkspace(), membership.getAccount(), membership.getRole(), null, "Member removed from workspace");
        log.info("Removed account {} from workspace {}", accountId, workspaceId);
    }

    /**
     * Changes a member's workspace-scoped role and re-syncs Firebase claims.
     */
    public WorkspaceMembership changeMemberRole(Long workspaceId, Long accountId, AuthAccount.Role newRole,
                                                Long callerAccountId) {
        requireWorkspacePermission(workspaceId, callerAccountId, Permission.TEAM_ROLE_ASSIGN);

        WorkspaceMembership membership = membershipRepo
                .findByWorkspaceIdAndAccountId(workspaceId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account " + accountId + " is not a member of workspace " + workspaceId));

        AuthAccount.Role oldRole = membership.getRole();
        membership.setRole(newRole);
        membershipRepo.save(membership);
        logRoleAudit(membership.getWorkspace(), membership.getAccount(), oldRole, newRole, "Role updated");

        log.info("Role changed for account {} in workspace {}: {} → {}", accountId, workspaceId, oldRole, newRole);

        // Re-sync Firebase claims with the new workspace-scoped role
        firebaseClaimsService.syncClaimsWithWorkspace(accountId, workspaceId);

        return membership;
    }

    // ── Workspace Queries ─────────────────────────────────────────────────

    /**
     * Returns all workspaces the given account belongs to (for the switcher UI).
     */
    @Transactional(readOnly = true)
    public List<WorkspaceMembership> getMyWorkspaces(Long accountId) {
        return membershipRepo.findAllByAccountIdWithWorkspace(accountId);
    }

    /**
     * Returns all members of a specific workspace.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceMembership> getWorkspaceMembers(Long workspaceId, Long callerAccountId) {
        requireWorkspacePermission(workspaceId, callerAccountId, Permission.TEAM_READ_ALL);
        return membershipRepo.findAllByWorkspaceIdWithAccount(workspaceId);
    }

    // ── Active Workspace Switch ──────────────────────────────────────────

    /**
     * Updates the user's activeWorkspaceId Firebase custom claim so the
     * frontend ID token refreshes to the new workspace context.
     * Validates membership before switching.
     */
    public void setActiveWorkspace(Long accountId, Long workspaceId) {
        if (!membershipRepo.existsByWorkspaceIdAndAccountId(workspaceId, accountId)) {
            throw new IllegalArgumentException(
                    "Account " + accountId + " is not a member of workspace " + workspaceId);
        }
        firebaseClaimsService.syncClaimsWithWorkspace(accountId, workspaceId);
        log.info("Switched activeWorkspaceId to {} for account {}", workspaceId, accountId);
    }

    // ── Access control ────────────────────────────────────────────────────

    /**
     * The authoritative permission check for any operation that targets a
     * specific workspace (identified by a path variable, not by the caller's
     * X-Workspace-Id header). SUPER_ADMIN bypasses membership entirely,
     * exactly like {@code WorkspaceResolutionFilter}. Everyone else must hold
     * an actual {@link WorkspaceMembership} row in *this* workspace whose role
     * grants the requested permission — a permission the caller holds in some
     * *other* workspace does not count.
     */
    private void requireWorkspacePermission(Long workspaceId, Long callerAccountId, Permission permission) {
        AuthAccount caller = accountRepo.findById(callerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + callerAccountId));

        if (caller.getRole() == AuthAccount.Role.SUPER_ADMIN) {
            return;
        }

        WorkspaceMembership membership = membershipRepo.findByWorkspaceIdAndAccountId(workspaceId, callerAccountId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Account " + callerAccountId + " is not a member of workspace " + workspaceId));

        if (!RolePermissions.has(membership.getRole(), permission)) {
            throw new AccessDeniedException(
                    "Role " + membership.getRole() + " lacks permission " + permission
                            + " in workspace " + workspaceId);
        }
    }

    private void logRoleAudit(Workspace ws, AuthAccount targetAccount, AuthAccount.Role oldRole, AuthAccount.Role newRole, String reason) {
        Long currentUserId = auditAwareService.getCurrentUserId();
        AuthAccount actor = null;
        if (currentUserId != null) {
            actor = accountRepo.findById(currentUserId).orElse(null);
        }
        RoleAudit audit = new RoleAudit(ws, actor, targetAccount, oldRole, newRole, reason);
        roleAuditRepo.save(audit);
    }
}
