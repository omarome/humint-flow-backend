package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "record_shares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workspace_id", "resource_type", "resource_id", "shared_with_account_id"})
})
public class RecordShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private EntityType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_account_id", nullable = false)
    private AuthAccount sharedWith;

    @Column(nullable = false, length = 32)
    private String permission = "READ";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public RecordShare() {}

    public RecordShare(Workspace workspace, EntityType resourceType, UUID resourceId, AuthAccount sharedWith, String permission) {
        this.workspace = workspace;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.sharedWith = sharedWith;
        this.permission = permission;
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Workspace getWorkspace() { return workspace; }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    public EntityType getResourceType() { return resourceType; }
    public void setResourceType(EntityType resourceType) { this.resourceType = resourceType; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public AuthAccount getSharedWith() { return sharedWith; }
    public void setSharedWith(AuthAccount sharedWith) { this.sharedWith = sharedWith; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
