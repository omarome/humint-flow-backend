package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.TeamMemberResponse;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.ActivityRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for the Team Management module.
 *
 * Provides:
 *  - Listing all active team members with stats
 *  - Getting an individual member profile
 *  - Updating profile fields (job title, department, phone, avatar, role)
 *  - Toggling active/inactive status
 */
@Service
public class TeamMemberService {

    private final AuthAccountRepository authAccountRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;

    public TeamMemberService(AuthAccountRepository authAccountRepository,
                             OpportunityRepository opportunityRepository,
                             ActivityRepository activityRepository) {
        this.authAccountRepository = authAccountRepository;
        this.opportunityRepository = opportunityRepository;
        this.activityRepository    = activityRepository;
    }

    /**
     * Returns all active team members, each enriched with open deal + activity counts.
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listActiveMembers() {
        return authAccountRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::toResponseWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Returns all team members (both active and inactive).
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listAllMembers() {
        return authAccountRepository.findAll()
                .stream()
                .map(this::toResponseWithStats)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single member by DB id.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public TeamMemberResponse getMember(Long id) {
        AuthAccount account = authAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found: " + id));
        return toResponseWithStats(account);
    }

    /**
     * Updates mutable profile fields on a team member.
     * Role changes are also applied here; they will be mirrored to Firebase Custom Claims
     * by {@link FirebaseClaimsService} (Phase 7).
     */
    @Transactional
    public TeamMemberResponse updateMember(Long id, TeamMemberUpdateRequest request) {
        AuthAccount account = authAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found: " + id));

        if (request.getDisplayName() != null) account.setDisplayName(request.getDisplayName());
        if (request.getJobTitle()    != null) account.setJobTitle(request.getJobTitle());
        if (request.getDepartment()  != null) account.setDepartment(request.getDepartment());
        if (request.getPhone()       != null) account.setPhone(request.getPhone());
        if (request.getAvatarUrl()   != null) account.setAvatarUrl(request.getAvatarUrl());
        if (request.getRole()        != null) {
            try {
                account.setRole(AuthAccount.Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }
        if (request.getIsActive() != null) account.setIsActive(request.getIsActive());

        AuthAccount saved = authAccountRepository.save(account);
        return toResponseWithStats(saved);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private TeamMemberResponse toResponseWithStats(AuthAccount account) {
        TeamMemberResponse dto = TeamMemberResponse.fromEntity(account);
        // Count open (non-deleted) opportunities assigned to this member
        dto.setOpenDeals(opportunityRepository.countByAssignedToIdAndIsDeletedFalse(account.getId()));
        // Count total activities created by this member via createdBy audit field
        dto.setTotalActivities(activityRepository.countByCreatedBy(account.getId()));
        return dto;
    }

    // ─── Nested request DTO ───────────────────────────────────────────────

    public static class TeamMemberUpdateRequest {
        private String displayName;
        private String jobTitle;
        private String department;
        private String phone;
        private String avatarUrl;
        private String role;
        private Boolean isActive;

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
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}
