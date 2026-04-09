package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.dto.OpportunityResponse;
import com.example.querybuilderapi.service.OpportunityService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpportunityController.class)
@Import(TestSecurityConfig.class)
class OpportunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpportunityService opportunityService;

    private static final UUID OPP_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
    private static final UUID ORG_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID CONTACT_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    // ─── GET /api/sales/opportunities ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/sales/opportunities returns 200 and paginated list")
    void listOpportunities_returnsPaged() throws Exception {
        OpportunityResponse opp = buildResponse();
        Page<OpportunityResponse> page = new PageImpl<>(List.of(opp));

        when(opportunityService.listOpportunities(eq(0), eq(20), eq("name"), eq("asc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/sales/opportunities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Enterprise Deal")))
                .andExpect(jsonPath("$.content[0].organizationName", is("Acme Corp")))
                .andExpect(jsonPath("$.content[0].primaryContactName", is("John Doe")))
                .andExpect(jsonPath("$.content[0].stage", is("NEGOTIATION")));
    }

    @Test
    @DisplayName("GET /api/sales/opportunities?stage=NEGOTIATION returns stage-filtered results")
    void listOpportunities_byStage() throws Exception {
        OpportunityResponse opp = buildResponse();
        when(opportunityService.getOpportunitiesByStage("NEGOTIATION")).thenReturn(List.of(opp));

        mockMvc.perform(get("/api/sales/opportunities")
                        .param("stage", "NEGOTIATION")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].stage", is("NEGOTIATION")));
    }

    @Test
    @DisplayName("GET /api/sales/opportunities?organizationId=... returns org-filtered results")
    void listOpportunities_byOrganization() throws Exception {
        OpportunityResponse opp = buildResponse();
        when(opportunityService.getOpportunitiesByOrganization(ORG_ID)).thenReturn(List.of(opp));

        mockMvc.perform(get("/api/sales/opportunities")
                        .param("organizationId", ORG_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId", is(ORG_ID.toString())));
    }

    // ─── GET /api/sales/opportunities/{id} ─────────────────────────────────

    @Test
    @DisplayName("GET /api/sales/opportunities/{id} returns 200 with flattened data")
    void getOpportunity_found() throws Exception {
        OpportunityResponse opp = buildResponse();
        when(opportunityService.getOpportunity(OPP_ID)).thenReturn(opp);

        mockMvc.perform(get("/api/sales/opportunities/" + OPP_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(OPP_ID.toString())))
                .andExpect(jsonPath("$.name", is("Enterprise Deal")))
                .andExpect(jsonPath("$.amount", is(50000.00)))
                .andExpect(jsonPath("$.organizationName", is("Acme Corp")))
                .andExpect(jsonPath("$.primaryContactName", is("John Doe")));
    }

    // ─── POST /api/sales/opportunities ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/sales/opportunities creates and returns 201")
    void createOpportunity_returns201() throws Exception {
        OpportunityResponse response = buildResponse();
        when(opportunityService.createOpportunity(any())).thenReturn(response);

        String requestJson = """
                {
                    "name": "Enterprise Deal",
                    "amount": 50000.00,
                    "stage": "NEGOTIATION",
                    "probability": 80,
                    "expectedCloseDate": "2026-06-30",
                    "organizationId": "%s",
                    "primaryContactId": "%s"
                }
                """.formatted(ORG_ID, CONTACT_ID);

        mockMvc.perform(post("/api/sales/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Enterprise Deal")));
    }

    @Test
    @DisplayName("POST /api/sales/opportunities returns 400 when name is blank")
    void createOpportunity_missingName_returns400() throws Exception {
        String requestJson = """
                {
                    "name": "",
                    "stage": "PROSPECTING",
                    "organizationId": "%s"
                }
                """.formatted(ORG_ID);

        mockMvc.perform(post("/api/sales/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    // ─── PATCH /api/sales/opportunities/{id} ───────────────────────────────

    @Test
    @DisplayName("PATCH /api/sales/opportunities/{id} (stage change for Kanban drag) returns 200")
    void patchOpportunity_stageChange_returns200() throws Exception {
        OpportunityResponse response = buildResponse();
        when(opportunityService.patchOpportunity(eq(OPP_ID), any())).thenReturn(response);

        String requestJson = """
                {
                    "stage": "CLOSED_WON"
                }
                """;

        mockMvc.perform(patch("/api/crm/opportunities/" + OPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    // ─── DELETE /api/crm/opportunities/{id} ──────────────────────────────

    @Test
    @DisplayName("DELETE /api/crm/opportunities/{id} soft deletes and returns 204")
    void deleteOpportunity_returns204() throws Exception {
        doNothing().when(opportunityService).deleteOpportunity(OPP_ID);

        mockMvc.perform(delete("/api/crm/opportunities/" + OPP_ID))
                .andExpect(status().isNoContent());
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private OpportunityResponse buildResponse() {
        com.example.querybuilderapi.model.Organization org =
                new com.example.querybuilderapi.model.Organization();
        org.setId(ORG_ID);
        org.setName("Acme Corp");

        com.example.querybuilderapi.model.Contact contact =
                new com.example.querybuilderapi.model.Contact();
        contact.setId(CONTACT_ID);
        contact.setFirstName("John");
        contact.setLastName("Doe");

        com.example.querybuilderapi.model.Opportunity opp =
                new com.example.querybuilderapi.model.Opportunity();
        opp.setId(OPP_ID);
        opp.setName("Enterprise Deal");
        opp.setAmount(new BigDecimal("50000.00"));
        opp.setStage("NEGOTIATION");
        opp.setProbability(80);
        opp.setExpectedCloseDate(LocalDate.of(2026, 6, 30));
        opp.setOrganization(org);
        opp.setPrimaryContact(contact);
        opp.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        opp.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        return OpportunityResponse.fromEntity(opp);
    }
}
