package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.AutomationRule;
import com.example.querybuilderapi.repository.AutomationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AutomationRuleService {

    private final AutomationRuleRepository automationRuleRepository;

    public AutomationRuleService(AutomationRuleRepository automationRuleRepository) {
        this.automationRuleRepository = automationRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<AutomationRule> getAllRules() {
        return automationRuleRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<AutomationRule> getRuleById(UUID id) {
        return automationRuleRepository.findById(id).filter(rule -> !rule.getIsDeleted());
    }

    public AutomationRule createRule(AutomationRule rule) {
        return automationRuleRepository.save(rule);
    }

    public AutomationRule updateRule(UUID id, AutomationRule updatedRule) {
        return automationRuleRepository.findById(id)
                .map(existingRule -> {
                    if (existingRule.getIsDeleted()) return null;
                    existingRule.setName(updatedRule.getName());
                    existingRule.setDescription(updatedRule.getDescription());
                    existingRule.setIsActive(updatedRule.getIsActive());
                    existingRule.setTriggerEntity(updatedRule.getTriggerEntity());
                    existingRule.setTriggerEvent(updatedRule.getTriggerEvent());
                    existingRule.setTriggerConfig(updatedRule.getTriggerConfig());
                    existingRule.setActionType(updatedRule.getActionType());
                    existingRule.setActionConfig(updatedRule.getActionConfig());
                    return automationRuleRepository.save(existingRule);
                })
                .orElseThrow(() -> new RuntimeException("AutomationRule not found with id " + id));
    }

    public void deleteRule(UUID id) {
        automationRuleRepository.findById(id).ifPresent(rule -> {
            rule.setIsDeleted(true);
            automationRuleRepository.save(rule);
        });
    }
}
