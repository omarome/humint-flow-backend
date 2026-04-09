package com.example.querybuilderapi.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * Request DTO for creating or updating a Contact.
 * Decouples the API contract from the JPA entity.
 */
public class ContactRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Email(message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    @Size(max = 150)
    private String jobTitle;

    @Size(max = 100)
    private String department;

    @Size(max = 50)
    private String leadSource;

    @Min(value = 0, message = "Lead score must be non-negative")
    private Integer leadScore;

    private String lifecycleStage;

    /**
     * Optional UUID of the parent Organization.
     * Null means this contact is not linked to any org.
     */
    private UUID organizationId;

    /**
     * Optional ID of the assigned user.
     */
    private Long assignedToId;

    public ContactRequest() {}

    // ─── Getters & Setters ───────────────────────────────────────────────

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLeadSource() { return leadSource; }
    public void setLeadSource(String leadSource) { this.leadSource = leadSource; }

    public Integer getLeadScore() { return leadScore; }
    public void setLeadScore(Integer leadScore) { this.leadScore = leadScore; }

    public String getLifecycleStage() { return lifecycleStage; }
    public void setLifecycleStage(String lifecycleStage) { this.lifecycleStage = lifecycleStage; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
}
