package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.*;
import com.example.querybuilderapi.model.*;
import com.example.querybuilderapi.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Universal query service — accepts a CrmQueryRequest and returns
 * a paginated list of the appropriate DTO for the requested entity type.
 *
 * Delegates Specification building to {@link CrmSpecificationBuilder}
 * and applies an extra `isDeleted = false` guard for all CRM entities.
 */
@Service
@Transactional(readOnly = true)
public class CrmQueryService {

    private final CrmSpecificationBuilder specBuilder;
    private final ContactRepository       contactRepository;
    private final OrganizationRepository  organizationRepository;
    private final OpportunityRepository   opportunityRepository;
    private final ActivityRepository      activityRepository;
    private final AuthAccountRepository   authAccountRepository;

    public CrmQueryService(CrmSpecificationBuilder specBuilder,
                           ContactRepository contactRepository,
                           OrganizationRepository organizationRepository,
                           OpportunityRepository opportunityRepository,
                           ActivityRepository activityRepository,
                           AuthAccountRepository authAccountRepository) {
        this.specBuilder           = specBuilder;
        this.contactRepository     = contactRepository;
        this.organizationRepository = organizationRepository;
        this.opportunityRepository  = opportunityRepository;
        this.activityRepository     = activityRepository;
        this.authAccountRepository  = authAccountRepository;
    }

    /**
     * Executes the query and returns a raw {@code Page<?>} of DTOs.
     * The controller wraps this in a typed response.
     */
    public Map<String, Object> query(CrmQueryRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());
        String entity = req.getEntityType().toUpperCase();

        return switch (entity) {
            case "CONTACT"      -> runContactQuery(req, pageable);
            case "ORGANIZATION" -> runOrganizationQuery(req, pageable);
            case "OPPORTUNITY"  -> runOpportunityQuery(req, pageable);
            case "ACTIVITY"     -> runActivityQuery(req, pageable);
            case "TEAM_MEMBER"  -> runTeamMemberQuery(req, pageable);
            default -> throw new IllegalArgumentException("Unknown entityType: " + req.getEntityType());
        };
    }

    // ── CONTACT ──────────────────────────────────────────────────────────

    private Map<String, Object> runContactQuery(CrmQueryRequest req, Pageable pageable) {
        Specification<Contact> notDeleted = (r, q, cb) -> cb.isFalse(r.get("isDeleted"));
        Specification<Contact> userFilter = specBuilder.build(req.getCombinator(), req.getRules());
        Page<ContactResponse> page = contactRepository
                .findAll(notDeleted.and(userFilter), pageable)
                .map(ContactResponse::fromEntity);
        return toMap(page);
    }

    // ── ORGANIZATION ─────────────────────────────────────────────────────

    private Map<String, Object> runOrganizationQuery(CrmQueryRequest req, Pageable pageable) {
        Specification<Organization> notDeleted = (r, q, cb) -> cb.isFalse(r.get("isDeleted"));
        Specification<Organization> userFilter = specBuilder.build(req.getCombinator(), req.getRules());
        Page<OrganizationResponse> page = organizationRepository
                .findAll(notDeleted.and(userFilter), pageable)
                .map(OrganizationResponse::fromEntity);
        return toMap(page);
    }

    // ── OPPORTUNITY ──────────────────────────────────────────────────────

    private Map<String, Object> runOpportunityQuery(CrmQueryRequest req, Pageable pageable) {
        Specification<Opportunity> notDeleted = (r, q, cb) -> cb.isFalse(r.get("isDeleted"));
        Specification<Opportunity> userFilter = specBuilder.build(req.getCombinator(), req.getRules());
        Page<OpportunityResponse> page = opportunityRepository
                .findAll(notDeleted.and(userFilter), pageable)
                .map(OpportunityResponse::fromEntity);
        return toMap(page);
    }

    // ── ACTIVITY ─────────────────────────────────────────────────────────

    private Map<String, Object> runActivityQuery(CrmQueryRequest req, Pageable pageable) {
        Specification<Activity> notDeleted = (r, q, cb) -> cb.isFalse(r.get("isDeleted"));
        Specification<Activity> userFilter = specBuilder.build(req.getCombinator(), req.getRules());
        Page<ActivityResponse> page = activityRepository
                .findAll(notDeleted.and(userFilter), pageable)
                .map(ActivityResponse::fromEntity);
        return toMap(page);
    }

    // ── TEAM_MEMBER ──────────────────────────────────────────────────────

    private Map<String, Object> runTeamMemberQuery(CrmQueryRequest req, Pageable pageable) {
        Specification<AuthAccount> activeOnly = (r, q, cb) -> cb.isTrue(r.get("isActive"));
        Specification<AuthAccount> userFilter = specBuilder.build(req.getCombinator(), req.getRules());
        Page<TeamMemberResponse> page = authAccountRepository
                .findAll(activeOnly.and(userFilter), pageable)
                .map(TeamMemberResponse::fromEntity);
        return toMap(page);
    }

    // ── Utilities ────────────────────────────────────────────────────────

    private Map<String, Object> toMap(Page<?> page) {
        return Map.of(
                "content",       page.getContent(),
                "totalElements", page.getTotalElements(),
                "totalPages",    page.getTotalPages(),
                "page",          page.getNumber(),
                "size",          page.getSize()
        );
    }
}
