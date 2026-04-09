package com.example.querybuilderapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Incoming body for POST /api/query.
 * Mirrors the react-querybuilder RuleGroup format so the frontend
 * can POST its query state directly without any transformation.
 *
 * Example:
 * {
 *   "entityType": "CONTACT",
 *   "combinator": "and",
 *   "rules": [
 *     { "field": "lifecycleStage", "operator": "=",  "value": "SQL" },
 *     { "field": "leadScore",      "operator": ">=", "value": "50"  }
 *   ],
 *   "page": 0,
 *   "size": 20
 * }
 */
public class CrmQueryRequest {

    @NotBlank
    private String entityType; // CONTACT | ORGANIZATION | OPPORTUNITY | ACTIVITY | TEAM_MEMBER

    private String combinator = "and"; // "and" | "or"

    private List<FilterRule> rules;

    private int page = 0;
    private int size = 20;

    // ── Inner types ──────────────────────────────────────────────────────

    public static class FilterRule {
        private String field;     // DB column name (e.g. "lifecycleStage", "organization.industry")
        private String operator;  // "=", "!=", "contains", "beginsWith", "endsWith", "<", "<=", ">", ">="
        private String value;
        // For nested rule groups
        private String combinator;
        private List<FilterRule> rules;

        public String getField()    { return field; }
        public String getOperator() { return operator; }
        public String getValue()    { return value; }
        public String getCombinator() { return combinator; }
        public List<FilterRule> getRules() { return rules; }

        public void setField(String field)       { this.field = field; }
        public void setOperator(String operator) { this.operator = operator; }
        public void setValue(String value)       { this.value = value; }
        public void setCombinator(String c)      { this.combinator = c; }
        public void setRules(List<FilterRule> r) { this.rules = r; }
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public String getEntityType()  { return entityType; }
    public String getCombinator()  { return combinator; }
    public List<FilterRule> getRules() { return rules; }
    public int getPage() { return page; }
    public int getSize() { return size; }

    public void setEntityType(String entityType)   { this.entityType = entityType; }
    public void setCombinator(String combinator)   { this.combinator = combinator; }
    public void setRules(List<FilterRule> rules)   { this.rules = rules; }
    public void setPage(int page) { this.page = page; }
    public void setSize(int size) { this.size = size; }
}
