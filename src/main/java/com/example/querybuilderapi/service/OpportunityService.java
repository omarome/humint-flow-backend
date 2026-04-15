package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.OpportunityRequest;
import com.example.querybuilderapi.dto.OpportunityResponse;
import com.example.querybuilderapi.event.OpportunityStageChangedEvent;
import com.example.querybuilderapi.model.Contact;
import com.example.querybuilderapi.model.Opportunity;
import com.example.querybuilderapi.model.Organization;
import com.example.querybuilderapi.model.AuthAccount;
import com.example.querybuilderapi.repository.AuthAccountRepository;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import com.example.querybuilderapi.model.RecordShare;
import com.example.querybuilderapi.model.EntityType;
import com.example.querybuilderapi.repository.RecordShareRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Opportunity (Deal) CRUD operations.
 * Handles dual FK resolution (Organization + Contact), audit fields,
 * soft deletes, and DTO mapping.
 */
@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OrganizationRepository organizationRepository;
    private final ContactRepository contactRepository;
    private final AuthAccountRepository authAccountRepository;
    private final AuditAwareService auditAwareService;
    private final ApplicationEventPublisher eventPublisher;
    private final FirestoreSyncService firestoreSyncService;
    private final CrmEventPublisher crmEventPublisher;
    private final RecordShareRepository recordShareRepository;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              OrganizationRepository organizationRepository,
                              ContactRepository contactRepository,
                              AuthAccountRepository authAccountRepository,
                              AuditAwareService auditAwareService,
                              ApplicationEventPublisher eventPublisher,
                              FirestoreSyncService firestoreSyncService,
                              CrmEventPublisher crmEventPublisher,
                              RecordShareRepository recordShareRepository) {
        this.opportunityRepository = opportunityRepository;
        this.organizationRepository = organizationRepository;
        this.contactRepository = contactRepository;
        this.authAccountRepository = authAccountRepository;
        this.auditAwareService = auditAwareService;
        this.eventPublisher = eventPublisher;
        this.firestoreSyncService = firestoreSyncService;
        this.crmEventPublisher = crmEventPublisher;
        this.recordShareRepository = recordShareRepository;
    }

    /**
     * Returns a paginated list of non-deleted opportunities.
     */
    @Transactional(readOnly = true)
    public Page<OpportunityResponse> listOpportunities(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                            sortBy != null ? sortBy : "name");
        Pageable pageable = PageRequest.of(page, size, sort);
        return opportunityRepository.findAllByIsDeletedFalse(pageable)
                .map(OpportunityResponse::fromEntity);
    }

    /**
     * Finds a single non-deleted opportunity by ID.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public OpportunityResponse getOpportunity(UUID id) {
        Opportunity opp = opportunityRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + id));
        return OpportunityResponse.fromEntity(opp);
    }

    /**
     * Finds all non-deleted opportunities for a given organization.
     */
    @Transactional(readOnly = true)
    public List<OpportunityResponse> getOpportunitiesByOrganization(UUID organizationId) {
        return opportunityRepository.findAllByOrganizationIdAndIsDeletedFalse(organizationId)
                .stream()
                .map(OpportunityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Finds all non-deleted opportunities linked to a specific contact.
     */
    @Transactional(readOnly = true)
    public List<OpportunityResponse> getOpportunitiesByContact(UUID contactId) {
        return opportunityRepository.findAllByPrimaryContactIdAndIsDeletedFalse(contactId)
                .stream()
                .map(OpportunityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Finds all non-deleted opportunities in a given stage (for Kanban view).
     */
    @Transactional(readOnly = true)
    public List<OpportunityResponse> getOpportunitiesByStage(String stage) {
        return opportunityRepository.findAllByStageAndIsDeletedFalse(stage)
                .stream()
                .map(OpportunityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new opportunity.
     *
     * @throws IllegalArgumentException if Organization or Contact not found
     */
    @Transactional
    public OpportunityResponse createOpportunity(OpportunityRequest request) {
        Opportunity opp = new Opportunity();
        mapRequestToEntity(request, opp);

        resolveOrganization(request.getOrganizationId(), opp);
        resolveContact(request.getPrimaryContactId(), opp);
        resolveAssignee(request.getAssignedToId(), opp);

        Long currentUserId = auditAwareService.getCurrentUserId();
        opp.setCreatedBy(currentUserId);
        opp.setUpdatedBy(currentUserId);

        Opportunity saved = opportunityRepository.save(opp);
        firestoreSyncService.syncOpportunity(saved);
        crmEventPublisher.publishCreated("OPPORTUNITY", saved.getId());
        return OpportunityResponse.fromEntity(saved);
    }

    /**
     * Fully updates an existing opportunity.
     *
     * @throws IllegalArgumentException if opportunity, org, or contact not found
     */
    @Transactional
    public OpportunityResponse updateOpportunity(UUID id, OpportunityRequest request) {
        Opportunity opp = opportunityRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + id));

        String oldStage = opp.getStage();
        mapRequestToEntity(request, opp);
        
        resolveOrganization(request.getOrganizationId(), opp);
        resolveContact(request.getPrimaryContactId(), opp);
        resolveAssignee(request.getAssignedToId(), opp);
        opp.setUpdatedBy(auditAwareService.getCurrentUserId());

        Opportunity saved = opportunityRepository.save(opp);
        firestoreSyncService.syncOpportunity(saved);
        crmEventPublisher.publishUpdated("OPPORTUNITY", saved.getId());

        if (oldStage != null && !oldStage.equals(saved.getStage())) {
            eventPublisher.publishEvent(new OpportunityStageChangedEvent(this, saved, oldStage, saved.getStage()));
        }

        return OpportunityResponse.fromEntity(saved);
    }

    /**
     * Partially updates an opportunity (only non-null fields).
     *
     * @throws IllegalArgumentException if opportunity, org, or contact not found
     */
    @Transactional
    public OpportunityResponse patchOpportunity(UUID id, OpportunityRequest request) {
        Opportunity opp = opportunityRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + id));

        String oldStage = opp.getStage();

        if (request.getName() != null) opp.setName(request.getName());
        if (request.getAmount() != null) opp.setAmount(request.getAmount());
        if (request.getStage() != null) opp.setStage(request.getStage());
        if (request.getProbability() != null) opp.setProbability(request.getProbability());
        if (request.getExpectedCloseDate() != null) opp.setExpectedCloseDate(request.getExpectedCloseDate());
        if (request.getActualCloseDate() != null) opp.setActualCloseDate(request.getActualCloseDate());
        if (request.getDealType() != null) opp.setDealType(request.getDealType());
        if (request.getOrganizationId() != null) resolveOrganization(request.getOrganizationId(), opp);
        if (request.getPrimaryContactId() != null) resolveContact(request.getPrimaryContactId(), opp);
        if (request.getAssignedToId() != null) resolveAssignee(request.getAssignedToId(), opp);

        opp.setUpdatedBy(auditAwareService.getCurrentUserId());

        Opportunity saved = opportunityRepository.save(opp);
        firestoreSyncService.syncOpportunity(saved);
        crmEventPublisher.publishUpdated("OPPORTUNITY", saved.getId());

        if (request.getStage() != null && !oldStage.equals(saved.getStage())) {
            eventPublisher.publishEvent(new OpportunityStageChangedEvent(this, saved, oldStage, saved.getStage()));
        }

        return OpportunityResponse.fromEntity(saved);
    }

    /**
     * Soft deletes an opportunity.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public void deleteOpportunity(UUID id) {
        Opportunity opp = opportunityRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + id));

        opp.softDelete();
        opp.setUpdatedBy(auditAwareService.getCurrentUserId());
        opportunityRepository.save(opp);
        firestoreSyncService.deleteOpportunity(id.toString());
        crmEventPublisher.publishDeleted("OPPORTUNITY", id);
    }

    /**
     * Search opportunities by name.
     */
    @Transactional(readOnly = true)
    public Page<OpportunityResponse> searchOpportunities(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return opportunityRepository.searchByName(query, pageable)
                .map(OpportunityResponse::fromEntity);
    }

    /**
     * Shares an opportunity with a user (GUEST or otherwise).
     */
    @Transactional
    public void shareOpportunity(UUID opportunityId, Long accountId, String permission) {
        Opportunity opp = opportunityRepository.findByIdAndIsDeletedFalse(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));
        AuthAccount user = authAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
                
        // Ensure no duplicate shares
        recordShareRepository.findByWorkspaceIdAndResourceTypeAndResourceIdAndSharedWithId(
            opp.getWorkspace().getId(), EntityType.OPPORTUNITY, opp.getId(), accountId)
            .ifPresent(share -> { throw new IllegalStateException("Already shared"); });

        RecordShare share = new RecordShare(opp.getWorkspace(), EntityType.OPPORTUNITY, opp.getId(), user, permission);
        recordShareRepository.save(share);
        
        // Push update to Firestore so proper UID gets into the sharedWith array
        firestoreSyncService.syncOpportunity(opp);
    }

    /**
     * Un-shares an opportunity with a user.
     */
    @Transactional
    public void unshareOpportunity(UUID opportunityId, Long accountId) {
        Opportunity opp = opportunityRepository.findByIdAndIsDeletedFalse(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));
        
        recordShareRepository.deleteByWorkspaceIdAndResourceTypeAndResourceIdAndSharedWithId(
            opp.getWorkspace().getId(), EntityType.OPPORTUNITY, opp.getId(), accountId);
            
        // Push update to Firestore
        firestoreSyncService.syncOpportunity(opp);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private void mapRequestToEntity(OpportunityRequest request, Opportunity opp) {
        opp.setName(request.getName());
        opp.setAmount(request.getAmount());
        opp.setStage(request.getStage() != null ? request.getStage() : Opportunity.DealStage.PROSPECTING.name());
        opp.setProbability(request.getProbability() != null ? request.getProbability() : 0);
        opp.setExpectedCloseDate(request.getExpectedCloseDate());
        opp.setActualCloseDate(request.getActualCloseDate());
        opp.setDealType(request.getDealType());
    }

    /**
     * Resolves the required Organization FK.
     *
     * @throws IllegalArgumentException if org not found or deleted
     */
    private void resolveOrganization(UUID organizationId, Opportunity opp) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required for an Opportunity");
        }
        Organization org = organizationRepository.findByIdAndIsDeletedFalse(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
        opp.setOrganization(org);
    }

    /**
     * Resolves the optional Contact FK.
     * Null contactId clears the relationship.
     *
     * @throws IllegalArgumentException if contact not found or deleted
     */
    private void resolveContact(UUID contactId, Opportunity opp) {
        if (contactId == null) {
            opp.setPrimaryContact(null);
            return;
        }
        Contact contact = contactRepository.findByIdAndIsDeletedFalse(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));
        opp.setPrimaryContact(contact);
    }

    /**
     * Resolves the assigned team member.
     */
    private void resolveAssignee(Long assignedToId, Opportunity opp) {
        if (assignedToId == null) {
            opp.setAssignedTo(null);
            return;
        }
        AuthAccount user = authAccountRepository.findById(assignedToId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found: " + assignedToId));
        opp.setAssignedTo(user);
    }
}
