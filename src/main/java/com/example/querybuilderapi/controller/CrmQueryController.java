package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.dto.CrmQueryRequest;
import com.example.querybuilderapi.model.Variable;
import com.example.querybuilderapi.repository.VariableRepository;
import com.example.querybuilderapi.service.CrmQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Universal CRM query endpoint for the Segmentation Engine (Phase 6).
 *
 * POST /api/query              — execute a filter query on any CRM entity
 * GET  /api/query/fields       — list all entity types with their field metadata
 * GET  /api/query/fields/{entityType} — list fields for a specific entity type
 */
@RestController
@RequestMapping("/api/query")
public class CrmQueryController {

    private final CrmQueryService  crmQueryService;
    private final VariableRepository variableRepository;

    public CrmQueryController(CrmQueryService crmQueryService,
                              VariableRepository variableRepository) {
        this.crmQueryService   = crmQueryService;
        this.variableRepository = variableRepository;
    }

    /**
     * Execute a filter query.
     *
     * Example body:
     * {
     *   "entityType": "CONTACT",
     *   "combinator": "and",
     *   "rules": [
     *     { "field": "lifecycleStage", "operator": "=", "value": "SQL" }
     *   ],
     *   "page": 0,
     *   "size": 20
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> query(@Valid @RequestBody CrmQueryRequest req) {
        Map<String, Object> result = crmQueryService.query(req);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns all CRM entity types grouped with their field metadata.
     * Used by the frontend to build the entity type selector + field picker.
     */
    @GetMapping("/fields")
    public ResponseEntity<Map<String, List<Variable>>> getAllFields() {
        Map<String, List<Variable>> fields = Map.of(
                "CONTACT",      variableRepository.findByEntityTypeOrderByOffsetAsc("CONTACT"),
                "ORGANIZATION", variableRepository.findByEntityTypeOrderByOffsetAsc("ORGANIZATION"),
                "OPPORTUNITY",  variableRepository.findByEntityTypeOrderByOffsetAsc("OPPORTUNITY"),
                "ACTIVITY",     variableRepository.findByEntityTypeOrderByOffsetAsc("ACTIVITY"),
                "TEAM_MEMBER",  variableRepository.findByEntityTypeOrderByOffsetAsc("TEAM_MEMBER")
        );
        return ResponseEntity.ok(fields);
    }

    /**
     * Returns field metadata for a single entity type.
     * Used by the frontend field picker when the user changes the entity type.
     */
    @GetMapping("/fields/{entityType}")
    public ResponseEntity<List<Variable>> getFieldsForEntity(@PathVariable String entityType) {
        List<Variable> fields = variableRepository.findByEntityTypeOrderByOffsetAsc(entityType.toUpperCase());
        if (fields.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fields);
    }
}
