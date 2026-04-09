package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.envers.Audited;

/**
 * Contact entity representing a person in the CRM.
 * Contacts belong to an Organization via a @ManyToOne relationship.
 *
 * Maps to the "contacts" table in PostgreSQL.
 * @Audited enables Hibernate Envers to track field-level changes.
 */
@Entity
@Audited
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contact_org", columnList = "organization_id"),
    @Index(name = "idx_contact_email", columnList = "email"),
    @Index(name = "idx_contact_name", columnList = "last_name, first_name"),
    @Index(name = "idx_contact_lifecycle", columnList = "lifecycle_stage"),
    @Index(name = "idx_contact_is_deleted", columnList = "is_deleted")
})
public class Contact extends BaseEntity {

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Column(unique = true)
    @Email(message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    @Column(name = "job_title")
    @Size(max = 150)
    private String jobTitle;

    @Size(max = 100)
    private String department;

    @Column(name = "lead_source")
    @Size(max = 50)
    private String leadSource;

    @Column(name = "lead_score")
    @Min(value = 0, message = "Lead score must be non-negative")
    private Integer leadScore = 0;

    @Column(name = "lifecycle_stage", length = 50)
    private String lifecycleStage = LifecycleStage.LEAD.name();

    // ─── Relationship ────────────────────────────────────────────────────

    /**
     * Many contacts belong to one organization.
     * FetchType.LAZY is the best practice — fetched on demand,
     * avoiding unnecessary JOINs when listing contacts.
     * Use @EntityGraph or JOIN FETCH when you DO need the org data.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * The team member assigned to this contact.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    @org.hibernate.envers.Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
    private User assignedTo;

    public Contact() {}

    // ─── Lifecycle Stage Enum ────────────────────────────────────────────

    /**
     * CRM lifecycle stages for lead-to-customer progression.
     */
    public enum LifecycleStage {
        LEAD,           // Initial capture
        MQL,            // Marketing Qualified Lead
        SQL,            // Sales Qualified Lead
        OPPORTUNITY,    // Linked to a deal
        CUSTOMER,       // Closed-Won
        EVANGELIST      // Happy customer, referral source
    }

    // ─── Derived Property ────────────────────────────────────────────────

    /**
     * Returns the full name by combining first and last name.
     */
    public String getFullName() {
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    // ─── Getters & Setters ───────────────────────────────────────────────

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLeadSource() {
        return leadSource;
    }

    public void setLeadSource(String leadSource) {
        this.leadSource = leadSource;
    }

    public Integer getLeadScore() {
        return leadScore;
    }

    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    public String getLifecycleStage() {
        return lifecycleStage;
    }

    public void setLifecycleStage(String lifecycleStage) {
        this.lifecycleStage = lifecycleStage;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
