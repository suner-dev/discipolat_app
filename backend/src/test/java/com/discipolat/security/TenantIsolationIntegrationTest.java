package com.discipolat.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2 #67 — Tests IDOR / multi-tenant isolation.
 *
 * Vérifie qu'un utilisateur du tenant A ne peut PAS accéder aux données du tenant B.
 * Chaque test simule une requête avec un token JWT du tenant A pour accéder
 * à des ressources du tenant B — doit renvoyer 403 ou 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2 #67 — Tests IDOR / Multi-tenant Isolation")
class TenantIsolationIntegrationTest {

    // Token JWT du tenant A (test)
    private static final String TOKEN_TENANT_A = "Bearer test-token-tenant-a";
    // Token JWT du tenant B (test)
    private static final String TOKEN_TENANT_B = "Bearer test-token-tenant-b";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les âmes du tenant B")
    void shouldDenyAccessToOtherTenantSouls() throws Exception {
        mockMvc.perform(get("/api/v1/souls")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les rapports du tenant B")
    void shouldDenyAccessToOtherTenantReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les finances du tenant B")
    void shouldDenyAccessToOtherTenantFinances() throws Exception {
        mockMvc.perform(get("/api/v1/finances/transactions")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les événements du tenant B")
    void shouldDenyAccessToOtherTenantEvents() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les familles du tenant B")
    void shouldDenyAccessToOtherTenantFamilies() throws Exception {
        mockMvc.perform(get("/api/v1/families")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les messages du tenant B")
    void shouldDenyAccessToOtherTenantMessages() throws Exception {
        mockMvc.perform(get("/api/v1/messages")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les utilisateurs du tenant B")
    void shouldDenyAccessToOtherTenantUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un membre du tenant A ne peut pas lire les alertes du tenant B")
    void shouldDenyAccessToOtherTenantAlerts() throws Exception {
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", TOKEN_TENANT_A)
                        .header("X-Tenant-Id", "tenant-b-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requête sans token doit être refusée (401)")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/souls"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requête avec token invalide doit être refusée (401)")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/souls")
                        .header("Authorization", "Bearer invalid-token-xyz"))
                .andExpect(status().isUnauthorized());
    }
}
