package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.model.Variable;
import com.example.querybuilderapi.service.VariableService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VariableController.class)
@Import(TestSecurityConfig.class)
class VariableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VariableService variableService;

    // --- GET /api/variables ---

    @Test
    @DisplayName("GET /api/variables returns 200 and list of variables alphabetically")
    void getAllVariables_returnsOk() throws Exception {
        Variable var1 = new Variable(1L, "age", "Age", 0, "UDINT");
        Variable var2 = new Variable(2L, "firstName", "First Name", 8, "STRING");
        Variable var3 = new Variable(3L, "isOnline", "Is Online", 12, "BOOL");

        when(variableService.getAllVariables()).thenReturn(List.of(var1, var2, var3));

        mockMvc.perform(get("/api/variables")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("age")))
                .andExpect(jsonPath("$[0].label", is("Age")))
                .andExpect(jsonPath("$[0].offset", is(0)))
                .andExpect(jsonPath("$[0].type", is("UDINT")))
                .andExpect(jsonPath("$[1].name", is("firstName")))
                .andExpect(jsonPath("$[1].label", is("First Name")))
                .andExpect(jsonPath("$[1].type", is("STRING")))
                .andExpect(jsonPath("$[2].name", is("isOnline")))
                .andExpect(jsonPath("$[2].type", is("BOOL")));
    }

    @Test
    @DisplayName("GET /api/variables returns 200 and empty list when no variables")
    void getAllVariables_returnsEmptyList() throws Exception {
        when(variableService.getAllVariables()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/variables")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/variables/{id} ---

    @Test
    @DisplayName("GET /api/variables/1 returns 200 and variable when found")
    void getVariableById_found() throws Exception {
        Variable var = new Variable(1L, "firstName", "First Name", 8, "STRING");

        when(variableService.getVariableById(1L)).thenReturn(var);

        mockMvc.perform(get("/api/variables/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("firstName")))
                .andExpect(jsonPath("$.label", is("First Name")))
                .andExpect(jsonPath("$.offset", is(8)))
                .andExpect(jsonPath("$.type", is("STRING")));
    }

    @Test
    @DisplayName("GET /api/variables/999 returns 404 when not found")
    void getVariableById_notFound() throws Exception {
        when(variableService.getVariableById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/variables/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
