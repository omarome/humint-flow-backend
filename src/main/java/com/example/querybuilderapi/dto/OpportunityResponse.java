package com.example.querybuilderapi.dto;

import com.example.querybuilderapi.model.Opportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Opportunity.
 * Flattens Organization and Contact relationships into IDs + names
 * to avoid circular reference issues and deep nesting.
 */
public class OpportunityResponse {

    private UUID id;
    private String name;
    private BigDecimal amount;
    private String stage;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    private String dealType;
    
    // Assigned Owner
    private Long assignedToId;
    private String assignedToName;

    // Flattened Organization
    private UUID organizationId;
    private String organizationName;

    // Flattened primary Contact
    private UUID primaryContactId;
    private String primaryContactName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OpportunityResponse() {}

    /**
     * Factory method to map an Opportunity entity to a response DTO.
     * Safely handles null Contact (optional relationship).
     */
    public static OpportunityResponse fromEntity(Opportunity opp) {
        OpportunityResponse dto = new OpportunityResponse();
        dto.id = opp.getId();
        dto.name = opp.getName();
        dto.amount = opp.getAmount();
        dto.stage = opp.getStage();
        dto.probability = opp.getProbability();
        dto.expectedCloseDate = opp.getExpectedCloseDate();
        dto.actualCloseDate = opp.getActualCloseDate();
        dto.dealType = opp.getDealType();
        dto.createdAt = opp.getCreatedAt();
        dto.updatedAt = opp.getUpdatedAt();

        // Flatten Organization (always present — required FK)
        if (opp.getOrganization() != null) {
            dto.organizationId = opp.getOrganization().getId();
            dto.organizationName = opp.getOrganization().getName();
        }

        // Flatten primary Contact (optional FK)
        if (opp.getPrimaryContact() != null) {
            dto.primaryContactId = opp.getPrimaryContact().getId();
            dto.primaryContactName = opp.getPrimaryContact().getFullName();
        }

        // Flatten Assigned Owner
        if (opp.getAssignedTo() != null) {
            dto.assignedToId = opp.getAssignedTo().getId();
            dto.assignedToName = opp.getAssignedTo().getDisplayName();
        }

        return dto;
    }

    // ─── Getters ─────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public String getStage() { return stage; }
    public Integer getProbability() { return probability; }
    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public LocalDate getActualCloseDate() { return actualCloseDate; }
    public String getDealType() { return dealType; }
    public UUID getOrganizationId() { return organizationId; }
    public String getOrganizationName() { return organizationName; }
    public UUID getPrimaryContactId() { return primaryContactId; }
    public String getPrimaryContactName() { return primaryContactName; }
    
    public Long getAssignedToId() { return assignedToId; }
    public String getAssignedToName() { return assignedToName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
