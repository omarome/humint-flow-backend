package com.example.querybuilderapi.dto;

import com.example.querybuilderapi.model.AuthAccount;

import java.time.Instant;

/**
 * Read DTO for team member profiles.
 * Includes deal and activity counts for the team dashboard.
 */
public class TeamMemberResponse {

    private Long id;
    private String email;
    private String displayName;
    private String jobTitle;
    private String department;
    private String phone;
    private String avatarUrl;
    private String photoUrl;
    private String role;
    private Boolean isActive;
    private Long managerId;
    private String managerName;
    private String firebaseUid;
    private Instant createdAt;

    // Aggregate stats (populated by service layer)
    private long openDeals;
    private long totalActivities;

    public TeamMemberResponse() {}

    /** Maps an AuthAccount entity to a DTO. Stats default to 0 — caller fills them. */
    public static TeamMemberResponse fromEntity(AuthAccount account) {
        TeamMemberResponse dto = new TeamMemberResponse();
        dto.id          = account.getId();
        dto.email       = account.getEmail();
        dto.displayName = account.getDisplayName();
        dto.jobTitle    = account.getJobTitle();
        dto.department  = account.getDepartment();
        dto.phone       = account.getPhone();
        dto.avatarUrl   = account.getAvatarUrl();
        dto.photoUrl    = account.getPhotoUrl();
        dto.role        = account.getRole().name();
        dto.isActive    = account.getIsActive();
        dto.firebaseUid = account.getFirebaseUid();
        dto.createdAt   = account.getCreatedAt();
        if (account.getManager() != null) {
            dto.managerId   = account.getManager().getId();
            dto.managerName = account.getManager().getDisplayName();
        }
        return dto;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public long getOpenDeals() { return openDeals; }
    public void setOpenDeals(long openDeals) { this.openDeals = openDeals; }

    public long getTotalActivities() { return totalActivities; }
    public void setTotalActivities(long totalActivities) { this.totalActivities = totalActivities; }
}
