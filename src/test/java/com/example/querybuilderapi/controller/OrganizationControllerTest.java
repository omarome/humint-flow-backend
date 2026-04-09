package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.dto.OrganizationResponse;
import com.example.querybuilderapi.service.OrganizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@Import(TestSecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    private static final UUID SAMPLE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    // ─── GET /api/sales/organizations ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/sales/organizations returns 200 and paginated list")
    void listOrganizations_returnsPagedResults() throws Exception {
        OrganizationResponse org = buildResponse();
        Page<OrganizationResponse> page = new PageImpl<>(List.of(org));

        when(organizationService.listOrganizations(eq(0), eq(20), eq("name"), eq("asc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/sales/organizations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Acme Corp")))
                .andExpect(jsonPath("$.content[0].industry", is("Technology")))
                .andExpect(jsonPath("$.content[0].id", is(SAMPLE_ID.toString())));
    }

    @Test
    @DisplayName("GET /api/sales/organizations?search=Acme returns search results")
    void listOrganizations_withSearch() throws Exception {
        OrganizationResponse org = buildResponse();
        Page<OrganizationResponse> page = new PageImpl<>(List.of(org));

        when(organizationService.searchOrganizations(eq("Acme"), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/sales/organizations")
                        .param("search", "Acme")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Acme Corp")));
    }

    // ─── GET /api/sales/organizations/{id} ─────────────────────────────────

    @Test
    @DisplayName("GET /api/sales/organizations/{id} returns 200 when found")
    void getOrganization_found() throws Exception {
        OrganizationResponse org = buildResponse();
        when(organizationService.getOrganization(SAMPLE_ID)).thenReturn(org);

        mockMvc.perform(get("/api/sales/organizations/" + SAMPLE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(SAMPLE_ID.toString())))
                .andExpect(jsonPath("$.name", is("Acme Corp")))
                .andExpect(jsonPath("$.employeeCount", is(500)))
                .andExpect(jsonPath("$.annualRevenue", is(10000000.00)));
    }

    @Test
    @DisplayName("GET /api/sales/organizations/{id} returns 400 when not found")
    void getOrganization_notFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(organizationService.getOrganization(unknownId))
                .thenThrow(new IllegalArgumentException("Organization not found"));

        mockMvc.perform(get("/api/sales/organizations/" + unknownId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/sales/organizations ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/sales/organizations creates and returns 201")
    void createOrganization_returns201() throws Exception {
        OrganizationResponse response = buildResponse();
        when(organizationService.createOrganization(any())).thenReturn(response);

        String requestJson = """
                {
                    "name": "Acme Corp",
                    "industry": "Technology",
                    "website": "https://acme.com",
                    "phone": "+1-555-0100",
                    "city": "San Francisco",
                    "country": "USA",
                    "employeeCount": 500,
                    "annualRevenue": 10000000.00
                }
                """;

        mockMvc.perform(post("/api/sales/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Acme Corp")))
                .andExpect(jsonPath("$.id", is(SAMPLE_ID.toString())));
    }

    @Test
    @DisplayName("POST /api/sales/organizations returns 400 when name is blank")
    void createOrganization_missingName_returns400() throws Exception {
        String requestJson = """
                {
                    "name": "",
                    "industry": "Technology"
                }
                """;

        mockMvc.perform(post("/api/sales/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/crm/organizations/{id} ─────────────────────────────────

    @Test
    @DisplayName("PUT /api/crm/organizations/{id} updates and returns 200")
    void updateOrganization_returns200() throws Exception {
        OrganizationResponse response = buildResponse();
        when(organizationService.updateOrganization(eq(SAMPLE_ID), any())).thenReturn(response);

        String requestJson = """
                {
                    "name": "Acme Corp Updated",
                    "industry": "SaaS"
                }
                """;

        mockMvc.perform(put("/api/crm/organizations/" + SAMPLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    // ─── PATCH /api/crm/organizations/{id} ───────────────────────────────

    @Test
    @DisplayName("PATCH /api/crm/organizations/{id} partial update returns 200")
    void patchOrganization_returns200() throws Exception {
        OrganizationResponse response = buildResponse();
        when(organizationService.patchOrganization(eq(SAMPLE_ID), any())).thenReturn(response);

        String requestJson = """
                {
                    "city": "New York"
                }
                """;

        mockMvc.perform(patch("/api/crm/organizations/" + SAMPLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    // ─── DELETE /api/crm/organizations/{id} ──────────────────────────────

    @Test
    @DisplayName("DELETE /api/crm/organizations/{id} soft deletes and returns 204")
    void deleteOrganization_returns204() throws Exception {
        doNothing().when(organizationService).deleteOrganization(SAMPLE_ID);

        mockMvc.perform(delete("/api/crm/organizations/" + SAMPLE_ID))
                .andExpect(status().isNoContent());
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private OrganizationResponse buildResponse() {
        OrganizationResponse response = OrganizationResponse.fromEntity(buildOrganization());
        return response;
    }

    private com.example.querybuilderapi.model.Organization buildOrganization() {
        com.example.querybuilderapi.model.Organization org = new com.example.querybuilderapi.model.Organization();
        org.setId(SAMPLE_ID);
        org.setName("Acme Corp");
        org.setIndustry("Technology");
        org.setWebsite("https://acme.com");
        org.setPhone("+1-555-0100");
        org.setCity("San Francisco");
        org.setCountry("USA");
        org.setEmployeeCount(500);
        org.setAnnualRevenue(new BigDecimal("10000000.00"));
        org.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        org.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return org;
    }
}
