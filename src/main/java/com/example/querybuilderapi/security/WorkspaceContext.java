package com.example.querybuilderapi.security;

import com.example.querybuilderapi.model.AuthAccount;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request-scoped bean that carries the workspace context for the current HTTP request.
 *
 * Populated by {@link WorkspaceResolutionFilter} after authentication.
 * Consumed by services and {@link PermissionEvaluator} to enforce workspace-scoped access.
 *
 * A request-scoped bean means one instance per HTTP request — thread-safe by design.
 */
@Component
@RequestScope
public class WorkspaceContext {

    /** The resolved workspace ID for this request. Null if not yet resolved. */
    private Long workspaceId;

    /**
     * The effective role for the authenticated user in the current workspace.
     * This OVERRIDES the global {@code auth_accounts.role} for all permission checks.
     * Null if not yet resolved.
     */
    private AuthAccount.Role effectiveRole;

    /** True if the current user is a SUPER_ADMIN bypassing workspace membership checks. */
    private boolean superAdminBypass = false;

    public WorkspaceContext() {}

    // ── Accessors ─────────────────────────────────────────────────────────

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public AuthAccount.Role getEffectiveRole() {
        return effectiveRole;
    }

    public void setEffectiveRole(AuthAccount.Role effectiveRole) {
        this.effectiveRole = effectiveRole;
    }

    public boolean isSuperAdminBypass() {
        return superAdminBypass;
    }

    public void setSuperAdminBypass(boolean superAdminBypass) {
        this.superAdminBypass = superAdminBypass;
    }

    /** @return true if workspace context has been fully resolved */
    public boolean isResolved() {
        return workspaceId != null || superAdminBypass;
    }
}
