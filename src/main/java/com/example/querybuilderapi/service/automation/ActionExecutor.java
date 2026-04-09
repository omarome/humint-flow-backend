package com.example.querybuilderapi.service.automation;

import java.util.Map;

/**
 * Strategy interface for executing automation actions.
 */
public interface ActionExecutor {

    /**
     * Executes the action based on the provided configuration payload and trigger Entity.
     *
     * @param actionConfig The JSON definition of what the action should do (e.g., {"title": "Call customer"})
     * @param targetEntity The entity that triggered the automation (e.g., an Opportunity)
     */
    void execute(Map<String, Object> actionConfig, Object targetEntity);

    /**
     * Identifies the action type this executor handles (e.g., "CREATE_TASK").
     */
    String getActionType();
}
