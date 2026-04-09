package com.example.querybuilderapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating or updating an Opportunity (Deal).
 */
public class OpportunityRequest {

    @NotBlank(message = "Opportunity name is required")
    @Size(max = 255)
    private String name;

    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    private BigDecimal amount;

    @NotBlank(message = "Stage is required")
    private String stage;

    @Min(value = 0, message = "Probability must be between 0 and 100")
    @Max(value = 100, message = "Probability must be between 0 and 100")
    private Integer probability;

    private LocalDate expectedCloseDate;

    private LocalDate actualCloseDate;

    @Size(max = 50)
    private String dealType;

    /**
     * Required: UUID of the parent Organization.
     */
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    /**
     * Optional: UUID of the primary Contact.
     */
    private UUID primaryContactId;

    /**
     * Optional: ID of the assigned user.
     */
    private Long assignedToId;

    public OpportunityRequest() {}

    // ─── Getters & Setters ───────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public Integer getProbability() { return probability; }
    public void setProbability(Integer probability) { this.probability = probability; }

    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDate expectedCloseDate) { this.expectedCloseDate = expectedCloseDate; }

    public LocalDate getActualCloseDate() { return actualCloseDate; }
    public void setActualCloseDate(LocalDate actualCloseDate) { this.actualCloseDate = actualCloseDate; }

    public String getDealType() { return dealType; }
    public void setDealType(String dealType) { this.dealType = dealType; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getPrimaryContactId() { return primaryContactId; }
    public void setPrimaryContactId(UUID primaryContactId) { this.primaryContactId = primaryContactId; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
}
