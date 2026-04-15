package com.example.querybuilderapi.security;

import com.example.querybuilderapi.model.AuthAccount;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for the Role → Permission matrix.
 *
 * Design principles:
 *  - Each role builds on top of the previous (additive hierarchy).
 *  - Changing a role's permissions requires editing one place only.
 *  - When new roles are added to {@link AuthAccount.Role}, add a block here.
 *
 * Role hierarchy (lowest → highest privilege):
 *   GUEST → VIEWER → USER ≈ SALES_REP → MANAGER → ADMIN → WORKSPACE_OWNER ≈ SUPER_ADMIN
 */
public final class RolePermissions {

    private RolePermissions() {}

    private static final Map<AuthAccount.Role, Set<Permission>> MATRIX;

    static {
        Map<AuthAccount.Role, Set<Permission>> m = new EnumMap<>(AuthAccount.Role.class);

        // ── GUEST ─────────────────────────────────────────────────────────────
        // External collaborators: read-only on explicitly shared content.
        // For now only the team directory is accessible (enough to know who to contact).
        EnumSet<Permission> guest = EnumSet.of(
                Permission.TEAM_READ
        );
        m.put(AuthAccount.Role.GUEST, guest);

        // ── VIEWER ────────────────────────────────────────────────────────────
        // Exec / auditor / finance stakeholder: full read-only access.
        EnumSet<Permission> viewer = EnumSet.of(
                Permission.ORGANIZATIONS_READ,
                Permission.CONTACTS_READ,
                Permission.OPPORTUNITIES_READ,
                Permission.ACTIVITIES_READ,
                Permission.TEAM_READ,
                Permission.AUTOMATIONS_READ,
                Permission.SAVED_VIEWS_READ,
                Permission.VARIABLES_READ,
                Permission.SEGMENTS_READ,
                Permission.NOTIFICATIONS_READ,
                Permission.COMMENTS_READ,
                Permission.ATTACHMENTS_READ,
                Permission.AUDIT_READ,
                Permission.GLOBAL_SEARCH,
                Permission.USERS_READ,
                Permission.FCM_TOKEN_WRITE   // own device token — every signed-in user needs this
        );
        m.put(AuthAccount.Role.VIEWER, viewer);

        // ── SALES_REP ─────────────────────────────────────────────────────────
        // IC sales rep: full read + create/update own CRM records.
        // Deletes are escalated to MANAGER / ADMIN.
        EnumSet<Permission> salesRep = EnumSet.copyOf(viewer);
        salesRep.addAll(EnumSet.of(
                Permission.ORGANIZATIONS_CREATE,
                Permission.ORGANIZATIONS_UPDATE,
                Permission.CONTACTS_CREATE,
                Permission.CONTACTS_UPDATE,
                Permission.OPPORTUNITIES_CREATE,
                Permission.OPPORTUNITIES_UPDATE,
                Permission.ACTIVITIES_CREATE,
                Permission.ACTIVITIES_UPDATE,
                Permission.COMMENTS_WRITE,
                Permission.ATTACHMENTS_WRITE,
                Permission.SAVED_VIEWS_WRITE
        ));
        m.put(AuthAccount.Role.SALES_REP, salesRep);

        // ── USER ──────────────────────────────────────────────────────────────
        // Legacy role retained for backward compatibility — treated identically
        // to SALES_REP.  New accounts should be SALES_REP; USER will be removed
        // once all existing accounts are migrated.
        m.put(AuthAccount.Role.USER, EnumSet.copyOf(salesRep));

        // ── MANAGER ───────────────────────────────────────────────────────────
        // Sales Manager / team lead: everything a rep can do + deletes + team edit
        // + automation management + push notifications.
        EnumSet<Permission> manager = EnumSet.copyOf(salesRep);
        manager.addAll(EnumSet.of(
                Permission.ORGANIZATIONS_DELETE,
                Permission.CONTACTS_DELETE,
                Permission.ACTIVITIES_DELETE,
                Permission.TEAM_READ_ALL,
                Permission.TEAM_EDIT,
                Permission.AUTOMATIONS_WRITE,
                Permission.PUSH_SEND
        ));
        m.put(AuthAccount.Role.MANAGER, manager);

        // ── ADMIN ─────────────────────────────────────────────────────────────
        // Workspace admin / Head of Sales: everything a manager can do +
        // opportunity deletes + role management + notifications + admin actions.
        EnumSet<Permission> admin = EnumSet.copyOf(manager);
        admin.addAll(EnumSet.of(
                Permission.OPPORTUNITIES_DELETE,
                Permission.TEAM_ROLE_ASSIGN,
                Permission.NOTIFICATIONS_MANAGE,
                Permission.ADMIN_INVITE,
                Permission.ADMIN_MANAGE
        ));
        m.put(AuthAccount.Role.ADMIN, admin);

        // ── WORKSPACE_OWNER / SUPER_ADMIN ─────────────────────────────────────
        // All permissions without exception.
        EnumSet<Permission> all = EnumSet.allOf(Permission.class);
        m.put(AuthAccount.Role.WORKSPACE_OWNER, EnumSet.copyOf(all));
        m.put(AuthAccount.Role.SUPER_ADMIN,     EnumSet.copyOf(all));

        MATRIX = Collections.unmodifiableMap(m);
    }

    /**
     * Returns {@code true} if the given role includes the requested permission.
     *
     * @param role       the caller's role (never {@code null})
     * @param permission the permission to test
     */
    public static boolean has(AuthAccount.Role role, Permission permission) {
        if (role == null || permission == null) return false;
        Set<Permission> granted = MATRIX.get(role);
        return granted != null && granted.contains(permission);
    }

    /**
     * Returns the full (unmodifiable) permission set for a given role.
     * Useful for debugging or building "what can this role do?" UI.
     */
    public static Set<Permission> permissionsFor(AuthAccount.Role role) {
        return MATRIX.getOrDefault(role, Collections.emptySet());
    }
}
