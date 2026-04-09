package com.example.querybuilderapi.service;

import com.example.querybuilderapi.dto.ActivityRequest;
import com.example.querybuilderapi.dto.ActivityResponse;
import com.example.querybuilderapi.model.Activity;
import com.example.querybuilderapi.model.EntityType;
import com.example.querybuilderapi.repository.ActivityRepository;
import com.example.querybuilderapi.repository.ContactRepository;
import com.example.querybuilderapi.repository.OpportunityRepository;
import com.example.querybuilderapi.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service layer for Activity (Timeline) CRUD.
 * Handles polymorphic entity validation, audit fields, and soft deletes.
 */
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final AuditAwareService auditAwareService;
    private final OrganizationRepository organizationRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;

    public ActivityService(ActivityRepository activityRepository,
                           AuditAwareService auditAwareService,
                           OrganizationRepository organizationRepository,
                           ContactRepository contactRepository,
                           OpportunityRepository opportunityRepository) {
        this.activityRepository = activityRepository;
        this.auditAwareService = auditAwareService;
        this.organizationRepository = organizationRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
    }

    /**
     * Returns a paginated timeline of activities for a given entity.
     */
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getTimeline(EntityType entityType, UUID entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activityRepository
                .findByEntityTypeAndEntityIdAndIsDeletedFalseOrderByCreatedAtDesc(entityType, entityId, pageable)
                .map(ActivityResponse::fromEntity);
    }

    /**
     * Returns all non-deleted activities across the workspace, ordered by creation date desc.
     * Used by the calendar view which needs a global activity feed.
     */
    @Transactional(readOnly = true)
    public Page<ActivityResponse> listAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        return activityRepository.findAllByIsDeletedFalse(pageable)
                .map(ActivityResponse::fromEntity);
    }

    /**
     * Creates a new activity. Validates that the target entity exists.
     */
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        validateEntityExists(request.getEntityType(), request.getEntityId());

        Activity activity = new Activity();
        mapRequestToEntity(request, activity);

        Long currentUserId = auditAwareService.getCurrentUserId();
        activity.setCreatedBy(currentUserId);
        activity.setUpdatedBy(currentUserId);

        Activity saved = activityRepository.save(activity);
        return ActivityResponse.fromEntity(saved);
    }

    /**
     * Partially updates an activity.
     */
    @Transactional
    public ActivityResponse updateActivity(UUID id, ActivityRequest request) {
        Activity activity = activityRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        if (request.getSubject() != null) activity.setSubject(request.getSubject());
        if (request.getBody() != null) activity.setBody(request.getBody());
        if (request.getCallDuration() != null) activity.setCallDuration(request.getCallDuration());
        if (request.getEmailTo() != null) activity.setEmailTo(request.getEmailTo());
        if (request.getMeetingDate() != null) activity.setMeetingDate(request.getMeetingDate());
        if (request.getTaskDueDate() != null) activity.setTaskDueDate(request.getTaskDueDate());
        if (request.getTaskCompleted() != null) activity.setTaskCompleted(request.getTaskCompleted());

        activity.setUpdatedBy(auditAwareService.getCurrentUserId());

        Activity saved = activityRepository.save(activity);
        return ActivityResponse.fromEntity(saved);
    }

    /**
     * Soft deletes an activity.
     */
    @Transactional
    public void deleteActivity(UUID id) {
        Activity activity = activityRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + id));

        activity.softDelete();
        activity.setUpdatedBy(auditAwareService.getCurrentUserId());
        activityRepository.save(activity);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private void mapRequestToEntity(ActivityRequest request, Activity activity) {
        activity.setActivityType(request.getActivityType());
        activity.setEntityType(request.getEntityType());
        activity.setEntityId(request.getEntityId());
        activity.setSubject(request.getSubject());
        activity.setBody(request.getBody());
        activity.setCallDuration(request.getCallDuration());
        activity.setEmailTo(request.getEmailTo());
        activity.setMeetingDate(request.getMeetingDate());
        activity.setTaskDueDate(request.getTaskDueDate());
        activity.setTaskCompleted(request.getTaskCompleted());
    }

    /**
     * Validates that the referenced CRM entity actually exists (not soft-deleted).
     */
    private void validateEntityExists(EntityType entityType, UUID entityId) {
        boolean exists = switch (entityType) {
            case ORGANIZATION -> organizationRepository.findByIdAndIsDeletedFalse(entityId).isPresent();
            case CONTACT -> contactRepository.findByIdAndIsDeletedFalse(entityId).isPresent();
            case OPPORTUNITY -> opportunityRepository.findByIdAndIsDeletedFalse(entityId).isPresent();
        };

        if (!exists) {
            throw new IllegalArgumentException(
                    entityType + " not found with ID: " + entityId);
        }
    }
}
