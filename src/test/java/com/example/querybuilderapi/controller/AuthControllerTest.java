package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.dto.AuthResponse;
import com.example.querybuilderapi.dto.LoginRequest;
import com.example.querybuilderapi.dto.RegisterRequest;
import com.example.querybuilderapi.exception.AccountNotInvitedException;
import com.example.querybuilderapi.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    // ── register ─────────────────────────────────────────────────────────────

    /**
     * Happy path: the email was pre-provisioned via the invite flow.
     * Service accepts the invite and returns 201 + tokens.
     */
    @Test
    public void register_invitedUser_shouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invited@example.com");
        request.setPassword("password123");
        request.setDisplayName("Invited User");

        AuthResponse response = new AuthResponse("access", "refresh", 900,
            new AuthResponse.UserInfo(1L, "invited@example.com", "Invited User", "SALES_REP", null));

        Mockito.when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.user.email").value("invited@example.com"));
    }

    /**
     * Closed self-registration gap (§9 risk #9):
     * An unknown email (not pre-provisioned) must receive 403, not 201.
     */
    @Test
    public void register_uninvitedEmail_shouldReturnForbidden() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("stranger@example.com");
        request.setPassword("password123");
        request.setDisplayName("Stranger");

        Mockito.when(authService.register(any(RegisterRequest.class)))
               .thenThrow(new AccountNotInvitedException("stranger@example.com"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Duplicate accept: calling /register for an already-active account
     * returns 400, not 201 (the invite was already consumed).
     */
    @Test
    public void register_alreadyActiveAccount_shouldReturnBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setDisplayName("Existing User");

        Mockito.when(authService.register(any(RegisterRequest.class)))
               .thenThrow(new IllegalArgumentException(
                       "An active account already exists for 'existing@example.com'. Use the login endpoint instead."));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    public void login_shouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        
        AuthResponse response = new AuthResponse("access", "refresh", 900, 
            new AuthResponse.UserInfo(1L, "test@example.com", "Test User", "SALES_REP", null));

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }
}
