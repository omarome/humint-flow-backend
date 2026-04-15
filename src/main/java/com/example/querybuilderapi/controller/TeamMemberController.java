package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.TeamMemberResponse;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.service.FirebaseClaimsService;
import com.example.querybuilderapi.service.TeamMemberService;
import com.example.querybuilderapi.service.TeamMemberService.TeamMemberUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the Team Management module.
 *
 * Endpoints:
 *   GET    /api/team          — list active members (ADMIN / MANAGER)
 *   GET    /api/team/all      — list all members including inactive (ADMIN only)
 *   GET    /api/team/{id}     — get single member profile (authenticated)
 *   PATCH  /api/team/{id}     — update profile / role / status (ADMIN / MANAGER)
 */
@RestController
@RequestMapping("/api/team")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;
    private final FirebaseClaimsService firebaseClaimsService;

    public TeamMemberController(TeamMemberService teamMemberService,
                                FirebaseClaimsService firebaseClaimsService) {
        this.teamMemberService    = teamMemberService;
        this.firebaseClaimsService = firebaseClaimsService;
    }

    /** Lists all active team members with deal + activity counts. */
    @GetMapping
    @PreAuthorize("@perms.can('TEAM_READ')")
    public ResponseEntity<List<TeamMemberResponse>> listActiveMembers() {
        return ResponseEntity.ok(teamMemberService.listActiveMembers());
    }

    /** Lists all team members including inactive (admin-only view). */
    @GetMapping("/all")
    @PreAuthorize("@perms.can('TEAM_READ_ALL')")
    public ResponseEntity<List<TeamMemberResponse>> listAllMembers() {
        return ResponseEntity.ok(teamMemberService.listAllMembers());
    }

    /** Gets the full profile of a single team member. */
    @GetMapping("/{id}")
    @PreAuthorize("@perms.can('TEAM_READ')")
    public ResponseEntity<TeamMemberResponse> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(teamMemberService.getMember(id));
    }

    /**
     * Partially updates a team member's profile.
     * Role changes are restricted to ADMIN.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("@perms.can('TEAM_EDIT')")
    public ResponseEntity<TeamMemberResponse> updateMember(
            @PathVariable Long id,
            @RequestBody TeamMemberUpdateRequest request) {
        return ResponseEntity.ok(teamMemberService.updateMember(id, request));
    }

    /**
     * Updates a team member's role in PostgreSQL AND syncs Firebase custom claims
     * so that Firestore Security Rules and backend @PreAuthorize checks reflect
     * the change immediately (client must call getIdToken(true) to refresh token).
     *
     * Admin-only endpoint.
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("@perms.can('TEAM_ROLE_ASSIGN')")
    public ResponseEntity<TeamMemberResponse> updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request) {
        firebaseClaimsService.setUserRole(id, request.role(), request.teamId());
        return ResponseEntity.ok(teamMemberService.getMember(id));
    }

    /** Request body for role updates. */
    public record RoleUpdateRequest(AuthAccount.Role role, Long teamId) {}
}
