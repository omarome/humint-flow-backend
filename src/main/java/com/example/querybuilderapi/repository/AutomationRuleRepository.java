package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID>, JpaSpecificationExecutor<AutomationRule> {

    List<AutomationRule> findByTriggerEntityAndTriggerEventAndIsActiveAndIsDeletedFalse(
            String triggerEntity, String triggerEvent, Boolean isActive);

    List<AutomationRule> findByIsDeletedFalseOrderByCreatedAtDesc();
}
