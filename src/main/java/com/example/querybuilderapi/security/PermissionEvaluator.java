package com.example.querybuilderapi.security;

import com.example.querybuilderapi.model.AuthAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security SpEL bean that exposes the {@link RolePermissions} matrix
 * to {@code @PreAuthorize} expressions across all controllers.
 *
 * Registered as {@code "perms"} so controllers can write:
 *
 * <pre>
 *   {@literal @}PreAuthorize("@perms.can('OPPORTUNITIES_DELETE')")
 * </pre>
 *
 * Role resolution order (most specific wins):
 *  1. {@link WorkspaceContext#getEffectiveRole()} — the per-workspace membership role
 *     resolved by {@link WorkspaceResolutionFilter}.  This is the correct role to use
 *     for all workspace-scoped endpoints once Phase 3 is active.
 *  2. {@link WorkspaceContext#isSuperAdminBypass()} — SUPER_ADMIN skips membership
 *     checks and implicitly holds every permission.
 *  3. Global role from the {@link AuthAccount} principal or JWT authority — fallback
 *     for auth/workspace endpoints that run before workspace resolution.
 *
 * If the permission name is unknown an error is logged and access is denied
 * rather than silently granted — fail-closed behaviour.
 */
@Component("perms")
public class PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PermissionEvaluator.class);

    private final WorkspaceContext workspaceContext;

    public PermissionEvaluator(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    /**
     * Returns {@code true} when the current caller holds the given permission
     * in the current workspace context.
     *
     * @param permissionName the {@link Permission} constant name (e.g. {@code "OPPORTUNITIES_DELETE"})
     * @return {@code true} if allowed, {@code false} otherwise
     */
    public boolean can(String permissionName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Permission permission;
        try {
            permission = Permission.valueOf(permissionName);
        } catch (IllegalArgumentException e) {
            log.error("Unknown permission checked in @PreAuthorize: '{}' — access denied.", permissionName);
            return false;   // fail-closed: unknown permission = deny
        }

        // ── Priority 1: SUPER_ADMIN bypass ───────────────────────────────────
        // WorkspaceResolutionFilter sets this flag when the caller is a SUPER_ADMIN.
        // SUPER_ADMIN holds every permission unconditionally.
        if (workspaceContext.isSuperAdminBypass()) {
            return true;
        }

        // ── Priority 2: Workspace-scoped effective role ───────────────────────
        // WorkspaceResolutionFilter resolves the caller's role in the target workspace
        // from workspace_memberships.role — this takes precedence over the global
        // auth_accounts.role so that a user who is MANAGER in one workspace but
        // SALES_REP in another gets the correct permissions per request.
        if (workspaceContext.isResolved() && workspaceContext.getEffectiveRole() != null) {
            return RolePermissions.has(workspaceContext.getEffectiveRole(), permission);
        }

        // ── Priority 3: Global role fallback ─────────────────────────────────
        // Used for endpoints that bypass workspace resolution (auth/**, health/**,
        // workspace-creation) where the workspace context is intentionally absent.

        // Primary: principal is an AuthAccount (set by FirebaseTokenFilter)
        if (auth.getPrincipal() instanceof AuthAccount account) {
            return RolePermissions.has(account.getRole(), permission);
        }

        // Last resort: derive role from the ROLE_* GrantedAuthority (JWT path)
        return auth.getAuthorities().stream()
                .anyMatch(authority -> {
                    String raw = authority.getAuthority();
                    if (!raw.startsWith("ROLE_")) return false;
                    try {
                        AuthAccount.Role role = AuthAccount.Role.valueOf(raw.substring(5));
                        return RolePermissions.has(role, permission);
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                });
    }
}
