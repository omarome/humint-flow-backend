package com.example.querybuilderapi.dto;

import com.example.querybuilderapi.model.Activity;
import com.example.querybuilderapi.model.ActivityType;
import com.example.querybuilderapi.model.EntityType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Activity — returned to the frontend.
 */
public class ActivityResponse {

    private UUID id;
    private ActivityType activityType;
    private EntityType entityType;
    private UUID entityId;
    private String subject;
    private String body;

    // Type-specific fields
    private Integer callDuration;
    private String emailTo;
    private LocalDateTime meetingDate;
    private LocalDate taskDueDate;
    private Boolean taskCompleted;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;

    /**
     * Factory method to convert an Activity entity to a response DTO.
     */
    public static ActivityResponse fromEntity(Activity activity) {
        ActivityResponse r = new ActivityResponse();
        r.id = activity.getId();
        r.activityType = activity.getActivityType();
        r.entityType = activity.getEntityType();
        r.entityId = activity.getEntityId();
        r.subject = activity.getSubject();
        r.body = activity.getBody();
        r.callDuration = activity.getCallDuration();
        r.emailTo = activity.getEmailTo();
        r.meetingDate = activity.getMeetingDate();
        r.taskDueDate = activity.getTaskDueDate();
        r.taskCompleted = activity.getTaskCompleted();
        r.createdAt = activity.getCreatedAt();
        r.updatedAt = activity.getUpdatedAt();
        r.createdBy = activity.getCreatedBy();
        return r;
    }

    // ─── Getters ─────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public ActivityType getActivityType() { return activityType; }
    public EntityType getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Integer getCallDuration() { return callDuration; }
    public String getEmailTo() { return emailTo; }
    public LocalDateTime getMeetingDate() { return meetingDate; }
    public LocalDate getTaskDueDate() { return taskDueDate; }
    public Boolean getTaskCompleted() { return taskCompleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
}
