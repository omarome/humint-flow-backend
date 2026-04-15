package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.model.AutomationRule;
import com.example.querybuilderapi.service.AutomationRuleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/automations")
public class AutomationRuleController {

    private final AutomationRuleService automationRuleService;

    public AutomationRuleController(AutomationRuleService automationRuleService) {
        this.automationRuleService = automationRuleService;
    }

    @GetMapping
    @PreAuthorize("@perms.can('AUTOMATIONS_READ')")
    public ResponseEntity<List<AutomationRule>> getAllRules() {
        return ResponseEntity.ok(automationRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perms.can('AUTOMATIONS_READ')")
    public ResponseEntity<AutomationRule> getRuleById(@PathVariable UUID id) {
        return automationRuleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@perms.can('AUTOMATIONS_WRITE')")
    public ResponseEntity<AutomationRule> createRule(@Valid @RequestBody AutomationRule rule) {
        return ResponseEntity.ok(automationRuleService.createRule(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perms.can('AUTOMATIONS_WRITE')")
    public ResponseEntity<AutomationRule> updateRule(@PathVariable UUID id, @Valid @RequestBody AutomationRule rule) {
        try {
            return ResponseEntity.ok(automationRuleService.updateRule(id, rule));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perms.can('AUTOMATIONS_WRITE')")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        automationRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
