package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A Workspace is an isolated CRM environment.
 * Users belong to one or more workspaces via {@link WorkspaceMembership}.
 * All CRM entities (Org, Contact, Opportunity, Activity) carry a workspace FK.
 *
 * Maps to the {@code workspaces} table added in V3__workspaces.sql.
 */
@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL-safe unique identifier, e.g. "acme-corp". */
    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    /** Human-readable display name, e.g. "Acme Corp". */
    @Column(nullable = false, length = 255)
    private String name;

    /** The account that created this workspace (nullable — for system-seeded workspaces). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AuthAccount createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Workspace() {}

    public Workspace(String slug, String name, AuthAccount createdBy) {
        this.slug      = slug;
        this.name      = name;
        this.createdBy = createdBy;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public String getSlug()                { return slug; }
    public void setSlug(String slug)       { this.slug = slug; }

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }

    public AuthAccount getCreatedBy()                    { return createdBy; }
    public void setCreatedBy(AuthAccount createdBy)      { this.createdBy = createdBy; }

    public Instant getCreatedAt()          { return createdAt; }
    public Instant getUpdatedAt()          { return updatedAt; }
}
