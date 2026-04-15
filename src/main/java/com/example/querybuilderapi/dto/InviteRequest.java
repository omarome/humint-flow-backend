package com.example.querybuilderapi.dto;

import com.example.querybuilderapi.model.AuthAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/admin/invite}.
 *
 * An admin pre-provisions an account so that the invitee can sign in via
 * Firebase (email/password or Google) and have their UID automatically linked.
 */
public class InviteRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Display name is required")
    private String displayName;

    /**
     * The role to assign to the invited user.
     * Defaults to SALES_REP if not provided.
     * ADMIN cannot invite another SUPER_ADMIN via this endpoint.
     */
    @NotNull(message = "Role is required")
    private AuthAccount.Role role = AuthAccount.Role.SALES_REP;

    /** Optional job title shown in the Team directory. */
    private String jobTitle;

    /** Optional department. */
    private String department;

    // ─── Getters & Setters ────────────────────────────────────────────────

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public AuthAccount.Role getRole() { return role; }
    public void setRole(AuthAccount.Role role) { this.role = role; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
