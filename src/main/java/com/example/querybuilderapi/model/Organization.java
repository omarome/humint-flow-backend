package com.example.querybuilderapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.envers.Audited;

/**
 * Organization entity representing a company/account in the CRM.
 * Maps to the "organizations" table in PostgreSQL.
 *
 * @Audited enables Hibernate Envers to track field-level changes in an _AUD table.
 */
@Entity
@Audited
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Version
    @Column(name = "version")
    private Long version;

    @Column(nullable = false)
    @NotBlank(message = "Organization name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String industry;

    @Size(max = 255)
    private String website;

    @Size(max = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Column(name = "employee_count")
    @Min(value = 0, message = "Employee count must be non-negative")
    private Integer employeeCount;

    @Column(name = "annual_revenue", precision = 15, scale = 2)
    @DecimalMin(value = "0.0", message = "Revenue must be non-negative")
    private java.math.BigDecimal annualRevenue;

    /**
     * The team member assigned to manage this organization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    @org.hibernate.envers.Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
    private User assignedTo;

    public Organization() {}

    // ─── Getters & Setters ───────────────────────────────────────────────

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public java.math.BigDecimal getAnnualRevenue() {
        return annualRevenue;
    }

    public void setAnnualRevenue(java.math.BigDecimal annualRevenue) {
        this.annualRevenue = annualRevenue;
    }
}
