package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.OpportunityRequest;
import com.example.querybuilderapi.dto.OpportunityResponse;
import com.example.querybuilderapi.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<OpportunityResponse> getOpportunity(@PathVariable UUID id) {
        OpportunityResponse opp = opportunityService.getOpportunity(id);
        return ResponseEntity.ok(opp);
    }

    /**
     * POST /api/crm/opportunities
     * Creates a new opportunity. organizationId is required in the body.
     */
    @PostMapping
    public ResponseEntity<OpportunityResponse> createOpportunity(
            @Valid @RequestBody OpportunityRequest request) {
        OpportunityResponse created = opportunityService.createOpportunity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/crm/opportunities/{id}
     * Fully updates an existing opportunity.
     */
    @PutMapping("/{id}")
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
     */
    @PatchMapping("/{id}")
    public ResponseEntity<OpportunityResponse> patchOpportunity(
            @PathVariable UUID id,
            @RequestBody OpportunityRequest request) {
        OpportunityResponse patched = opportunityService.patchOpportunity(id, request);
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /api/crm/opportunities/{id}
     * Soft deletes an opportunity.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOpportunity(@PathVariable UUID id) {
        opportunityService.deleteOpportunity(id);
        return ResponseEntity.noContent().build();
    }
}
