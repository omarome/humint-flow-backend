package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Join table between {@link AuthAccount} and {@link Workspace}.
 *
 * Each membership carries a workspace-scoped {@link AuthAccount.Role}.
 * A user's effective role for any API request is the role stored here —
 * NOT the global {@code auth_accounts.role} column (which is legacy).
 *
 * Maps to the {@code workspace_memberships} table added in V3__workspaces.sql.
 */
@Entity
@Table(
    name = "workspace_memberships",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_workspace_account",
        columnNames = {"workspace_id", "account_id"}
    )
)
public class WorkspaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AuthAccount account;

    /**
     * The workspace-scoped role for this membership.
     * This overrides {@code auth_accounts.role} for all permission checks.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthAccount.Role role = AuthAccount.Role.SALES_REP;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false, nullable = false)
    private Instant joinedAt;

    public WorkspaceMembership() {}

    public WorkspaceMembership(Workspace workspace, AuthAccount account, AuthAccount.Role role) {
        this.workspace = workspace;
        this.account   = account;
        this.role      = role;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public Workspace getWorkspace()                    { return workspace; }
    public void setWorkspace(Workspace workspace)      { this.workspace = workspace; }

    public AuthAccount getAccount()                    { return account; }
    public void setAccount(AuthAccount account)        { this.account = account; }

    public AuthAccount.Role getRole()                  { return role; }
    public void setRole(AuthAccount.Role role)         { this.role = role; }

    public Instant getJoinedAt()                       { return joinedAt; }
}
