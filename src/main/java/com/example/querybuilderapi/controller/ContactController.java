package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.ContactRequest;
import com.example.querybuilderapi.dto.ContactResponse;
import com.example.querybuilderapi.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Contact CRUD operations.
 *
 * Base path: /api/crm/contacts
 */
@RestController
@RequestMapping("/api/sales/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * GET /api/crm/contacts
     * Returns a paginated list of contacts.
     *
     * @param page    zero-based page index (default 0)
     * @param size    items per page (default 20)
     * @param sortBy  field to sort by (default "lastName")
     * @param sortDir "asc" or "desc" (default "asc")
     * @param search  optional name/email search query
     * @param organizationId optional filter by organization
     */
    @GetMapping
    public ResponseEntity<?> listContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID organizationId) {

        // Filter by organization if specified
        if (organizationId != null) {
            List<ContactResponse> contacts = contactService.getContactsByOrganization(organizationId);
            return ResponseEntity.ok(contacts);
        }

        // Search if query provided
        if (search != null && !search.isBlank()) {
            Page<ContactResponse> result = contactService.searchContacts(search.trim(), page, size);
            return ResponseEntity.ok(result);
        }

        // Default: paginated list
        Page<ContactResponse> result = contactService.listContacts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/crm/contacts/{id}
     * Returns a single contact by UUID, including flattened Organization data.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContact(@PathVariable UUID id) {
        ContactResponse contact = contactService.getContact(id);
        return ResponseEntity.ok(contact);
    }

    /**
     * POST /api/crm/contacts
     * Creates a new contact. If organizationId is provided in the body,
     * the contact will be linked to that organization.
     */
    @PostMapping
    public ResponseEntity<ContactResponse> createContact(
            @Valid @RequestBody ContactRequest request) {
        ContactResponse created = contactService.createContact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/crm/contacts/{id}
     * Fully updates an existing contact.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable UUID id,
            @Valid @RequestBody ContactRequest request) {
        ContactResponse updated = contactService.updateContact(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/crm/contacts/{id}
     * Partially updates a contact (only non-null fields).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ContactResponse> patchContact(
            @PathVariable UUID id,
            @RequestBody ContactRequest request) {
        ContactResponse patched = contactService.patchContact(id, request);
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /api/crm/contacts/{id}
     * Soft deletes a contact.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
