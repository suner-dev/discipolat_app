package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.platform.domain.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

/**
 * Tests de régression API du {@link FeedbackController} (retours testeurs).
 *
 * Exerce la chaîne de sécurité RÉELLE (@WebMvcTest + SecurityConfig +
 * JwtTokenProvider réel) : soumission authentifiée, consultation
 * ADMIN/PASTEUR, changement de statut ADMIN — et surtout les refus
 * (401 sans token, 403 pour un rôle non habilité avec `verify(never)`).
 */
@WebMvcTest(FeedbackController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private SecurityUtils securityUtils;

    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "testeur@discipolat.com";

    private FeedbackResponse sample;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        sample = new FeedbackResponse(
                UUID.randomUUID(), "BUG", "HAUTE", "Sujet de test", "Description",
                "/dashboard", "Chrome", "Desktop", "Linux", "1.0.0", "NOUVEAU",
                USER_ID, EMAIL, LocalDateTime.now(), LocalDateTime.now());
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false, null);
    }

    private static final String VALID_BODY = """
            {"category":"BUG","priority":"HAUTE","subject":"Le bouton export ne répond pas","description":"Étapes : ..."}
            """;

    // ======================== Soumission (authentifié) ========================

    @Test
    @DisplayName("POST /feedback sans token → 401")
    void create_sansToken_401() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verify(feedbackService, never()).create(any(), any());
    }

    @Test
    @DisplayName("POST /feedback authentifié → 201 et délégation au service avec l'utilisateur courant")
    void create_authentifie_201() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(feedbackService.create(eq(USER_ID), any(CreateFeedbackRequest.class))).thenReturn(sample);

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", bearer("MEMBRE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Sujet de test"))
                .andExpect(jsonPath("$.reporterEmail").value(EMAIL));

        verify(feedbackService).create(eq(USER_ID), any(CreateFeedbackRequest.class));
    }

    // ======================== Consultation (ADMIN/PASTEUR) ========================

    @Test
    @DisplayName("GET /admin/feedback par ADMIN → 200")
    void list_parAdmin_200() throws Exception {
        when(feedbackService.listAll()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Sujet de test"));
    }

    @Test
    @DisplayName("GET /admin/feedback par PASTEUR → 200")
    void list_parPasteur_200() throws Exception {
        when(feedbackService.listAll()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /admin/feedback par MEMBRE → 403 sans toucher au service")
    void list_parMembre_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isForbidden());

        verify(feedbackService, never()).listAll();
    }

    @Test
    @DisplayName("GET /admin/feedback sans token → 401")
    void list_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedback"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/feedback/stats par PASTEUR → 200")
    void stats_parPasteur_200() throws Exception {
        when(feedbackService.stats()).thenReturn(new FeedbackStatsResponse(
                5, 2, 1, 1, 1, Map.of("BUG", 3L, "SUGGESTION", 2L)));

        mockMvc.perform(get("/api/v1/admin/feedback/stats")
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.parCategorie.BUG").value(3));
    }

    // ======================== Changement de statut (ADMIN) ========================

    @Test
    @DisplayName("PATCH /admin/feedback/{id}/status par ADMIN → 200")
    void updateStatus_parAdmin_200() throws Exception {
        when(feedbackService.updateStatus(eq(sample.id()), eq("RESOLU"))).thenReturn(sample);

        mockMvc.perform(patch("/api/v1/admin/feedback/" + sample.id() + "/status")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLU\"}"))
                .andExpect(status().isOk());

        verify(feedbackService).updateStatus(eq(sample.id()), eq("RESOLU"));
    }

    @Test
    @DisplayName("PATCH /admin/feedback/{id}/status par PASTEUR → 200 (admin ouvert au pasteur)")
    void updateStatus_parPasteur_200() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/feedback/" + sample.id() + "/status")
                        .header("Authorization", bearer("PASTEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLU\"}"))
                .andExpect(status().isOk());

        verify(feedbackService).updateStatus(eq(sample.id()), eq("RESOLU"));
    }
}
