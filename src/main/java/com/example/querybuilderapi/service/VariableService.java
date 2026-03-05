package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.Variable;
import com.example.querybuilderapi.repository.VariableRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
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

    /**
     * Seeds the variables table with field metadata if empty.
     */
    @Bean
    CommandLineRunner seedVariables() {
        return args -> {
            if (variableRepository.count() == 0) {
                variableRepository.saveAll(List.of(
                    new Variable(null, "age", "Age", 0, "UDINT"),
                    new Variable(null, "email", "Email", 4, "EMAIL"),
                    new Variable(null, "firstName", "First Name", 8, "STRING"),
                    new Variable(null, "isOnline", "Is Online", 12, "BOOL"),
                    new Variable(null, "lastName", "Last Name", 16, "STRING"),
                    new Variable(null, "nickname", "Nickname", 20, "STRING"),
                    new Variable(null, "status", "Status", 24, "STRING")
                ));
                System.out.println("✅ Seeded 7 variables into the database.");
            }
        };
    }
}
