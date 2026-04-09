package com.example.querybuilderapi.controller;

import com.example.querybuilderapi.config.TestSecurityConfig;
import com.example.querybuilderapi.dto.ContactResponse;
import com.example.querybuilderapi.service.ContactService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContactController.class)
@Import(TestSecurityConfig.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    private static final UUID CONTACT_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID ORG_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    // ─── GET /api/sales/contacts ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/sales/contacts returns 200 and paginated list")
    void listContacts_returnsPaged() throws Exception {
        ContactResponse contact = buildResponse();
        Page<ContactResponse> page = new PageImpl<>(List.of(contact));

        when(contactService.listContacts(eq(0), eq(20), eq("lastName"), eq("asc")))
                .thenReturn(page);

        mockMvc.perform(get("/api/sales/contacts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fullName", is("John Doe")))
                .andExpect(jsonPath("$.content[0].organizationName", is("Acme Corp")))
                .andExpect(jsonPath("$.content[0].lifecycleStage", is("SQL")));
    }

    @Test
    @DisplayName("GET /api/sales/contacts?organizationId=... returns contacts for org")
    void listContacts_byOrganization() throws Exception {
        ContactResponse contact = buildResponse();
        when(contactService.getContactsByOrganization(ORG_ID)).thenReturn(List.of(contact));

        mockMvc.perform(get("/api/sales/contacts")
                        .param("organizationId", ORG_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId", is(ORG_ID.toString())));
    }

    @Test
    @DisplayName("GET /api/sales/contacts?search=John returns search results")
    void listContacts_withSearch() throws Exception {
        ContactResponse contact = buildResponse();
        Page<ContactResponse> page = new PageImpl<>(List.of(contact));

        when(contactService.searchContacts(eq("John"), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/sales/contacts")
                        .param("search", "John")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName", is("John")));
    }

    // ─── GET /api/sales/contacts/{id} ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/sales/contacts/{id} returns 200 with flattened org data")
    void getContact_found() throws Exception {
        ContactResponse contact = buildResponse();
        when(contactService.getContact(CONTACT_ID)).thenReturn(contact);

        mockMvc.perform(get("/api/sales/contacts/" + CONTACT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(CONTACT_ID.toString())))
                .andExpect(jsonPath("$.fullName", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@acme.com")))
                .andExpect(jsonPath("$.organizationId", is(ORG_ID.toString())))
                .andExpect(jsonPath("$.organizationName", is("Acme Corp")));
    }

    // ─── POST /api/sales/contacts ──────────────────────────────────────────

    @Test
    @DisplayName("POST /api/sales/contacts creates and returns 201")
    void createContact_returns201() throws Exception {
        ContactResponse response = buildResponse();
        when(contactService.createContact(any())).thenReturn(response);

        String requestJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@acme.com",
                    "phone": "+1-555-0101",
                    "jobTitle": "VP of Sales",
                    "leadScore": 85,
                    "lifecycleStage": "SQL",
                    "organizationId": "%s"
                }
                """.formatted(ORG_ID);

        mockMvc.perform(post("/api/sales/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("POST /api/sales/contacts returns 400 when firstName is blank")
    void createContact_missingName_returns400() throws Exception {
        String requestJson = """
                {
                    "firstName": "",
                    "lastName": "Doe"
                }
                """;

        mockMvc.perform(post("/api/crm/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    // ─── PATCH /api/crm/contacts/{id} ────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/crm/contacts/{id} partial update returns 200")
    void patchContact_returns200() throws Exception {
        ContactResponse response = buildResponse();
        when(contactService.patchContact(eq(CONTACT_ID), any())).thenReturn(response);

        String requestJson = """
                {
                    "jobTitle": "CTO"
                }
                """;

        mockMvc.perform(patch("/api/crm/contacts/" + CONTACT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    // ─── DELETE /api/crm/contacts/{id} ───────────────────────────────────

    @Test
    @DisplayName("DELETE /api/crm/contacts/{id} soft deletes and returns 204")
    void deleteContact_returns204() throws Exception {
        doNothing().when(contactService).deleteContact(CONTACT_ID);

        mockMvc.perform(delete("/api/crm/contacts/" + CONTACT_ID))
                .andExpect(status().isNoContent());
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private ContactResponse buildResponse() {
        com.example.querybuilderapi.model.Organization org =
                new com.example.querybuilderapi.model.Organization();
        org.setId(ORG_ID);
        org.setName("Acme Corp");

        com.example.querybuilderapi.model.Contact contact =
                new com.example.querybuilderapi.model.Contact();
        contact.setId(CONTACT_ID);
        contact.setFirstName("John");
        contact.setLastName("Doe");
        contact.setEmail("john.doe@acme.com");
        contact.setPhone("+1-555-0101");
        contact.setJobTitle("VP of Sales");
        contact.setDepartment("Sales");
        contact.setLeadSource("Website");
        contact.setLeadScore(85);
        contact.setLifecycleStage("SQL");
        contact.setOrganization(org);
        contact.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        contact.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        return ContactResponse.fromEntity(contact);
    }
}
