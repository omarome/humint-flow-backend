package com.example.querybuilderapi.model;

/**
 * CRM entity types for polymorphic associations.
 * Used by Activity to reference any entity without a foreign key.
 */
public enum EntityType {
    ORGANIZATION,
    CONTACT,
    OPPORTUNITY
}
