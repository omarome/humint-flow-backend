package com.example.querybuilderapi.repository;

import com.example.querybuilderapi.model.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class VariableRepositoryTest {

    @Autowired
    private VariableRepository variableRepository;

    @BeforeEach
    void setUp() {
        variableRepository.deleteAll();
    }

    @Test
    @DisplayName("save and findById returns the saved variable")
    void saveAndFindById() {
        Variable var = new Variable(null, "firstName", "First Name", 8, "STRING");

        Variable saved = variableRepository.save(var);

        assertThat(saved.getId()).isNotNull();

        Optional<Variable> found = variableRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("firstName");
        assertThat(found.get().getLabel()).isEqualTo("First Name");
        assertThat(found.get().getOffset()).isEqualTo(8);
        assertThat(found.get().getType()).isEqualTo("STRING");
    }

    @Test
    @DisplayName("findAllByOrderByNameAsc returns variables in alphabetical order")
    void findAllByOrderByNameAsc() {
        variableRepository.save(new Variable(null, "status", "Status", 24, "STRING"));
        variableRepository.save(new Variable(null, "age", "Age", 0, "UDINT"));
        variableRepository.save(new Variable(null, "firstName", "First Name", 8, "STRING"));

        List<Variable> variables = variableRepository.findAllByOrderByNameAsc();

        assertThat(variables).hasSize(3);
        assertThat(variables.get(0).getName()).isEqualTo("age");
        assertThat(variables.get(1).getName()).isEqualTo("firstName");
        assertThat(variables.get(2).getName()).isEqualTo("status");
    }

    @Test
    @DisplayName("findById returns empty when variable does not exist")
    void findById_notFound() {
        Optional<Variable> found = variableRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("deleteById removes the variable")
    void deleteById() {
        Variable saved = variableRepository.save(
                new Variable(null, "age", "Age", 0, "UDINT"));

        variableRepository.deleteById(saved.getId());

        Optional<Variable> found = variableRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("count returns correct number of variables")
    void count() {
        assertThat(variableRepository.count()).isZero();

        variableRepository.save(new Variable(null, "age", "Age", 0, "UDINT"));
        variableRepository.save(new Variable(null, "email", "Email", 4, "STRING"));

        assertThat(variableRepository.count()).isEqualTo(2);
    }
}
