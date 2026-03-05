package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.service.UserService;
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

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // --- GET /api/users ---

    @Test
    @DisplayName("GET /api/users returns 200 and list of users")
    void getAllUsers_returnsOk() throws Exception {
        User user1 = new User(1L, "John", "Doe", 28,
                "john.doe@example.com", "Active", "Johnny", true);
        User user2 = new User(2L, "Jane", "Smith", 32,
                "jane.smith@example.com", "Active", null, false);

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].firstName", is("John")))
                .andExpect(jsonPath("$[0].lastName", is("Doe")))
                .andExpect(jsonPath("$[0].email", is("john.doe@example.com")))
                .andExpect(jsonPath("$[0].isOnline", is(true)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].firstName", is("Jane")))
                .andExpect(jsonPath("$[1].nickname", nullValue()));
    }

    @Test
    @DisplayName("GET /api/users returns 200 and empty list when no users")
    void getAllUsers_returnsEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /api/users/{id} ---

    @Test
    @DisplayName("GET /api/users/1 returns 200 and user when found")
    void getUserById_found() throws Exception {
        User user = new User(1L, "John", "Doe", 28,
                "john.doe@example.com", "Active", "Johnny", true);

        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.age", is(28)))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.status", is("Active")))
                .andExpect(jsonPath("$.nickname", is("Johnny")))
                .andExpect(jsonPath("$.isOnline", is(true)));
    }

    @Test
    @DisplayName("GET /api/users/999 returns 404 when not found")
    void getUserById_notFound() throws Exception {
        when(userService.getUserById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
