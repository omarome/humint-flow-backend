package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.SavedView;
import com.example.querybuilderapi.service.SavedViewService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> saveView(@RequestBody Map<String, String> payload) {
        try {
            String name = payload.get("name");
            String queryJson = payload.get("queryJson");
            
            SavedView savedView = savedViewService.saveView(name, queryJson);
            return ResponseEntity.ok(savedView);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * GET /api/saved-views — returns all saved views.
     */
    @GetMapping
    public List<SavedView> getAllSavedViews() {
        return savedViewService.getAllSavedViews();
    }

    /**
     * DELETE /api/saved-views/{id} — deletes a saved view by its ID.
     */
    @DeleteMapping("/{id}")
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
