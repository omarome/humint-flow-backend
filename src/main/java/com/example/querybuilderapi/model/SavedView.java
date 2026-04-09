package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store saved filter views.
 */
@Entity
@Table(name = "saved_views")
public class SavedView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String queryJson;

    /**
     * The CRM entity this saved view targets.
     * NULL for legacy views that query the users table.
     * Non-null for CRM segments: CONTACT, ORGANIZATION, OPPORTUNITY, ACTIVITY, TEAM_MEMBER.
     */
    @Column(name = "entity_type", length = 30)
    private String entityType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public SavedView() {
    }

    public SavedView(Long id, String name, String queryJson, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.queryJson = queryJson;
        this.createdAt = createdAt;
    }

    // Builder-like static method for convenience
    public static SavedViewBuilder builder() {
        return new SavedViewBuilder();
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQueryJson() {
        return queryJson;
    }

    public void setQueryJson(String queryJson) {
        this.queryJson = queryJson;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Simple Builder inner class
    public static class SavedViewBuilder {
        private String name;
        private String queryJson;
        private String entityType;

        public SavedViewBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SavedViewBuilder queryJson(String queryJson) {
            this.queryJson = queryJson;
            return this;
        }

        public SavedViewBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public SavedView build() {
            SavedView view = new SavedView();
            view.setName(this.name);
            view.setQueryJson(this.queryJson);
            view.setEntityType(this.entityType);
            return view;
        }
    }
}
