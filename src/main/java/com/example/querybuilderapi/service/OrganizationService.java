package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.OrganizationRequest;
import com.example.querybuilderapi.dto.OrganizationResponse;
import com.example.querybuilderapi.model.Organization;
import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.OrganizationRepository;
import com.example.querybuilderapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for Organization CRUD operations.
 * Handles audit field population, soft deletes, and DTO mapping.
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditAwareService auditAwareService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               AuditAwareService auditAwareService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.auditAwareService = auditAwareService;
    }

    /**
     * Returns a paginated list of non-deleted organizations.
     *
     * @param page     zero-based page index
     * @param size     number of items per page
     * @param sortBy   field to sort by (default: "name")
     * @param sortDir  sort direction: "asc" or "desc" (default: "asc")
     */
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> listOrganizations(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                            sortBy != null ? sortBy : "name");
        Pageable pageable = PageRequest.of(page, size, sort);
        return organizationRepository.findAllByIsDeletedFalse(pageable)
                .map(OrganizationResponse::fromEntity);
    }

    /**
     * Finds a single non-deleted organization by ID.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID id) {
        Organization org = organizationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        return OrganizationResponse.fromEntity(org);
    }

    /**
     * Creates a new organization from the given request DTO.
     */
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        Organization org = new Organization();
        mapRequestToEntity(request, org);
        resolveAssignee(request.getAssignedToId(), org);

        Long currentUserId = auditAwareService.getCurrentUserId();
        org.setCreatedBy(currentUserId);
        org.setUpdatedBy(currentUserId);

        Organization saved = organizationRepository.save(org);
        return OrganizationResponse.fromEntity(saved);
    }

    /**
     * Fully updates an existing organization.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        mapRequestToEntity(request, org);
        resolveAssignee(request.getAssignedToId(), org);
        org.setUpdatedBy(auditAwareService.getCurrentUserId());

        Organization saved = organizationRepository.save(org);
        return OrganizationResponse.fromEntity(saved);
    }

    /**
     * Partially updates an organization (only non-null fields from request).
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public OrganizationResponse patchOrganization(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        if (request.getName() != null) org.setName(request.getName());
        if (request.getIndustry() != null) org.setIndustry(request.getIndustry());
        if (request.getWebsite() != null) org.setWebsite(request.getWebsite());
        if (request.getPhone() != null) org.setPhone(request.getPhone());
        if (request.getAddress() != null) org.setAddress(request.getAddress());
        if (request.getCity() != null) org.setCity(request.getCity());
        if (request.getCountry() != null) org.setCountry(request.getCountry());
        if (request.getEmployeeCount() != null) org.setEmployeeCount(request.getEmployeeCount());
        if (request.getAnnualRevenue() != null) org.setAnnualRevenue(request.getAnnualRevenue());
        if (request.getAssignedToId() != null) resolveAssignee(request.getAssignedToId(), org);

        org.setUpdatedBy(auditAwareService.getCurrentUserId());

        Organization saved = organizationRepository.save(org);
        return OrganizationResponse.fromEntity(saved);
    }

    /**
     * Soft deletes an organization (sets isDeleted = true).
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public void deleteOrganization(UUID id) {
        Organization org = organizationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));

        org.softDelete();
        org.setUpdatedBy(auditAwareService.getCurrentUserId());
        organizationRepository.save(org);
    }

    /**
     * Search organizations by name.
     */
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> searchOrganizations(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return organizationRepository.searchByName(query, pageable)
                .map(OrganizationResponse::fromEntity);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private void mapRequestToEntity(OrganizationRequest request, Organization org) {
        org.setName(request.getName());
        org.setIndustry(request.getIndustry());
        org.setWebsite(request.getWebsite());
        org.setPhone(request.getPhone());
        org.setAddress(request.getAddress());
        org.setCity(request.getCity());
        org.setCountry(request.getCountry());
        org.setEmployeeCount(request.getEmployeeCount());
        org.setAnnualRevenue(request.getAnnualRevenue());
    }

    /**
     * Resolves the assigned user.
     */
    private void resolveAssignee(Long assignedToId, Organization org) {
        if (assignedToId == null) {
            org.setAssignedTo(null);
            return;
        }
        User user = userRepository.findById(assignedToId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + assignedToId));
        org.setAssignedTo(user);
    }
}
