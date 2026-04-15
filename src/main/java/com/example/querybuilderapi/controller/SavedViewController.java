package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.SavedView;
import com.example.querybuilderapi.service.SavedViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing saved filter views.
 */
@RestController
@RequestMapping("/api/saved-views")
public class SavedViewController {

    private final SavedViewService savedViewService;

    public SavedViewController(SavedViewService savedViewService) {
        this.savedViewService = savedViewService;
    }

    /**
     * POST /api/saved-views — saves a new filter view.
     */
    @PostMapping
    @PreAuthorize("@perms.can(\'SAVED_VIEWS_WRITE\')")
    public ResponseEntity<?> saveView(@RequestBody Map<String, String> payload) {
        try {
            String name       = payload.get("name");
            String queryJson  = payload.get("queryJson");
            String entityType = payload.get("entityType");

            SavedView savedView = savedViewService.saveView(name, queryJson, entityType);
            return ResponseEntity.ok(savedView);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * GET /api/saved-views?entityType=ORGANIZATION — returns views for an entity type.
     * Omit entityType param to get legacy (null) views.
     */
    @GetMapping
    @PreAuthorize("@perms.can('SAVED_VIEWS_READ')")
    public List<SavedView> getSavedViews(@RequestParam(required = false) String entityType) {
        return savedViewService.getViewsByEntityType(entityType);
    }

    /**
     * DELETE /api/saved-views/{id} — deletes a saved view by its ID.
     * Any authenticated user can delete; service layer enforces that reps can only delete their own views.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.can(\'SAVED_VIEWS_WRITE\')")
    public ResponseEntity<?> deleteView(@PathVariable Long id) {
        try {
            savedViewService.deleteView(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }
}
