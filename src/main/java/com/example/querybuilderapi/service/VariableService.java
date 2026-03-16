package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.Variable;
import com.example.querybuilderapi.repository.VariableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Variable service backed by a PostgreSQL database via Spring Data JPA.
 */
@Service
public class VariableService {

    private final VariableRepository variableRepository;

    public VariableService(VariableRepository variableRepository) {
        this.variableRepository = variableRepository;
    }

    /**
     * Returns all variables ordered alphabetically by name.
     */
    public List<Variable> getAllVariables() {
        return variableRepository.findAllByOrderByNameAsc();
    }

    /**
     * Returns a variable by id, or null if not found.
     */
    public Variable getVariableById(Long id) {
        return variableRepository.findById(id).orElse(null);
    }
}
