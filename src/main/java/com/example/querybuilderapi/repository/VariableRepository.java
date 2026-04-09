package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Variable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Variable entity.
 */
@Repository
public interface VariableRepository extends JpaRepository<Variable, Long> {

    /**
     * Returns all variables ordered alphabetically by name.
     */
    List<Variable> findAllByOrderByNameAsc();

    /**
     * Returns variables for a specific CRM entity type.
     */
    List<Variable> findByEntityTypeOrderByOffsetAsc(String entityType);

    /**
     * Returns legacy variables (entityType IS NULL) — original users table fields.
     */
    List<Variable> findByEntityTypeIsNullOrderByOffsetAsc();
}
