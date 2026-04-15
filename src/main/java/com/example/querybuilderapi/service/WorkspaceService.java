package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.model.Workspace;
import com.example.querybuilderapi.model.WorkspaceMembership;
import com.example.querybuilderapi.model.RoleAudit;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.WorkspaceMembershipRepository;
import com.example.querybuilderapi.repository.WorkspaceRepository;
import com.example.querybuilderapi.repository.RoleAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for workspace lifecycle operations.
 *
 * All write operations are transactional.
 * Permission checks are handled at the controller layer via @PreAuthorize.
 */
@Service
@Transactional
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceRepository           workspaceRepo;
    private final WorkspaceMembershipRepository membershipRepo;
    private final AuthAccountRepository         accountRepo;
    private final FirebaseClaimsService         firebaseClaimsService;
    private final RoleAuditRepository           roleAuditRepo;
    private final AuditAwareService             auditAwareService;

    public WorkspaceService(WorkspaceRepository workspaceRepo,
                            WorkspaceMembershipRepository membershipRepo,
                            AuthAccountRepository accountRepo,
                            FirebaseClaimsService firebaseClaimsService,
                            RoleAuditRepository roleAuditRepo,
                            AuditAwareService auditAwareService) {
        this.workspaceRepo        = workspaceRepo;
        this.membershipRepo       = membershipRepo;
        this.accountRepo          = accountRepo;
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

    // ── Member Management ────────────────────────────────────────────────

    /**
     * Adds an existing AuthAccount to a workspace with a given role.
     * No-op if already a member (idempotent).
     */
    public WorkspaceMembership addMember(Long workspaceId, Long accountId, AuthAccount.Role role) {
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
    public void removeMember(Long workspaceId, Long accountId) {
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
    public WorkspaceMembership changeMemberRole(Long workspaceId, Long accountId, AuthAccount.Role newRole) {
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
    public List<WorkspaceMembership> getWorkspaceMembers(Long workspaceId) {
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
