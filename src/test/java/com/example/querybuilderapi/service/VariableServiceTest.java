package com.example.querybuilderapi.service;

import com.example.querybuilderapi.model.Variable;
import com.example.querybuilderapi.repository.VariableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VariableServiceTest {

    @Mock
    private VariableRepository variableRepository;

    @InjectMocks
    private VariableService variableService;

    private Variable sampleVariable;

    @BeforeEach
    void setUp() {
        sampleVariable = new Variable(1L, "firstName", "First Name", 8, "STRING");
    }

    // --- getAllVariables ---

    @Test
    @DisplayName("getAllVariables returns variables ordered alphabetically")
    void getAllVariables_returnsOrdered() {
        Variable var2 = new Variable(2L, "age", "Age", 0, "UDINT");
        when(variableRepository.findAllByOrderByNameAsc()).thenReturn(List.of(var2, sampleVariable));

        List<Variable> result = variableService.getAllVariables();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("age");
        assertThat(result.get(1).getName()).isEqualTo("firstName");
        verify(variableRepository, times(1)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("getAllVariables returns empty list when no variables exist")
    void getAllVariables_returnsEmptyList() {
        when(variableRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());

        List<Variable> result = variableService.getAllVariables();

        assertThat(result).isEmpty();
        verify(variableRepository, times(1)).findAllByOrderByNameAsc();
    }

    // --- getVariableById ---

    @Test
    @DisplayName("getVariableById returns variable when found")
    void getVariableById_found() {
        when(variableRepository.findById(1L)).thenReturn(Optional.of(sampleVariable));

        Variable result = variableService.getVariableById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("firstName");
        assertThat(result.getLabel()).isEqualTo("First Name");
        assertThat(result.getType()).isEqualTo("STRING");
        verify(variableRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getVariableById returns null when not found")
    void getVariableById_notFound() {
        when(variableRepository.findById(999L)).thenReturn(Optional.empty());

        Variable result = variableService.getVariableById(999L);

        assertThat(result).isNull();
        verify(variableRepository, times(1)).findById(999L);
    }

    // --- seedVariables ---

    @Test
    @DisplayName("seedVariables seeds data when database is empty")
    void seedVariables_seedsWhenEmpty() throws Exception {
        when(variableRepository.count()).thenReturn(0L);
        when(variableRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        variableService.seedVariables().run();

        verify(variableRepository, times(1)).count();
        verify(variableRepository, times(1)).saveAll(argThat(list ->
                ((List<?>) list).size() == 7
        ));
    }

    @Test
    @DisplayName("seedVariables does not seed when database already has data")
    void seedVariables_skipsWhenNotEmpty() throws Exception {
        when(variableRepository.count()).thenReturn(7L);

        variableService.seedVariables().run();

        verify(variableRepository, times(1)).count();
        verify(variableRepository, never()).saveAll(anyList());
    }
}
