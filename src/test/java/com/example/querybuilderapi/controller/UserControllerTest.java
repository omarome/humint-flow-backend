package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.model.User;
import com.example.querybuilderapi.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // --- GET /api/users ---

    @Test
    @DisplayName("GET /api/users returns 200 and list of users")
    void getAllUsers_returnsOk() throws Exception {
        User user1 = new User(1L, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales");
        User user2 = new User(2L, "Jane", "Smith",
                "jane.smith@example.com", "Active", false, "Manager", "Sales");

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].fullName", is("John Doe")))
                .andExpect(jsonPath("$[0].email", is("john.doe@example.com")))
                .andExpect(jsonPath("$[0].isOnline", is(true)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].fullName", is("Jane Smith")));
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

    @Test
    @DisplayName("GET /api/users?sortBy=age&sortDir=desc returns users sorted by age descending")
    void getAllUsers_sorted() throws Exception {
        User older = new User(2L, "Jane", "Smith",
                "jane.smith@example.com", "Active", false, "Manager", "Sales");
        User younger = new User(1L, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales");

        when(userService.getAllUsers(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(older, younger));

        mockMvc.perform(get("/api/users")
                        .param("sortBy", "age")
                        .param("sortDir", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- GET /api/users/{id} ---

    @Test
    @DisplayName("GET /api/users/1 returns 200 and user when found")
    void getUserById_found() throws Exception {
        User user = new User(1L, "John", "Doe",
                "john.doe@example.com", "Active", true, "Sales", "Inside Sales");

        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fullName", is("John Doe")))

                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.status", is("Active")))
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
