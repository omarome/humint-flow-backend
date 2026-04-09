package com.example.querybuilderapi.event;

import com.example.querybuilderapi.model.Opportunity;
import org.springframework.context.ApplicationEvent;

public class OpportunityStageChangedEvent extends ApplicationEvent {

    private final Opportunity opportunity;
    private final String oldStage;
    private final String newStage;

    public OpportunityStageChangedEvent(Object source, Opportunity opportunity, String oldStage, String newStage) {
        super(source);
        this.opportunity = opportunity;
        this.oldStage = oldStage;
        this.newStage = newStage;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public String getOldStage() {
        return oldStage;
    }

    public String getNewStage() {
        return newStage;
    }
}
