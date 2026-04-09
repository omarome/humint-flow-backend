package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.envers.Audited;

import java.util.Map;

/**
 * Entity representing a no-code automation rule.
 * Example: "When Opportunity Stage is CLOSED_WON, Create a Task"
 */
@Entity
@Audited
@Table(name = "automation_rules", indexes = {
    @Index(name = "idx_automation_trigger", columnList = "trigger_entity, trigger_event"),
    @Index(name = "idx_automation_active", columnList = "is_active")
})
public class AutomationRule extends BaseEntity {

    @NotBlank(message = "Rule name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ─── Trigger Configuration ──────────────────────────────────────────

    @NotBlank(message = "Trigger Entity is required")
    @Column(name = "trigger_entity", nullable = false)
    private String triggerEntity; // OPPORTUNITY, CONTACT, ORGANIZATION, etc.

    @NotBlank(message = "Trigger Event is required")
    @Column(name = "trigger_event", nullable = false)
    private String triggerEvent; // STAGE_CHANGED, CREATED, UPDATED

    /**
     * JSON payload defining conditions.
     * Example: {"field": "stage", "value": "CLOSED_WON"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private Map<String, Object> triggerConfig;

    // ─── Action Configuration ───────────────────────────────────────────

    @NotBlank(message = "Action Type is required")
    @Column(name = "action_type", nullable = false)
    private String actionType; // CREATE_TASK, SEND_NOTIFICATION, SEND_EMAIL

    /**
     * JSON payload defining the action inputs.
     * Example: {"title": "Send Onboarding Kit", "assignTo": "manager"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_config", columnDefinition = "jsonb")
    private Map<String, Object> actionConfig;

    public AutomationRule() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getTriggerEntity() {
        return triggerEntity;
    }

    public void setTriggerEntity(String triggerEntity) {
        this.triggerEntity = triggerEntity;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public Map<String, Object> getTriggerConfig() {
        return triggerConfig;
    }

    public void setTriggerConfig(Map<String, Object> triggerConfig) {
        this.triggerConfig = triggerConfig;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(Map<String, Object> actionConfig) {
        this.actionConfig = actionConfig;
    }
}
