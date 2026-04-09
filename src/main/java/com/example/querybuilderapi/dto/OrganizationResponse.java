package com.example.querybuilderapi.dto;

import com.example.querybuilderapi.model.Organization;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Organization.
 * Exposes only the fields needed by the frontend, keeping internal
 * audit fields (createdBy/updatedBy) out of the API response.
 */
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String industry;
    private String website;
    private String phone;
    private String address;
    private String city;
    private String country;
    private Integer employeeCount;
    private BigDecimal annualRevenue;
    
    // Assigned Owner
    private Long assignedToId;
    private String assignedToName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrganizationResponse() {}

    /**
     * Factory method to map an Organization entity to a response DTO.
     */
    public static OrganizationResponse fromEntity(Organization org) {
        OrganizationResponse dto = new OrganizationResponse();
        dto.id = org.getId();
        dto.name = org.getName();
        dto.industry = org.getIndustry();
        dto.website = org.getWebsite();
        dto.phone = org.getPhone();
        dto.address = org.getAddress();
        dto.city = org.getCity();
        dto.country = org.getCountry();
        dto.employeeCount = org.getEmployeeCount();
        dto.annualRevenue = org.getAnnualRevenue();
        dto.createdAt = org.getCreatedAt();
        dto.updatedAt = org.getUpdatedAt();

        // Flatten Assigned Owner
        if (org.getAssignedTo() != null) {
            dto.assignedToId = org.getAssignedTo().getId();
            dto.assignedToName = org.getAssignedTo().getFullName();
        }

        return dto;
    }

    // ─── Getters ─────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getIndustry() { return industry; }
    public String getWebsite() { return website; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Integer getEmployeeCount() { return employeeCount; }
    public BigDecimal getAnnualRevenue() { return annualRevenue; }
    
    public Long getAssignedToId() { return assignedToId; }
    public String getAssignedToName() { return assignedToName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
