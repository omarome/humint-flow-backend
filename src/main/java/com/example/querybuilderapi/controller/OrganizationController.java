package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.OrganizationRequest;
import com.example.querybuilderapi.dto.OrganizationResponse;
import com.example.querybuilderapi.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Organization CRUD operations.
 *
 * Base path: /api/crm/organizations
 */
@RestController
@RequestMapping("/api/sales/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * GET /api/crm/organizations
     * Returns a paginated list of organizations.
     *
     * @param page    zero-based page index (default 0)
     * @param size    items per page (default 20)
     * @param sortBy  field to sort by (default "name")
     * @param sortDir "asc" or "desc" (default "asc")
     * @param search  optional name search query
     */
    @GetMapping
    @PreAuthorize("@perms.can('ORGANIZATIONS_READ')")
    public ResponseEntity<Page<OrganizationResponse>> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {

        Page<OrganizationResponse> result;
        if (search != null && !search.isBlank()) {
            result = organizationService.searchOrganizations(search.trim(), page, size);
        } else {
            result = organizationService.listOrganizations(page, size, sortBy, sortDir);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/crm/organizations/{id}
     * Returns a single organization by UUID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@perms.can('ORGANIZATIONS_READ')")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID id) {
        OrganizationResponse org = organizationService.getOrganization(id);
        return ResponseEntity.ok(org);
    }

    /**
     * POST /api/crm/organizations
     * Creates a new organization.
     */
    @PostMapping
    @PreAuthorize("@perms.can('ORGANIZATIONS_CREATE')")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request) {
        OrganizationResponse created = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/crm/organizations/{id}
     * Fully updates an existing organization.
     * Ownership enforcement (SALES_REP → own records only) is handled in the service layer.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@perms.can(\'ORGANIZATIONS_UPDATE\')")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request) {
        OrganizationResponse updated = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/crm/organizations/{id}
     * Partially updates an organization (only non-null fields).
     * Ownership enforcement (SALES_REP → own records only) is handled in the service layer.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("@perms.can(\'ORGANIZATIONS_UPDATE\')")
    public ResponseEntity<OrganizationResponse> patchOrganization(
            @PathVariable UUID id,
            @RequestBody OrganizationRequest request) {
        OrganizationResponse patched = organizationService.patchOrganization(id, request);
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /api/crm/organizations/{id}
     * Soft deletes an organization. Restricted to ADMIN and MANAGER.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.can('ORGANIZATIONS_DELETE')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable UUID id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}
