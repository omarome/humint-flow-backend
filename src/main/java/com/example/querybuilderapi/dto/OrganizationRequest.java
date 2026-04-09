package com.example.querybuilderapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating or updating an Organization.
 * Decouples the API contract from the JPA entity.
 */
public class OrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String industry;

    @Size(max = 255)
    private String website;

    @Size(max = 50)
    private String phone;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Min(value = 0, message = "Employee count must be non-negative")
    private Integer employeeCount;

    @DecimalMin(value = "0.0", message = "Revenue must be non-negative")
    private BigDecimal annualRevenue;

    /**
     * Optional: ID of the assigned user.
     */
    private Long assignedToId;

    public OrganizationRequest() {}

    // ─── Getters & Setters ───────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }

    public BigDecimal getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(BigDecimal annualRevenue) { this.annualRevenue = annualRevenue; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
}
