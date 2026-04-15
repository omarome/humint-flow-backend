package com.example.querybuilderapi.security;

/**
 * Granular permission constants for HumintFlow.
 *
 * Each constant maps to one specific action on one resource.
 * Role → permission sets are defined in {@link RolePermissions}.
 *
 * Controllers reference these via the {@code @perms.can('...')} SpEL expression:
 *
 *   {@code @PreAuthorize("@perms.can('OPPORTUNITIES_DELETE')")}
 *
 * Naming convention: {RESOURCE}_{ACTION}
 */
public enum Permission {

    // ── Organizations ────────────────────────────────────────────────────────
    ORGANIZATIONS_READ,
    ORGANIZATIONS_CREATE,
    ORGANIZATIONS_UPDATE,
    ORGANIZATIONS_DELETE,

    // ── Contacts ─────────────────────────────────────────────────────────────
    CONTACTS_READ,
    CONTACTS_CREATE,
    CONTACTS_UPDATE,
    CONTACTS_DELETE,

    // ── Opportunities ─────────────────────────────────────────────────────────
    OPPORTUNITIES_READ,
    OPPORTUNITIES_CREATE,
    OPPORTUNITIES_UPDATE,
    OPPORTUNITIES_DELETE,

    // ── Activities (timeline) ─────────────────────────────────────────────────
    ACTIVITIES_READ,
    ACTIVITIES_CREATE,
    ACTIVITIES_UPDATE,
    ACTIVITIES_DELETE,

    // ── Team members ──────────────────────────────────────────────────────────
    /** List active members. */
    TEAM_READ,
    /** List all members including inactive (admin view). */
    TEAM_READ_ALL,
    /** Update a member's profile fields. */
    TEAM_EDIT,
    /** Change another account's role. */
    TEAM_ROLE_ASSIGN,

    // ── Automations ───────────────────────────────────────────────────────────
    AUTOMATIONS_READ,
    /** Create, update, delete, and toggle automation rules. */
    AUTOMATIONS_WRITE,

    // ── Saved views ───────────────────────────────────────────────────────────
    SAVED_VIEWS_READ,
    SAVED_VIEWS_WRITE,

    // ── Variable / field metadata ─────────────────────────────────────────────
    VARIABLES_READ,

    // ── CRM segmentation query engine ─────────────────────────────────────────
    SEGMENTS_READ,

    // ── Notifications ─────────────────────────────────────────────────────────
    /** Read and mark-read own notifications. */
    NOTIFICATIONS_READ,
    /** Broadcast, delete-all, and create test notifications (admin/ops). */
    NOTIFICATIONS_MANAGE,

    // ── Comments ──────────────────────────────────────────────────────────────
    COMMENTS_READ,
    COMMENTS_WRITE,

    // ── Attachments ───────────────────────────────────────────────────────────
    ATTACHMENTS_READ,
    ATTACHMENTS_WRITE,

    // ── Audit log ─────────────────────────────────────────────────────────────
    AUDIT_READ,

    // ── Push notifications (FCM) ──────────────────────────────────────────────
    PUSH_SEND,

    // ── Global search ─────────────────────────────────────────────────────────
    GLOBAL_SEARCH,

    // ── FCM device token registration ─────────────────────────────────────────
    FCM_TOKEN_WRITE,

    // ── Legacy users table ────────────────────────────────────────────────────
    USERS_READ,

    // ── Admin-only operations ─────────────────────────────────────────────────
    /** Pre-provision a new user account (invite flow). */
    ADMIN_INVITE,
    /** Deactivate or reactivate an account. */
    ADMIN_MANAGE,
}
