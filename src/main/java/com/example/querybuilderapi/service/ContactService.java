package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.ContactRequest;
import com.example.querybuilderapi.dto.ContactResponse;
import com.example.querybuilderapi.model.Contact;
import com.example.querybuilderapi.model.Organization;
import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import com.example.querybuilderapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for Contact CRUD operations.
 * Handles Organization relationship resolution, audit field population,
 * soft deletes, and DTO mapping.
 */
@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditAwareService auditAwareService;

    public ContactService(ContactRepository contactRepository,
                          OrganizationRepository organizationRepository,
                          UserRepository userRepository,
                          AuditAwareService auditAwareService) {
        this.contactRepository = contactRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.auditAwareService = auditAwareService;
    }

    /**
     * Returns a paginated list of non-deleted contacts.
     * Organization data is eagerly fetched via @EntityGraph in the repository.
     */
    @Transactional(readOnly = true)
    public Page<ContactResponse> listContacts(int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                            sortBy != null ? sortBy : "lastName");
        Pageable pageable = PageRequest.of(page, size, sort);
        return contactRepository.findAllByIsDeletedFalse(pageable)
                .map(ContactResponse::fromEntity);
    }

    /**
     * Finds a single non-deleted contact by ID.
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public ContactResponse getContact(UUID id) {
        Contact contact = contactRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));
        return ContactResponse.fromEntity(contact);
    }

    /**
     * Finds all non-deleted contacts belonging to a specific organization.
     */
    @Transactional(readOnly = true)
    public List<ContactResponse> getContactsByOrganization(UUID organizationId) {
        return contactRepository.findAllByOrganizationIdAndIsDeletedFalse(organizationId)
                .stream()
                .map(ContactResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new contact from the given request DTO.
     * Resolves and validates the Organization FK if provided.
     *
     * @throws IllegalArgumentException if organizationId is provided but not found
     */
    @Transactional
    public ContactResponse createContact(ContactRequest request) {
        Contact contact = new Contact();
        mapRequestToEntity(request, contact);

        // Resolve relationships
        resolveOrganization(request.getOrganizationId(), contact);
        resolveAssignee(request.getAssignedToId(), contact);

        Long currentUserId = auditAwareService.getCurrentUserId();
        contact.setCreatedBy(currentUserId);
        contact.setUpdatedBy(currentUserId);

        Contact saved = contactRepository.save(contact);
        return ContactResponse.fromEntity(saved);
    }

    /**
     * Fully updates an existing contact.
     *
     * @throws IllegalArgumentException if contact or referenced org not found
     */
    @Transactional
    public ContactResponse updateContact(UUID id, ContactRequest request) {
        Contact contact = contactRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));

        mapRequestToEntity(request, contact);
        resolveOrganization(request.getOrganizationId(), contact);
        resolveAssignee(request.getAssignedToId(), contact);
        contact.setUpdatedBy(auditAwareService.getCurrentUserId());

        Contact saved = contactRepository.save(contact);
        return ContactResponse.fromEntity(saved);
    }

    /**
     * Partially updates a contact (only non-null fields from request).
     *
     * @throws IllegalArgumentException if contact or referenced org not found
     */
    @Transactional
    public ContactResponse patchContact(UUID id, ContactRequest request) {
        Contact contact = contactRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));

        if (request.getFirstName() != null) contact.setFirstName(request.getFirstName());
        if (request.getLastName() != null) contact.setLastName(request.getLastName());
        if (request.getEmail() != null) contact.setEmail(request.getEmail());
        if (request.getPhone() != null) contact.setPhone(request.getPhone());
        if (request.getJobTitle() != null) contact.setJobTitle(request.getJobTitle());
        if (request.getDepartment() != null) contact.setDepartment(request.getDepartment());
        if (request.getLeadSource() != null) contact.setLeadSource(request.getLeadSource());
        if (request.getLeadScore() != null) contact.setLeadScore(request.getLeadScore());
        if (request.getLifecycleStage() != null) contact.setLifecycleStage(request.getLifecycleStage());
        if (request.getOrganizationId() != null) resolveOrganization(request.getOrganizationId(), contact);
        if (request.getAssignedToId() != null) resolveAssignee(request.getAssignedToId(), contact);

        contact.setUpdatedBy(auditAwareService.getCurrentUserId());

        Contact saved = contactRepository.save(contact);
        return ContactResponse.fromEntity(saved);
    }

    /**
     * Soft deletes a contact (sets isDeleted = true).
     *
     * @throws IllegalArgumentException if not found
     */
    @Transactional
    public void deleteContact(UUID id) {
        Contact contact = contactRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));

        contact.softDelete();
        contact.setUpdatedBy(auditAwareService.getCurrentUserId());
        contactRepository.save(contact);
    }

    /**
     * Search contacts by name or email.
     */
    @Transactional(readOnly = true)
    public Page<ContactResponse> searchContacts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        return contactRepository.searchByNameOrEmail(query, pageable)
                .map(ContactResponse::fromEntity);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    /**
     * Maps all scalar fields from the request DTO to the entity.
     * Does NOT handle the Organization relationship — use resolveOrganization().
     */
    private void mapRequestToEntity(ContactRequest request, Contact contact) {
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setJobTitle(request.getJobTitle());
        contact.setDepartment(request.getDepartment());
        contact.setLeadSource(request.getLeadSource());
        contact.setLeadScore(request.getLeadScore() != null ? request.getLeadScore() : 0);
        contact.setLifecycleStage(request.getLifecycleStage() != null
                ? request.getLifecycleStage()
                : Contact.LifecycleStage.LEAD.name());
    }

    /**
     * Resolves the Organization FK from the request.
     * - If organizationId is null, clears the relationship.
     * - If organizationId is provided, validates it exists and is not deleted.
     *
     * @throws IllegalArgumentException if org not found or deleted
     */
    private void resolveOrganization(UUID organizationId, Contact contact) {
        if (organizationId == null) {
            contact.setOrganization(null);
            return;
        }

        Organization org = organizationRepository.findByIdAndIsDeletedFalse(organizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Organization not found: " + organizationId));
        contact.setOrganization(org);
    }

    /**
     * Resolves the assigned user.
     */
    private void resolveAssignee(Long assignedToId, Contact contact) {
        if (assignedToId == null) {
            contact.setAssignedTo(null);
            return;
        }
        User user = userRepository.findById(assignedToId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + assignedToId));
        contact.setAssignedTo(user);
    }
}
