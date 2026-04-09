package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.ContactRequest;
import com.example.querybuilderapi.dto.ContactResponse;
import com.example.querybuilderapi.model.Contact;
import com.example.querybuilderapi.model.Organization;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuditAwareService auditAwareService;

    @InjectMocks
    private ContactService contactService;

    private Contact sampleContact;
    private Organization sampleOrg;
    private UUID contactId;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        orgId = UUID.randomUUID();
        sampleOrg = new Organization();
        sampleOrg.setId(orgId);
        sampleOrg.setName("Acme Corp");

        contactId = UUID.randomUUID();
        sampleContact = new Contact();
        sampleContact.setId(contactId);
        sampleContact.setFirstName("John");
        sampleContact.setLastName("Doe");
        sampleContact.setEmail("john.doe@acme.com");
        sampleContact.setPhone("+1-555-0101");
        sampleContact.setJobTitle("VP of Sales");
        sampleContact.setDepartment("Sales");
        sampleContact.setLeadSource("Website");
        sampleContact.setLeadScore(85);
        sampleContact.setLifecycleStage(Contact.LifecycleStage.SQL.name());
        sampleContact.setOrganization(sampleOrg);
        sampleContact.setIsDeleted(false);

        when(auditAwareService.getCurrentUserId()).thenReturn(1L);
    }

    // ─── List ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listContacts returns paginated results with organization data")
    void listContacts_returnsPaginated() {
        Page<Contact> page = new PageImpl<>(List.of(sampleContact));
        when(contactRepository.findAllByIsDeletedFalse(any(Pageable.class))).thenReturn(page);

        Page<ContactResponse> result = contactService.listContacts(0, 20, "lastName", "asc");

        assertEquals(1, result.getTotalElements());
        ContactResponse dto = result.getContent().get(0);
        assertEquals("John Doe", dto.getFullName());
        assertEquals("Acme Corp", dto.getOrganizationName());
        assertEquals(orgId, dto.getOrganizationId());
    }

    // ─── Get by ID ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getContact returns response with flattened Organization data")
    void getContact_found() {
        when(contactRepository.findByIdAndIsDeletedFalse(contactId)).thenReturn(Optional.of(sampleContact));

        ContactResponse result = contactService.getContact(contactId);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("John Doe", result.getFullName());
        assertEquals("john.doe@acme.com", result.getEmail());
        assertEquals(orgId, result.getOrganizationId());
        assertEquals("Acme Corp", result.getOrganizationName());
        assertEquals("SQL", result.getLifecycleStage());
    }

    @Test
    @DisplayName("getContact throws when not found")
    void getContact_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(contactRepository.findByIdAndIsDeletedFalse(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> contactService.getContact(unknownId));
    }

    // ─── Get by Organization ─────────────────────────────────────────────

    @Test
    @DisplayName("getContactsByOrganization returns contacts for a given org")
    void getContactsByOrganization_returnsContacts() {
        when(contactRepository.findAllByOrganizationIdAndIsDeletedFalse(orgId))
                .thenReturn(List.of(sampleContact));

        List<ContactResponse> result = contactService.getContactsByOrganization(orgId);

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
    }

    // ─── Create ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createContact saves with valid Organization FK and audit fields")
    void createContact_withOrganization() {
        ContactRequest request = buildRequest();
        request.setOrganizationId(orgId);

        when(organizationRepository.findByIdAndIsDeletedFalse(orgId)).thenReturn(Optional.of(sampleOrg));
        when(contactRepository.save(any(Contact.class))).thenReturn(sampleContact);

        ContactResponse result = contactService.createContact(request);

        assertNotNull(result);

        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());

        Contact saved = captor.getValue();
        assertEquals(1L, saved.getCreatedBy());
        assertEquals(1L, saved.getUpdatedBy());
        assertEquals(sampleOrg, saved.getOrganization());
    }

    @Test
    @DisplayName("createContact without organizationId sets org to null")
    void createContact_withoutOrganization() {
        ContactRequest request = buildRequest();
        request.setOrganizationId(null);

        when(contactRepository.save(any(Contact.class))).thenReturn(sampleContact);

        contactService.createContact(request);

        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        assertNull(captor.getValue().getOrganization());
    }

    @Test
    @DisplayName("createContact throws when referenced Organization not found")
    void createContact_invalidOrganization() {
        UUID fakeOrgId = UUID.randomUUID();
        ContactRequest request = buildRequest();
        request.setOrganizationId(fakeOrgId);

        when(organizationRepository.findByIdAndIsDeletedFalse(fakeOrgId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> contactService.createContact(request));
        verify(contactRepository, never()).save(any());
    }

    // ─── Patch ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("patchContact only updates non-null fields")
    void patchContact_partialUpdate() {
        when(contactRepository.findByIdAndIsDeletedFalse(contactId)).thenReturn(Optional.of(sampleContact));
        when(contactRepository.save(any(Contact.class))).thenReturn(sampleContact);

        ContactRequest patch = new ContactRequest();
        patch.setJobTitle("CTO");

        contactService.patchContact(contactId, patch);

        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());

        Contact patched = captor.getValue();
        assertEquals("CTO", patched.getJobTitle());
        // Untouched fields should remain unchanged
        assertEquals("John", patched.getFirstName());
        assertEquals("john.doe@acme.com", patched.getEmail());
    }

    // ─── Delete (Soft) ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteContact sets isDeleted=true (soft delete)")
    void deleteContact_softDeletes() {
        when(contactRepository.findByIdAndIsDeletedFalse(contactId)).thenReturn(Optional.of(sampleContact));
        when(contactRepository.save(any(Contact.class))).thenReturn(sampleContact);

        contactService.deleteContact(contactId);

        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsDeleted());
    }

    // ─── Search ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchContacts delegates to repository and returns results")
    void searchContacts_returnsResults() {
        Page<Contact> page = new PageImpl<>(List.of(sampleContact));
        when(contactRepository.searchByNameOrEmail(eq("John"), any(Pageable.class))).thenReturn(page);

        Page<ContactResponse> result = contactService.searchContacts("John", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("John Doe", result.getContent().get(0).getFullName());
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private ContactRequest buildRequest() {
        ContactRequest req = new ContactRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john.doe@acme.com");
        req.setPhone("+1-555-0101");
        req.setJobTitle("VP of Sales");
        req.setDepartment("Sales");
        req.setLeadSource("Website");
        req.setLeadScore(85);
        req.setLifecycleStage("SQL");
        return req;
    }
}
