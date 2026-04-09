package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.ActivityRequest;
import com.example.querybuilderapi.dto.ActivityResponse;
import com.example.querybuilderapi.model.EntityType;
import com.example.querybuilderapi.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Activity (Timeline) CRUD operations.
 *
 * Base path: /api/sales/activities
 */
@RestController
@RequestMapping("/api/sales/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * GET /api/sales/activities?entityType=CONTACT&entityId={uuid}  → entity timeline
     * GET /api/sales/activities?size=500                             → all activities (calendar view)
     *
     * Both entityType and entityId are now optional. When omitted, returns all non-deleted
     * activities across the workspace (used by CalendarView). When provided together,
     * returns the timeline for that specific entity.
     */
    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> getActivities(
            @RequestParam(value = "entityType", required = false) EntityType entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (entityType != null && entityId != null) {
            return ResponseEntity.ok(activityService.getTimeline(entityType, entityId, page, size));
        }
        return ResponseEntity.ok(activityService.listAll(page, size));
    }

    /**
     * POST /api/sales/activities
     * Creates a new activity.
     */
    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityRequest request) {
        ActivityResponse created = activityService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/sales/activities/{id}
     * Partially updates an existing activity.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable UUID id,
            @RequestBody ActivityRequest request) {
        ActivityResponse updated = activityService.updateActivity(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/sales/activities/{id}
     * Soft deletes an activity.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}
