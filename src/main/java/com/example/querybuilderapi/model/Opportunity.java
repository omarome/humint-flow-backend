package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.envers.Audited;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Opportunity (Deal) entity representing a sales opportunity in the CRM.
 * Has required @ManyToOne to Organization and optional @ManyToOne to Contact.
 *
 * Maps to the "opportunities" table in PostgreSQL.
 * @Audited enables Hibernate Envers to track field-level changes.
 */
@Entity
@Audited
@Table(name = "opportunities", indexes = {
    @Index(name = "idx_opp_org", columnList = "organization_id"),
    @Index(name = "idx_opp_contact", columnList = "primary_contact_id"),
    @Index(name = "idx_opp_stage", columnList = "stage"),
    @Index(name = "idx_opp_close_date", columnList = "expected_close_date"),
    @Index(name = "idx_opp_is_deleted", columnList = "is_deleted"),
    @Index(name = "idx_opp_assignee", columnList = "assigned_to_id")
})
public class Opportunity extends BaseEntity {

    /** Optimistic locking — prevents lost-update conflicts on concurrent edits. */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(nullable = false)
    @NotBlank(message = "Opportunity name is required")
    @Size(max = 255)
    private String name;

    @Column(precision = 15, scale = 2)
    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Stage is required")
    private String stage = DealStage.PROSPECTING.name();

    @Column
    @Min(value = 0, message = "Probability must be between 0 and 100")
    @Max(value = 100, message = "Probability must be between 0 and 100")
    private Integer probability = 0;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;

    @Column(name = "deal_type", length = 50)
    @Size(max = 50)
    private String dealType;

    // ─── Relationships ───────────────────────────────────────────────────

    /**
     * Required: Every deal must belong to an Organization.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Optional: Primary contact associated with this deal.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contact_id")
    private Contact primaryContact;

    /**
     * The AuthAccount (team member) assigned to lead this deal/opportunity.
     * Replaces the legacy User entity — aligned with Firebase-backed auth.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    @org.hibernate.envers.Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
    private AuthAccount assignedTo;

    public Opportunity() {}

    // ─── Deal Stage Enum ─────────────────────────────────────────────────

    /**
     * Standard CRM deal pipeline stages.
     * Each stage has a default probability percentage.
     */
    public enum DealStage {
        PROSPECTING(10),
        QUALIFICATION(20),
        NEEDS_ANALYSIS(40),
        VALUE_PROPOSITION(60),
        NEGOTIATION(80),
        CLOSED_WON(100),
        CLOSED_LOST(0);

        private final int defaultProbability;

        DealStage(int defaultProbability) {
            this.defaultProbability = defaultProbability;
        }

        public int getDefaultProbability() {
            return defaultProbability;
        }
    }

    // ─── Getters & Setters ───────────────────────────────────────────────

    public AuthAccount getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(AuthAccount assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public LocalDate getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(LocalDate expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public LocalDate getActualCloseDate() {
        return actualCloseDate;
    }

    public void setActualCloseDate(LocalDate actualCloseDate) {
        this.actualCloseDate = actualCloseDate;
    }

    public String getDealType() {
        return dealType;
    }

    public void setDealType(String dealType) {
        this.dealType = dealType;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Contact getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(Contact primaryContact) {
        this.primaryContact = primaryContact;
    }
}
