package com.example.querybuilderapi.dto;

import com.example.querybuilderapi.model.Contact;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Contact.
 * Flattens the Organization relationship into organizationId + organizationName
 * to avoid exposing the full nested entity and to prevent circular references.
 */
public class ContactResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;
    private String department;
    private String leadSource;
    private Integer leadScore;
    private String lifecycleStage;
    private UUID organizationId;
    private String organizationName;
    
    // Assigned Owner
    private Long assignedToId;
    private String assignedToName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContactResponse() {}

    /**
     * Factory method to map a Contact entity to a response DTO.
     * Safely handles null Organization (contact may be unlinked).
     */
    public static ContactResponse fromEntity(Contact contact) {
        ContactResponse dto = new ContactResponse();
        dto.id = contact.getId();
        dto.firstName = contact.getFirstName();
        dto.lastName = contact.getLastName();
        dto.fullName = contact.getFullName();
        dto.email = contact.getEmail();
        dto.phone = contact.getPhone();
        dto.jobTitle = contact.getJobTitle();
        dto.department = contact.getDepartment();
        dto.leadSource = contact.getLeadSource();
        dto.leadScore = contact.getLeadScore();
        dto.lifecycleStage = contact.getLifecycleStage();
        dto.createdAt = contact.getCreatedAt();
        dto.updatedAt = contact.getUpdatedAt();

        // Flatten the Organization relationship
        if (contact.getOrganization() != null) {
            dto.organizationId = contact.getOrganization().getId();
            dto.organizationName = contact.getOrganization().getName();
        }

        // Flatten Assigned Owner
        if (contact.getAssignedTo() != null) {
            dto.assignedToId = contact.getAssignedTo().getId();
            dto.assignedToName = contact.getAssignedTo().getFullName();
        }

        return dto;
    }

    // ─── Getters ─────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getLeadSource() { return leadSource; }
    public Integer getLeadScore() { return leadScore; }
    public String getLifecycleStage() { return lifecycleStage; }
    public UUID getOrganizationId() { return organizationId; }
    public String getOrganizationName() { return organizationName; }
    
    public Long getAssignedToId() { return assignedToId; }
    public String getAssignedToName() { return assignedToName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
