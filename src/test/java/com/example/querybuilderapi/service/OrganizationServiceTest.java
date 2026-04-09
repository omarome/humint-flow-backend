package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.OrganizationRequest;
import com.example.querybuilderapi.dto.OrganizationResponse;
import com.example.querybuilderapi.model.Organization;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuditAwareService auditAwareService;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization sampleOrg;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleId = UUID.randomUUID();
        sampleOrg = new Organization();
        sampleOrg.setId(sampleId);
        sampleOrg.setName("Acme Corp");
        sampleOrg.setIndustry("Technology");
        sampleOrg.setWebsite("https://acme.com");
        sampleOrg.setPhone("+1-555-0100");
        sampleOrg.setCity("San Francisco");
        sampleOrg.setCountry("USA");
        sampleOrg.setEmployeeCount(500);
        sampleOrg.setAnnualRevenue(new BigDecimal("10000000.00"));
        sampleOrg.setIsDeleted(false);

        when(auditAwareService.getCurrentUserId()).thenReturn(1L);
    }

    // ─── List ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listOrganizations returns paginated non-deleted results")
    void listOrganizations_returnsPaginated() {
        Page<Organization> page = new PageImpl<>(List.of(sampleOrg));
        when(organizationRepository.findAllByIsDeletedFalse(any(Pageable.class))).thenReturn(page);

        Page<OrganizationResponse> result = organizationService.listOrganizations(0, 20, "name", "asc");

        assertEquals(1, result.getTotalElements());
        assertEquals("Acme Corp", result.getContent().get(0).getName());
        verify(organizationRepository).findAllByIsDeletedFalse(any(Pageable.class));
    }

    // ─── Get by ID ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrganization returns response when found")
    void getOrganization_found() {
        when(organizationRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(sampleOrg));

        OrganizationResponse result = organizationService.getOrganization(sampleId);

        assertNotNull(result);
        assertEquals(sampleId, result.getId());
        assertEquals("Acme Corp", result.getName());
        assertEquals("Technology", result.getIndustry());
    }

    @Test
    @DisplayName("getOrganization throws when not found")
    void getOrganization_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(organizationRepository.findByIdAndIsDeletedFalse(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> organizationService.getOrganization(unknownId));
    }

    // ─── Create ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOrganization saves entity with audit fields and returns response")
    void createOrganization_savesWithAudit() {
        OrganizationRequest request = buildRequest();
        when(organizationRepository.save(any(Organization.class))).thenReturn(sampleOrg);

        OrganizationResponse result = organizationService.createOrganization(request);

        assertNotNull(result);
        assertEquals("Acme Corp", result.getName());

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(captor.capture());

        Organization saved = captor.getValue();
        assertEquals(1L, saved.getCreatedBy());
        assertEquals(1L, saved.getUpdatedBy());
    }

    // ─── Update ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrganization fully overwrites fields")
    void updateOrganization_overwritesAll() {
        when(organizationRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(sampleOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(sampleOrg);

        OrganizationRequest request = buildRequest();
        request.setName("Updated Corp");

        OrganizationResponse result = organizationService.updateOrganization(sampleId, request);

        assertNotNull(result);
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    @DisplayName("updateOrganization throws when not found")
    void updateOrganization_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(organizationRepository.findByIdAndIsDeletedFalse(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> organizationService.updateOrganization(unknownId, buildRequest()));
    }

    // ─── Patch ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("patchOrganization only updates non-null fields")
    void patchOrganization_partialUpdate() {
        when(organizationRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(sampleOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(sampleOrg);

        OrganizationRequest patch = new OrganizationRequest();
        patch.setCity("New York");

        organizationService.patchOrganization(sampleId, patch);

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(captor.capture());

        Organization patched = captor.getValue();
        assertEquals("New York", patched.getCity());
        // Name should remain unchanged since we didn't patch it
        assertEquals("Acme Corp", patched.getName());
    }

    // ─── Delete (Soft) ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOrganization sets isDeleted=true (soft delete)")
    void deleteOrganization_softDeletes() {
        when(organizationRepository.findByIdAndIsDeletedFalse(sampleId)).thenReturn(Optional.of(sampleOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(sampleOrg);

        organizationService.deleteOrganization(sampleId);

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsDeleted());
    }

    @Test
    @DisplayName("deleteOrganization throws when not found")
    void deleteOrganization_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(organizationRepository.findByIdAndIsDeletedFalse(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> organizationService.deleteOrganization(unknownId));
    }

    // ─── Search ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchOrganizations delegates to repository and returns results")
    void searchOrganizations_returnsResults() {
        Page<Organization> page = new PageImpl<>(List.of(sampleOrg));
        when(organizationRepository.searchByName(eq("Acme"), any(Pageable.class))).thenReturn(page);

        Page<OrganizationResponse> result = organizationService.searchOrganizations("Acme", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Acme Corp", result.getContent().get(0).getName());
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private OrganizationRequest buildRequest() {
        OrganizationRequest req = new OrganizationRequest();
        req.setName("Acme Corp");
        req.setIndustry("Technology");
        req.setWebsite("https://acme.com");
        req.setPhone("+1-555-0100");
        req.setCity("San Francisco");
        req.setCountry("USA");
        req.setEmployeeCount(500);
        req.setAnnualRevenue(new BigDecimal("10000000.00"));
        return req;
    }
}
