package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Polymorphic Activity entity.
 *
 * Activities can be linked to any CRM entity (Organization, Contact, Opportunity)
 * via the (entityType, entityId) pair — no foreign key needed.
 *
 * Supports: NOTE, EMAIL, CALL, MEETING, TASK
 */
@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activity_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_activity_created", columnList = "created_at")
})
public class Activity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    @Size(max = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    // ─── Polymorphic Association ─────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    // ─── Type-Specific Fields ────────────────────────────────────────────

    /** Duration in seconds (for CALL type) */
    @Column(name = "call_duration")
    private Integer callDuration;

    /** Recipient email (for EMAIL type) */
    @Column(name = "email_to")
    @Size(max = 255)
    private String emailTo;

    /** Meeting date/time (for MEETING type) */
    @Column(name = "meeting_date")
    private LocalDateTime meetingDate;

    /** Due date (for TASK type) */
    @Column(name = "task_due_date")
    private LocalDate taskDueDate;

    /** Completion status (for TASK type) */
    @Column(name = "task_completed")
    private Boolean taskCompleted = false;

    /** Team member assigned to this activity (especially for tasks/meetings) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private com.example.querybuilderapi.model.AuthAccount assignedTo;

    /** Workspace this activity belongs to. Nullable for legacy rows; set at creation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    public Activity() {}

    // ─── Getters & Setters ───────────────────────────────────────────────

    public com.example.querybuilderapi.model.AuthAccount getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(com.example.querybuilderapi.model.AuthAccount assignedTo) {
        this.assignedTo = assignedTo;
    }


    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Integer getCallDuration() {
        return callDuration;
    }

    public void setCallDuration(Integer callDuration) {
        this.callDuration = callDuration;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public LocalDateTime getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(LocalDateTime meetingDate) {
        this.meetingDate = meetingDate;
    }

    public LocalDate getTaskDueDate() {
        return taskDueDate;
    }

    public void setTaskDueDate(LocalDate taskDueDate) {
        this.taskDueDate = taskDueDate;
    }

    public Boolean getTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(Boolean taskCompleted) {
        this.taskCompleted = taskCompleted;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
}
