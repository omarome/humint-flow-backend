package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.Variable;
import com.example.querybuilderapi.service.VariableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for variable metadata.
 */
@RestController
@RequestMapping("/api/variables")
public class VariableController {

    private final VariableService variableService;

    public VariableController(VariableService variableService) {
        this.variableService = variableService;
    }

    /**
     * GET /api/variables — returns all variables ordered alphabetically by name.
     * Accessible to all authenticated users (needed to build the filter/query UI).
     */
    @GetMapping
    @PreAuthorize("@perms.can('VARIABLES_READ')")
    public List<Variable> getAllVariables() {
        return variableService.getAllVariables();
    }

    /**
     * GET /api/variables/{id} — returns a single variable by id.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@perms.can('VARIABLES_READ')")
    public ResponseEntity<Variable> getVariableById(@PathVariable Long id) {
        Variable variable = variableService.getVariableById(id);
        if (variable == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(variable);
    }
}
