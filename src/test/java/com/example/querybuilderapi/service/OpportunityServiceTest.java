package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.OpportunityRequest;
import com.example.querybuilderapi.dto.OpportunityResponse;
import com.example.querybuilderapi.model.Contact;
import com.example.querybuilderapi.model.Opportunity;
import com.example.querybuilderapi.model.Organization;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private AuditAwareService auditAwareService;

    @InjectMocks
    private OpportunityService opportunityService;

    private Opportunity sampleOpp;
    private Organization sampleOrg;
    private Contact sampleContact;
    private UUID oppId, orgId, contactId;

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

        oppId = UUID.randomUUID();
        sampleOpp = new Opportunity();
        sampleOpp.setId(oppId);
        sampleOpp.setName("Enterprise Deal");
        sampleOpp.setAmount(new BigDecimal("50000.00"));
        sampleOpp.setStage(Opportunity.DealStage.NEGOTIATION.name());
        sampleOpp.setProbability(80);
        sampleOpp.setExpectedCloseDate(LocalDate.of(2026, 6, 30));
        sampleOpp.setOrganization(sampleOrg);
        sampleOpp.setPrimaryContact(sampleContact);
        sampleOpp.setIsDeleted(false);

        when(auditAwareService.getCurrentUserId()).thenReturn(1L);
    }

    // ─── List ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listOpportunities returns paginated results with flattened relationships")
    void listOpportunities_returnsPaginated() {
        Page<Opportunity> page = new PageImpl<>(List.of(sampleOpp));
        when(opportunityRepository.findAllByIsDeletedFalse(any(Pageable.class))).thenReturn(page);

        Page<OpportunityResponse> result = opportunityService.listOpportunities(0, 20, "name", "asc");

        assertEquals(1, result.getTotalElements());
        OpportunityResponse dto = result.getContent().get(0);
        assertEquals("Enterprise Deal", dto.getName());
        assertEquals("Acme Corp", dto.getOrganizationName());
        assertEquals("John Doe", dto.getPrimaryContactName());
    }

    // ─── Get by ID ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getOpportunity returns response with flattened org + contact data")
    void getOpportunity_found() {
        when(opportunityRepository.findByIdAndIsDeletedFalse(oppId)).thenReturn(Optional.of(sampleOpp));

        OpportunityResponse result = opportunityService.getOpportunity(oppId);

        assertNotNull(result);
        assertEquals("Enterprise Deal", result.getName());
        assertEquals(new BigDecimal("50000.00"), result.getAmount());
        assertEquals("NEGOTIATION", result.getStage());
        assertEquals(80, result.getProbability());
        assertEquals(orgId, result.getOrganizationId());
        assertEquals(contactId, result.getPrimaryContactId());
    }

    @Test
    @DisplayName("getOpportunity throws when not found")
    void getOpportunity_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(opportunityRepository.findByIdAndIsDeletedFalse(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> opportunityService.getOpportunity(unknownId));
    }

    // ─── Get by Organization ─────────────────────────────────────────────

    @Test
    @DisplayName("getOpportunitiesByOrganization returns deals for given org")
    void getOpportunitiesByOrganization() {
        when(opportunityRepository.findAllByOrganizationIdAndIsDeletedFalse(orgId))
                .thenReturn(List.of(sampleOpp));

        List<OpportunityResponse> result = opportunityService.getOpportunitiesByOrganization(orgId);

        assertEquals(1, result.size());
        assertEquals(orgId, result.get(0).getOrganizationId());
    }

    // ─── Get by Stage (Kanban) ───────────────────────────────────────────

    @Test
    @DisplayName("getOpportunitiesByStage returns deals for Kanban pipeline view")
    void getOpportunitiesByStage() {
        when(opportunityRepository.findAllByStageAndIsDeletedFalse("NEGOTIATION"))
                .thenReturn(List.of(sampleOpp));

        List<OpportunityResponse> result = opportunityService.getOpportunitiesByStage("NEGOTIATION");

        assertEquals(1, result.size());
        assertEquals("NEGOTIATION", result.get(0).getStage());
    }

    // ─── Create ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOpportunity resolves both FKs and sets audit fields")
    void createOpportunity_withBothFKs() {
        OpportunityRequest request = buildRequest();
        when(organizationRepository.findByIdAndIsDeletedFalse(orgId)).thenReturn(Optional.of(sampleOrg));
        when(contactRepository.findByIdAndIsDeletedFalse(contactId)).thenReturn(Optional.of(sampleContact));
        when(opportunityRepository.save(any(Opportunity.class))).thenReturn(sampleOpp);

        OpportunityResponse result = opportunityService.createOpportunity(request);

        assertNotNull(result);

        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());

        Opportunity saved = captor.getValue();
        assertEquals(1L, saved.getCreatedBy());
        assertEquals(sampleOrg, saved.getOrganization());
        assertEquals(sampleContact, saved.getPrimaryContact());
    }

    @Test
    @DisplayName("createOpportunity without contact sets primaryContact to null")
    void createOpportunity_withoutContact() {
        OpportunityRequest request = buildRequest();
        request.setPrimaryContactId(null);

        when(organizationRepository.findByIdAndIsDeletedFalse(orgId)).thenReturn(Optional.of(sampleOrg));
        when(opportunityRepository.save(any(Opportunity.class))).thenReturn(sampleOpp);

        opportunityService.createOpportunity(request);

        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());
        assertNull(captor.getValue().getPrimaryContact());
    }

    @Test
    @DisplayName("createOpportunity throws when Organization not found")
    void createOpportunity_invalidOrg() {
        UUID fakeOrgId = UUID.randomUUID();
        OpportunityRequest request = buildRequest();
        request.setOrganizationId(fakeOrgId);

        when(organizationRepository.findByIdAndIsDeletedFalse(fakeOrgId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> opportunityService.createOpportunity(request));
        verify(opportunityRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOpportunity throws when Contact not found")
    void createOpportunity_invalidContact() {
        UUID fakeContactId = UUID.randomUUID();
        OpportunityRequest request = buildRequest();
        request.setPrimaryContactId(fakeContactId);

        when(organizationRepository.findByIdAndIsDeletedFalse(orgId)).thenReturn(Optional.of(sampleOrg));
        when(contactRepository.findByIdAndIsDeletedFalse(fakeContactId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> opportunityService.createOpportunity(request));
        verify(opportunityRepository, never()).save(any());
    }

    // ─── Patch ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("patchOpportunity only updates non-null fields (e.g. stage change for Kanban drag)")
    void patchOpportunity_stageChange() {
        when(opportunityRepository.findByIdAndIsDeletedFalse(oppId)).thenReturn(Optional.of(sampleOpp));
        when(opportunityRepository.save(any(Opportunity.class))).thenReturn(sampleOpp);

        OpportunityRequest patch = new OpportunityRequest();
        patch.setStage("CLOSED_WON");

        opportunityService.patchOpportunity(oppId, patch);

        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());

        Opportunity patched = captor.getValue();
        assertEquals("CLOSED_WON", patched.getStage());
        // Untouched fields should remain unchanged
        assertEquals("Enterprise Deal", patched.getName());
        assertEquals(new BigDecimal("50000.00"), patched.getAmount());
    }

    // ─── Delete (Soft) ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOpportunity sets isDeleted=true (soft delete)")
    void deleteOpportunity_softDeletes() {
        when(opportunityRepository.findByIdAndIsDeletedFalse(oppId)).thenReturn(Optional.of(sampleOpp));
        when(opportunityRepository.save(any(Opportunity.class))).thenReturn(sampleOpp);

        opportunityService.deleteOpportunity(oppId);

        ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsDeleted());
    }

    // ─── Search ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchOpportunities delegates to repository and returns results")
    void searchOpportunities_returnsResults() {
        Page<Opportunity> page = new PageImpl<>(List.of(sampleOpp));
        when(opportunityRepository.searchByName(eq("Enterprise"), any(Pageable.class))).thenReturn(page);

        Page<OpportunityResponse> result = opportunityService.searchOpportunities("Enterprise", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Enterprise Deal", result.getContent().get(0).getName());
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private OpportunityRequest buildRequest() {
        OpportunityRequest req = new OpportunityRequest();
        req.setName("Enterprise Deal");
        req.setAmount(new BigDecimal("50000.00"));
        req.setStage("NEGOTIATION");
        req.setProbability(80);
        req.setExpectedCloseDate(LocalDate.of(2026, 6, 30));
        req.setOrganizationId(orgId);
        req.setPrimaryContactId(contactId);
        return req;
    }
}
