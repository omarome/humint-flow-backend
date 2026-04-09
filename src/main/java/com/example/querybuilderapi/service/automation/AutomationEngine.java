package com.example.querybuilderapi.service.automation;

import com.example.querybuilderapi.event.OpportunityStageChangedEvent;
import com.example.querybuilderapi.model.AutomationRule;
import com.example.querybuilderapi.repository.AutomationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AutomationEngine {

    private static final Logger log = LoggerFactory.getLogger(AutomationEngine.class);

    private final AutomationRuleRepository automationRuleRepository;
    private final Map<String, ActionExecutor> actionExecutors;

    public AutomationEngine(AutomationRuleRepository automationRuleRepository, List<ActionExecutor> executors) {
        this.automationRuleRepository = automationRuleRepository;
        this.actionExecutors = executors.stream()
                .collect(Collectors.toMap(ActionExecutor::getActionType, Function.identity()));
    }

    @EventListener
    @Transactional
    public void onOpportunityStageChanged(OpportunityStageChangedEvent event) {
        log.info("AutomationEngine caught OpportunityStageChangedEvent: {} -> {}", event.getOldStage(), event.getNewStage());

        // Fetch active rules that trigger on OPPORTUNITY STAGE_CHANGED
        List<AutomationRule> matchingRules = automationRuleRepository
                .findByTriggerEntityAndTriggerEventAndIsActiveAndIsDeletedFalse("OPPORTUNITY", "STAGE_CHANGED", true);

        for (AutomationRule rule : matchingRules) {
            Map<String, Object> config = rule.getTriggerConfig();
            if (config != null) {
                String targetStage = (String) config.get("value");
                // Rule matches if the new stage equals the trigger configuration stage
                if (event.getNewStage() != null && event.getNewStage().equalsIgnoreCase(targetStage)) {
                    log.info("Executing rule '{}' for Opportunity ID: {}", rule.getName(), event.getOpportunity().getId());
                    executeRuleAction(rule, event.getOpportunity());
                }
            }
        }
    }

    private void executeRuleAction(AutomationRule rule, Object targetEntity) {
        ActionExecutor executor = actionExecutors.get(rule.getActionType());
        if (executor != null) {
            try {
                executor.execute(rule.getActionConfig(), targetEntity);
            } catch (Exception e) {
                log.error("Failed to execute automation action {} for rule {}", rule.getActionType(), rule.getName(), e);
            }
        } else {
            log.warn("No ActionExecutor found for action type: {}", rule.getActionType());
        }
    }
}
