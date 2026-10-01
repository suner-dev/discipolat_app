package com.discipolat.modules.authentication.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.platform.domain.TenantRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Constat M2 / contrat §3.3 — endpoint public de suivi d'une demande
 * d'inscription : {@code POST /api/v1/auth/registration-status}.
 *
 * <p>Protections vérifiées ici : rate-limit par IP (429 + {@code Retry-After}),
 * {@code Cache-Control: no-store} sur TOUTES les réponses, et absence totale de
 * fuite d'information (ni mot de passe, ni nom d'organisation, ni compte).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerRegistrationStatusTest {

    @Mock private com.discipolat.modules.authentication.domain.AuthService authService;
    @Mock private PerIpRateLimiter rateLimiter;
    @Mock private com.discipolat.modules.tenants.domain.AuthorizationService authorizationService;
    @Mock private TenantRegistrationService tenantRegistrationService;
    @Mock private AuthResponseFactory authResponseFactory;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authService, rateLimiter, authorizationService, authResponseFactory,
                tenantRegistrationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void givenRateLimit(boolean allowed) {
        when(rateLimiter.tryConsumeRegistrationStatus(anyString()))
                .thenReturn(new RateLimitResult(allowed, allowed ? 2 : 0, 300));
    }

    // ---------- cas nominaux ----------

    @Test
    @DisplayName("Aucune demande : NONE, canLogin=false, no-store")
    void unknownEmailReturnsNone() throws Exception {
        givenRateLimit(true);
        when(tenantRegistrationService.registrationStatus("inconnu@example.com"))
                .thenReturn(new RegistrationStatusResponse("NONE", null, null, false));

        mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.canLogin").value(false))
                .andExpect(jsonPath("$.decidedAt").doesNotExist())
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    @DisplayName("Demande en attente : PENDING_APPROVAL, canLogin=false")
    void pendingReturnsPendingApproval() throws Exception {
        givenRateLimit(true);
        when(tenantRegistrationService.registrationStatus("jean@example.com"))
                .thenReturn(new RegistrationStatusResponse("PENDING_APPROVAL", null, null, false));

        mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jean@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.canLogin").value(false));
    }

    @Test
    @DisplayName("Demande approuvée : APPROVED + canLogin=true + decidedAt")
    void approvedReturnsCanLoginTrue() throws Exception {
        givenRateLimit(true);
        Instant decidedAt = Instant.parse("2026-09-27T10:00:00Z");
        when(tenantRegistrationService.registrationStatus("jean@example.com"))
                .thenReturn(new RegistrationStatusResponse("APPROVED", decidedAt, null, true));

        mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jean@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.canLogin").value(true))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Demande rejetée : REJECTED + motif communiqué")
    void rejectedReturnsReason() throws Exception {
        givenRateLimit(true);
        when(tenantRegistrationService.registrationStatus("jean@example.com"))
                .thenReturn(new RegistrationStatusResponse(
                        "REJECTED", Instant.parse("2026-09-27T10:00:00Z"),
                        "Informations complémentaires nécessaires", false));

        mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jean@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Informations complémentaires nécessaires"))
                .andExpect(jsonPath("$.canLogin").value(false));
    }

    // ---------- rate limit ----------

    @Test
    @DisplayName("Quota IP dépassé : 429 + Retry-After + no-store, service NON appelé")
    void rateLimitedReturns429() throws Exception {
        givenRateLimit(false);

        mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jean@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.status").value("NONE"));

        // Aucune interrogation métier : le rate-limit protège aussi la base.
        verify(tenantRegistrationService, never()).registrationStatus(anyString());
    }

    // ---------- anti-fuite ----------

    @Test
    @DisplayName("La réponse n'expose ni mot de passe, ni organisation, ni compte")
    void responseLeaksNothingSensitive() throws Exception {
        givenRateLimit(true);
        when(tenantRegistrationService.registrationStatus("jean@example.com"))
                .thenReturn(new RegistrationStatusResponse("PENDING_APPROVAL", null, null, false));

        String body = mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jean@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("password")
                .doesNotContain("passwordHash")
                .doesNotContain("organizationName")
                .doesNotContain("userId")
                .doesNotContain("slug")
                .doesNotContain("tenantId");
        // Les seuls champs du contrat §3.3.
        org.assertj.core.api.Assertions.assertThat(body).contains("status", "canLogin");
    }

    @Test
    @DisplayName("L'email est transmis tel quel : la normalisation est faite par le service")
    void emailIsForwardedToTheService() throws Exception {
        givenRateLimit(true);
        when(tenantRegistrationService.registrationStatus(anyString()))
                .thenReturn(new RegistrationStatusResponse("NONE", null, null, false));

        mockMvc.perform(post("/api/v1/auth/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Jean@Example.COM\"}"))
                .andExpect(status().isOk());

        verify(tenantRegistrationService).registrationStatus("Jean@Example.COM");
    }
}
