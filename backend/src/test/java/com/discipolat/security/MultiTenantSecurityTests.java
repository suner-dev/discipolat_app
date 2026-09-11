package com.discipolat.security;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MultiTenantSecurityTests — Tests d'isolation cross-tenant (Section 69 du prompt).
 * Verifie qu'un utilisateur d'un tenant NE PEUT PAS acceder aux donnees d'un autre tenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, TestJwtConfig.class})
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class MultiTenantSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TENANT_A_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_A_ID = UUID.fromString("10000000-0000-0000-0000-000000000010");
    private static final UUID MEMBER_A_ID = UUID.fromString("10000000-0000-0000-0000-000000000011");
    private static final UUID MEMBER_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000011");
    private static final UUID SOUL_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000100");

    @BeforeEach
    void setUp() { TenantContext.clear(); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    @Order(1)
    @DisplayName("Tenant A peut acceder a ses propres donnees")
    void tenantA_CanAccessOwnSouls() throws Exception {
        String token = generateToken(ADMIN_A_ID, "admin-a@discipolat.com", "TENANT_ADMIN", TENANT_A_ID);
        mockMvc.perform(get("/api/v1/souls").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("Tenant A ne peut PAS acceder aux souls de Tenant B")
    void tenantA_CannotAccessTenantB_Souls() throws Exception {
        String token = generateToken(MEMBER_A_ID, "member-a@discipolat.com", "MEMBER", TENANT_A_ID);
        mockMvc.perform(get("/api/v1/souls/" + SOUL_B_ID).header("Authorization", "Bearer " + token))
                .andExpect(anyOfStatus(403, 404));
    }

    @Test
    @Order(3)
    @DisplayName("Tenant B ne peut PAS acceder aux souls de Tenant A")
    void tenantB_CannotAccessTenantA_Souls() throws Exception {
        String token = generateToken(MEMBER_B_ID, "member-b@discipolat.com", "MEMBER", TENANT_B_ID);
        mockMvc.perform(get("/api/v1/souls/" + UUID.fromString("10000000-0000-0000-0000-000000000100"))
                .header("Authorization", "Bearer " + token))
                .andExpect(anyOfStatus(403, 404));
    }

    @Test
    @Order(10)
    @DisplayName("MEMBRE ne peut PAS acceder aux endpoints admin")
    void member_CannotAccessAdmin() throws Exception {
        String token = generateToken(MEMBER_A_ID, "member-a@discipolat.com", "MEMBER", TENANT_A_ID);
        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("MEMBRE ne peut PAS creer de roles")
    void member_CannotCreateRoles() throws Exception {
        String token = generateToken(MEMBER_A_ID, "member-a@discipolat.com", "MEMBER", TENANT_A_ID);
        mockMvc.perform(post("/api/v1/admin/roles").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"key\":\"HACKER\",\"label\":\"Hacker\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(30)
    @DisplayName("IDOR: manipulation cross-tenant refusee")
    void idor_CrossTenantDenied() throws Exception {
        String token = generateToken(MEMBER_A_ID, "member-a@discipolat.com", "MEMBER", TENANT_A_ID);
        mockMvc.perform(put("/api/v1/souls/" + SOUL_B_ID).header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"nom\":\"HACKED\"}"))
                .andExpect(anyOfStatus(403, 404));
    }

    @Test
    @Order(40)
    @DisplayName("Requete sans token = 401")
    void noToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/souls")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(41)
    @DisplayName("Token invalide = 401")
    void invalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/souls").header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(50)
    @DisplayName("PLATFORM_SUPER_ADMIN peut acceder au dashboard plateforme")
    void superAdmin_CanAccessPlatform() throws Exception {
        String token = generateToken(ADMIN_A_ID, "super@discipolat.com", "PLATFORM_SUPER_ADMIN", null);
        mockMvc.perform(get("/api/v1/platform/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(51)
    @DisplayName("TENANT_ADMIN ne peut PAS acceder au dashboard plateforme")
    void tenantAdmin_CannotAccessPlatform() throws Exception {
        String token = generateToken(ADMIN_A_ID, "admin-a@discipolat.com", "TENANT_ADMIN", TENANT_A_ID);
        mockMvc.perform(get("/api/v1/platform/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String generateToken(UUID userId, String email, String role, UUID tenantId) {
        return jwtTokenProvider.generateAccessToken(userId, email, role,
                java.util.Set.of(role), false, tenantId);
    }

    private static org.springframework.test.web.servlet.ResultMatcher anyOfStatus(int... statuses) {
        return result -> {
            int actual = result.getResponse().getStatus();
            for (int s : statuses) { if (actual == s) return; }
            throw new AssertionError("Status " + actual + " not in expected: " + java.util.Arrays.toString(statuses));
        };
    }
}
