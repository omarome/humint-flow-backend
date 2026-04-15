package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "role_audits")
public class RoleAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AuthAccount actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id", nullable = false)
    private AuthAccount targetAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_role")
    private AuthAccount.Role oldRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role")
    private AuthAccount.Role newRole;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public RoleAudit() {}

    public RoleAudit(Workspace workspace, AuthAccount actor, AuthAccount targetAccount,
                     AuthAccount.Role oldRole, AuthAccount.Role newRole, String reason) {
        this.workspace = workspace;
        this.actor = actor;
        this.targetAccount = targetAccount;
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.reason = reason;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public AuthAccount getActor() { return actor; }
    public void setActor(AuthAccount actor) { this.actor = actor; }

    public AuthAccount getTargetAccount() { return targetAccount; }
    public void setTargetAccount(AuthAccount targetAccount) { this.targetAccount = targetAccount; }

    public AuthAccount.Role getOldRole() { return oldRole; }
    public void setOldRole(AuthAccount.Role oldRole) { this.oldRole = oldRole; }

    public AuthAccount.Role getNewRole() { return newRole; }
    public void setNewRole(AuthAccount.Role newRole) { this.newRole = newRole; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
