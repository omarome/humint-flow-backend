package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.ContactResponse;
import com.example.querybuilderapi.dto.OpportunityResponse;
import com.example.querybuilderapi.dto.OrganizationResponse;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * GET /api/search?q=acme&limit=5
 *
 * Returns up to {@code limit} results per entity type for a global search bar.
 * Each entity searches across its primary text fields using LIKE queries that
 * are already indexed in the repository layer.
 *
 * Response shape:
 * {
 *   "contacts":       [ ContactResponse, … ],
 *   "organizations":  [ OrganizationResponse, … ],
 *   "opportunities":  [ OpportunityResponse, … ]
 * }
 */
@RestController
@RequestMapping("/api/search")
public class GlobalSearchController {

    private final ContactRepository      contactRepository;
    private final OrganizationRepository organizationRepository;
    private final OpportunityRepository  opportunityRepository;

    public GlobalSearchController(ContactRepository contactRepository,
                                  OrganizationRepository organizationRepository,
                                  OpportunityRepository opportunityRepository) {
        this.contactRepository      = contactRepository;
        this.organizationRepository = organizationRepository;
        this.opportunityRepository  = opportunityRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<?>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "contacts", List.of(),
                    "organizations", List.of(),
                    "opportunities", List.of()
            ));
        }

        String term = q.trim();
        PageRequest page = PageRequest.of(0, limit);

        List<ContactResponse> contacts = contactRepository
                .searchByNameOrEmail(term, page)
                .map(ContactResponse::fromEntity)
                .getContent();

        List<OrganizationResponse> organizations = organizationRepository
                .searchByName(term, page)
                .map(OrganizationResponse::fromEntity)
                .getContent();

        List<OpportunityResponse> opportunities = opportunityRepository
                .searchByName(term, page)
                .map(OpportunityResponse::fromEntity)
                .getContent();

        return ResponseEntity.ok(Map.of(
                "contacts",      contacts,
                "organizations", organizations,
                "opportunities", opportunities
        ));
    }
}
