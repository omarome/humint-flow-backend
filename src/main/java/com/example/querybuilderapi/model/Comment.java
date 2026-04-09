package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A comment left by a user on any CRM entity.
 *
 * Comments are polymorphic — they reference an entity by type + UUID rather
 * than a foreign key, so a single table can store comments for contacts,
 * opportunities, organizations, etc.
 *
 * Maps to the "comments" table.
 */
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_comment_author", columnList = "author_id")
})
public class Comment extends BaseEntity {

    /**
     * The type of entity this comment belongs to (e.g. "CONTACT", "OPPORTUNITY").
     */
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    /**
     * The UUID of the entity this comment belongs to.
     */
    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    /**
     * The AuthAccount that wrote the comment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AuthAccount author;

    /**
     * The comment body. Supports plain text (markdown rendering is client-side).
     */
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Comment body cannot be empty")
    @Size(max = 10_000, message = "Comment too long")
    private String body;

    /**
     * Optional: the ID of the parent comment for threaded replies (null = top-level).
     */
    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    public Comment() {}

    // ─── Getters & Setters ────────────────────────────────────────────────

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public AuthAccount getAuthor() { return author; }
    public void setAuthor(AuthAccount author) { this.author = author; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
}
