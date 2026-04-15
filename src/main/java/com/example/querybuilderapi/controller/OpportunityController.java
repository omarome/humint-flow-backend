package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.OpportunityRequest;
import com.example.querybuilderapi.dto.OpportunityResponse;
import com.example.querybuilderapi.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Opportunity (Deal) CRUD operations.
 *
 * Base path: /api/crm/opportunities
 */
@RestController
@RequestMapping("/api/sales/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    /**
     * GET /api/crm/opportunities
     * Returns opportunities — filterable by organization, contact, stage, or search.
     */
    @GetMapping
    @PreAuthorize("@perms.can('OPPORTUNITIES_READ')")
    public ResponseEntity<?> listOpportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID contactId,
            @RequestParam(required = false) String stage) {

        // Filter by organization
        if (organizationId != null) {
            List<OpportunityResponse> opps = opportunityService.getOpportunitiesByOrganization(organizationId);
            return ResponseEntity.ok(opps);
        }

        // Filter by contact
        if (contactId != null) {
            List<OpportunityResponse> opps = opportunityService.getOpportunitiesByContact(contactId);
            return ResponseEntity.ok(opps);
        }

        // Filter by stage (for Kanban pipeline view)
        if (stage != null && !stage.isBlank()) {
            List<OpportunityResponse> opps = opportunityService.getOpportunitiesByStage(stage.trim());
            return ResponseEntity.ok(opps);
        }

        // Search by name
        if (search != null && !search.isBlank()) {
            Page<OpportunityResponse> result = opportunityService.searchOpportunities(search.trim(), page, size);
            return ResponseEntity.ok(result);
        }

        // Default: paginated list
        Page<OpportunityResponse> result = opportunityService.listOpportunities(page, size, sortBy, sortDir);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/crm/opportunities/{id}
     * Returns a single opportunity with flattened Organization + Contact data.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@perms.can('OPPORTUNITIES_READ')")
    public ResponseEntity<OpportunityResponse> getOpportunity(@PathVariable UUID id) {
        OpportunityResponse opp = opportunityService.getOpportunity(id);
        return ResponseEntity.ok(opp);
    }

    /**
     * POST /api/crm/opportunities
     * Creates a new opportunity. organizationId is required in the body.
     */
    @PostMapping
    @PreAuthorize("@perms.can('OPPORTUNITIES_CREATE')")
    public ResponseEntity<OpportunityResponse> createOpportunity(
            @Valid @RequestBody OpportunityRequest request) {
        OpportunityResponse created = opportunityService.createOpportunity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/crm/opportunities/{id}
     * Fully updates an existing opportunity.
     * Ownership enforcement (SALES_REP → own deals only) is handled in the service layer.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@perms.can('OPPORTUNITIES_UPDATE')")
    public ResponseEntity<OpportunityResponse> updateOpportunity(
            @PathVariable UUID id,
            @Valid @RequestBody OpportunityRequest request) {
        OpportunityResponse updated = opportunityService.updateOpportunity(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/crm/opportunities/{id}
     * Partially updates an opportunity (only non-null fields).
     * Used for drag-and-drop stage changes in the Kanban view.
     * SALES_REP is allowed here because reps move their own deals through Kanban;
     * ownership enforcement (rep can only patch their own deal) is in the service layer.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("@perms.can('OPPORTUNITIES_UPDATE')")
    public ResponseEntity<OpportunityResponse> patchOpportunity(
            @PathVariable UUID id,
            @RequestBody OpportunityRequest request) {
        OpportunityResponse patched = opportunityService.patchOpportunity(id, request);
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /api/crm/opportunities/{id}
     * Soft deletes an opportunity. Restricted to ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.can('OPPORTUNITIES_DELETE')")
    public ResponseEntity<Void> deleteOpportunity(@PathVariable UUID id) {
        opportunityService.deleteOpportunity(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/crm/opportunities/{id}/share
     * Shares an opportunity explicitly with a specific user account. Required for GUEST flow.
     */
    @PostMapping("/{id}/share")
    @PreAuthorize("@perms.can('ADMIN_MANAGE')")
    public ResponseEntity<Void> shareOpportunity(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> payload) {
        Long accountId = Long.valueOf(payload.get("accountId").toString());
        String permission = payload.getOrDefault("permission", "READ").toString();
        opportunityService.shareOpportunity(id, accountId, permission);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/crm/opportunities/{id}/share/{accountId}
     * Revokes a direct record share.
     */
    @DeleteMapping("/{id}/share/{accountId}")
    @PreAuthorize("@perms.can('ADMIN_MANAGE')")
    public ResponseEntity<Void> unshareOpportunity(
            @PathVariable UUID id,
            @PathVariable Long accountId) {
        opportunityService.unshareOpportunity(id, accountId);
        return ResponseEntity.noContent().build();
    }
}
