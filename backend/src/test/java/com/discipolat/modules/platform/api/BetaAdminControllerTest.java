package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.platform.domain.BetaResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de régression API du {@link BetaAdminController} (environnement bêta).
 *
 * Le reset bêta supprime TOUTES les données métier : il doit être réservé
 * à l'ADMIN (403 pour les autres, même PASTEUR) — en plus de la double garde
 * métier du service (environnement prod + flag), testée dans
 * BetaResetServiceTest.
 */
@WebMvcTest(BetaAdminController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class BetaAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private BetaResetService betaResetService;

    private static final UUID ADMIN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String EMAIL = "admin@discipolat.com";

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                ADMIN_ID, EMAIL, role, Set.of(role), false, null);
    }

    // ======================== Status ========================

    @Test
    @DisplayName("GET /admin/beta/status par ADMIN → 200 avec l'état de l'environnement")
    void status_parAdmin_200() throws Exception {
        when(betaResetService.status()).thenReturn(Map.of(
                "environment", "beta", "resetEnabled", true));

        mockMvc.perform(get("/api/v1/admin/beta/status")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("beta"))
                .andExpect(jsonPath("$.resetEnabled").value(true));
    }

    @Test
    @DisplayName("GET /admin/beta/status par PASTEUR → 403 (réservé ADMIN)")
    void status_parPasteur_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/beta/status")
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isForbidden());

        verify(betaResetService, never()).status();
    }

    // ======================== Reset ========================

    @Test
    @DisplayName("POST /admin/beta/reset par ADMIN → 200")
    void reset_parAdmin_200() throws Exception {
        when(betaResetService.reset()).thenReturn(Map.of(
                "status", "OK", "environment", "beta", "truncatedTables", 12));

        mockMvc.perform(post("/api/v1/admin/beta/reset")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.truncatedTables").value(12));
    }

    @Test
    @DisplayName("POST /admin/beta/reset par PASTEUR → 403 sans déclencher le reset")
    void reset_parPasteur_403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/beta/reset")
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isForbidden());

        verify(betaResetService, never()).reset();
    }

    @Test
    @DisplayName("POST /admin/beta/reset par MEMBRE → 403")
    void reset_parMembre_403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/beta/reset")
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isForbidden());

        verify(betaResetService, never()).reset();
    }

    @Test
    @DisplayName("POST /admin/beta/reset sans token → 401")
    void reset_sansToken_401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/beta/reset"))
                .andExpect(status().isUnauthorized());

        verify(betaResetService, never()).reset();
    }
}
