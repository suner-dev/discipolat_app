package com.discipolat.modules.authentication.api;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat HTTP de {@code POST /api/v1/auth/register} pour la porte
 * « s'inscrire au nom d'une église » ({@code tenantSlug} web / {@code tenantId}
 * mobile).
 *
 * <p>Cette porte est le point exact de la faille refermée : elle appelait
 * directement {@code authService.registerInChurch}, sans jamais consulter le
 * {@code JoinMode} de l'église. Elle passe désormais par
 * {@code tenantJoinService.registerWithSlug}, et ce test verrouille que le
 * contrôleur ne puisse plus court-circuiter ce service — c'est-à-dire qu'un
 * simple retour de code pourrait réintroduire la faille sans que rien ne casse.
 *
 * <p>Les autres portes de {@code /register} sont rejouées ici : ce correctif ne
 * doit avoir rien déplacé ailleurs.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerRegisterChurchTest {

    private static final String SLUG = "eglise-bethel-de-cocody";
    private static final String CHURCH_NAME = "Église Bethel de Cocody";

    @Mock private com.discipolat.modules.authentication.domain.AuthService authService;
    @Mock private PerIpRateLimiter rateLimiter;
    @Mock private com.discipolat.modules.tenants.domain.AuthorizationService authorizationService;
    @Mock private AuthResponseFactory authResponseFactory;
    @Mock private com.discipolat.modules.platform.domain.TenantRegistrationService tenantRegistrationService;
    @Mock private com.discipolat.modules.tenants.domain.SelfServiceChurchService selfServiceChurchService;
    @Mock private com.discipolat.modules.tenants.domain.TenantJoinService tenantJoinService;

    private MockMvc mockMvc;

    /** Corps minimal valide : RegisterRequest impose les 3 consentements RGPD. */
    private static final String BASE_JSON = """
            {"email":"marie@example.com","password":"motdepasse123",
             "firstName":"Marie","lastName":"Kouassi",
             "consentCgu":true,"consentPrivacy":true,"consentArt9":true,"legalVersion":"2026-01-01"}
            """;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authService, rateLimiter, authorizationService, authResponseFactory,
                tenantRegistrationService, selfServiceChurchService, tenantJoinService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.discipolat.common.infrastructure.api.GlobalExceptionHandler())
                .build();
        when(rateLimiter.tryConsumeRegister(anyString()))
                .thenReturn(new RateLimitResult(true, 4, 60));
    }

    private static com.discipolat.modules.tenants.domain.TenantJoinService.RegisterOutcome outcome(
            String status) {
        return new com.discipolat.modules.tenants.domain.TenantJoinService.RegisterOutcome(
                status, null, CHURCH_NAME, "false");
    }

    private String body(String extraJson) {
        return "{" + extraJson + BASE_JSON.substring(1);
    }

    // ---------- la faille refermée ----------

    @Test
    @DisplayName("Église en APPROVAL : 202 PENDING_APPROVAL et AUCUN compte créé")
    void approvalChurchAnswersPendingWithoutCreatingAccount() throws Exception {
        when(tenantJoinService.registerWithSlug(anyString(), anyString(), anyString(), anyString(),
                any(), eq(SLUG), isNull())).thenReturn(outcome("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"tenantSlug\":\"" + SLUG + "\",")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.churchName").value(CHURCH_NAME));

        // Le court-circuit historique : la porte doit passer par le service de
        // rejointure, seul lieu où la politique de l'église est lue.
        verify(authService, never()).registerInChurch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Église en OPEN : 201 + email d'activation — comportement historique préservé")
    void openChurchStillCreatesAccount() throws Exception {
        when(tenantJoinService.registerWithSlug(anyString(), anyString(), anyString(), anyString(),
                any(), eq(SLUG), isNull())).thenReturn(outcome("ACTIVATION_EMAIL_SENT"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"tenantSlug\":\"" + SLUG + "\",")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBRE"))
                .andExpect(jsonPath("$.churchName").value(CHURCH_NAME))
                .andExpect(jsonPath("$.status").doesNotExist());

        verify(authService, never()).registerInChurch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Variante mobile (tenantId) : même service, même politique")
    void tenantIdVariantAlsoGoesThroughJoinService() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(tenantJoinService.registerWithSlug(anyString(), anyString(), anyString(), anyString(),
                any(), isNull(), eq(tenantId))).thenReturn(outcome("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"tenantId\":\"" + tenantId + "\",")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        verify(authService, never()).registerInChurch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Slug inconnu : 404 sans révéler si une église porte ce nom")
    void unknownSlugIsIndistinguishable() throws Exception {
        when(tenantJoinService.registerWithSlug(anyString(), anyString(), anyString(), anyString(),
                any(), eq("inconnu"), isNull()))
                .thenThrow(new DomainException("Aucune église trouvée pour ce lien",
                        HttpStatus.NOT_FOUND, "JOIN_SLUG_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"tenantSlug\":\"inconnu\",")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Aucune église trouvée pour ce lien"));
    }

    // ---------- non-régression des autres portes ----------

    @Test
    @DisplayName("Inscription classique sans église : demande d'église, Super Admin, unchanged")
    void classicRegistrationUnchanged() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BASE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        verify(tenantJoinService, never()).registerWithSlug(any(), any(), any(), any(), any(), any(), any());
        verify(authService).register(anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("Inscription par code en APPROVAL : 202, unchanged")
    void joinCodeApprovalUnchanged() throws Exception {
        when(tenantJoinService.registerWithCode(anyString(), anyString(), anyString(), anyString(),
                any(), eq("BETHEL-7K2M"))).thenReturn(outcome("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"joinCode\":\"BETHEL-7K2M\",")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        verify(tenantJoinService, never()).registerWithSlug(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createChurch reste exclusif de tenantSlug/tenantId/joinCode")
    void createChurchExclusivityUnchanged() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"createChurch\":true,\"churchName\":\"Bethel\","
                                + "\"tenantSlug\":\"" + SLUG + "\",")))
                .andExpect(status().is4xxClientError());

        verify(selfServiceChurchService, never())
                .createChurch(any(), any(), any(), any(), any(), any(), any(), any());
        verify(tenantJoinService, never()).registerWithSlug(any(), any(), any(), any(), any(), any(), any());
    }
}
