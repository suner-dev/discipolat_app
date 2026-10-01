package com.discipolat.modules.authentication.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.infrastructure.config.RateLimitResult;
import com.discipolat.modules.authentication.config.SocialAuthProperties;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.authentication.domain.SocialIdentityService;
import com.discipolat.modules.authentication.domain.SocialIdentityVerifier;
import com.discipolat.modules.authentication.domain.SocialProvider;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserStatus;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat HTTP de la connexion par identité externe.
 *
 * <p>Points vérifiés : la réponse est identique à celle de {@code /auth/login},
 * un fournisseur non configuré ne doit pas laisser croire qu'il fonctionne, un
 * fournisseur inconnu n'est pas executed, et chaque endpoint public est borné.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialAuthControllerTest {

    @Mock private AuthService authService;
    @Mock private SocialIdentityService socialIdentityService;
    @Mock private AuthResponseFactory authResponseFactory;
    @Mock private PerIpRateLimiter rateLimiter;

    private MockMvc mockMvc;
    private SocialAuthProperties properties;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new SocialAuthProperties();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SocialAuthController(
                        authService, socialIdentityService, properties, authResponseFactory, rateLimiter))
                // Le advice réel : on teste le comportement de production, pas une
                // version qui laisserait remonter les exceptions en 500.
                .setControllerAdvice(new com.discipolat.common.infrastructure.api.GlobalExceptionHandler())
                .build();
    }

    private void allowRequests() {
        when(rateLimiter.tryConsumeSocialLogin(anyString())).thenReturn(RateLimitResult.allowed(9));
        when(rateLimiter.tryConsumeSocialLink(anyString())).thenReturn(RateLimitResult.allowed(4));
    }

    /** Simule une session authentifiée : les routes /link exigent un utilisateur. */
    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, "jwt-token",
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
    }

    private void configureGoogle() {
        SocialAuthProperties.Google google = new SocialAuthProperties.Google();
        google.setEnabled(true);
        google.setWebClientId("web-client.apps.googleusercontent.com");
        properties.setGoogle(google);
    }

    // ==================================================================
    @Test
    @DisplayName("GET /social/providers : seul un fournisseur réellement configuré est annoncé")
    void listsOnlyConfiguredProviders() throws Exception {
        configureGoogle();

        mockMvc.perform(get("/api/v1/auth/social/providers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("google")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("microsoft"))))
                .andExpect(jsonPath("$.accountLinkingEnabled").value(true));
    }

    @Test
    @DisplayName("GET /social/providers : aucun fournisseur configuré → liste vide, aucun bouton possible")
    void listsNoProviderWhenNoneConfigured() throws Exception {
        mockMvc.perform(get("/api/v1/auth/social/providers"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"providers\":[],\"accountLinkingEnabled\":true}"));
    }

    // ==================================================================
    @Test
    @DisplayName("POST /social/google : renvoie la même charge utile que /auth/login")
    void socialLoginReturnsStandardAuthResponse() throws Exception {
        configureGoogle();
        allowRequests();
        User user = User.builder().id(USER_ID).email("paul@exemple.com")
                .tenantId(UUID.randomUUID()).role(UserRole.MEMBRE).statut(UserStatus.ACTIVE).build();
        when(socialIdentityService.login(eq(SocialProvider.GOOGLE), eq("credential")))
                .thenReturn(new SocialIdentityService.SocialLoginResult(
                        new AuthService.AuthResult("access", "refresh", user, "MEMBRE"),
                        SocialProvider.GOOGLE));
        when(authResponseFactory.from(any())).thenReturn(new AuthResponse(
                "access", "refresh", "Bearer", USER_ID, "paul@exemple.com", "MEMBRE",
                List.of("MEMBRE"), "MEMBRE", false, "Paul", "Koffi", false,
                List.of(), false));

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-RateLimit-Remaining", "9"))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("paul@exemple.com"))
                .andExpect(jsonPath("$.activeRole").value("MEMBRE"));
    }

    @Test
    @DisplayName("POST /social/{provider} : un fournisseur inconnu est refusé sans le vérifier")
    void unknownProviderIsRejectedBeforeVerification() throws Exception {
        configureGoogle();
        allowRequests();

        mockMvc.perform(post("/api/v1/auth/social/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("SOCIAL_CREDENTIAL_INVALID"));

        verify(socialIdentityService, never()).login(any(), anyString());
    }

    @Test
    @DisplayName("POST /social/{provider} : credential absent → 400 avant toute vérification")
    void missingCredentialIsRejected() throws Exception {
        configureGoogle();
        allowRequests();

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("SOCIAL_CREDENTIAL_INVALID"));

        verify(socialIdentityService, never()).login(any(), anyString());
    }

    @Test
    @DisplayName("POST /social/{provider} : quota dépassé → 429 avec Retry-After")
    void socialLoginIsRateLimited() throws Exception {
        configureGoogle();
        when(rateLimiter.tryConsumeSocialLogin(anyString())).thenReturn(RateLimitResult.denied(2_000_000_000L));

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        verify(socialIdentityService, never()).login(any(), anyString());
    }

    @Test
    @DisplayName("POST /social/{provider} : compte inconnu → 403 SOCIAL_ACCOUNT_NOT_LINKED, aucun compte créé")
    void unknownAccountIsRefusedWithActionableCode() throws Exception {
        configureGoogle();
        allowRequests();
        when(socialIdentityService.login(eq(SocialProvider.GOOGLE), eq("credential")))
                .thenThrow(new DomainException(
                        "Aucun compte Discipolat pour cette adresse.", HttpStatus.FORBIDDEN,
                        "SOCIAL_ACCOUNT_NOT_LINKED"));

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("SOCIAL_ACCOUNT_NOT_LINKED"));
    }

    @Test
    @DisplayName("503 si le fournisseur est activé mais non configuré : fail-closed explicite")
    void unconfiguredProviderAnswers503() throws Exception {
        SocialAuthProperties.Google google = new SocialAuthProperties.Google();
        google.setEnabled(true);      // activé sans client id
        properties.setGoogle(google);
        allowRequests();

        when(socialIdentityService.login(any(), anyString()))
                .thenThrow(SocialIdentityVerifier.SocialCredentialException.notConfigured("google"));

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("SOCIAL_PROVIDER_NOT_CONFIGURED"));
    }

    // ==================================================================
    @Test
    @DisplayName("POST /social/link : le fournisseur est transmis et le résultat rendu")
    void linkIdentityDelegatesToService() throws Exception {
        configureGoogle();
        allowRequests();
        authenticate();
        when(socialIdentityService.linkToCurrentUser(any(), eq(SocialProvider.MICROSOFT), eq("credential"), eq(true)))
                .thenReturn(new SocialIdentityService.SocialLinkResult(SocialProvider.MICROSOFT, true));

        mockMvc.perform(post("/api/v1/auth/social/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"microsoft\",\"credential\":\"credential\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("microsoft"))
                .andExpect(jsonPath("$.newlyLinked").value(true));
    }

    @Test
    @DisplayName("POST /social/link : email différent du compte connecté → 403")
    void linkRefusedOnEmailMismatch() throws Exception {
        configureGoogle();
        allowRequests();
        authenticate();
        when(socialIdentityService.linkToCurrentUser(any(), any(), anyString(), anyBoolean()))
                .thenThrow(new DomainException(
                        "L'adresse de ce compte externe ne correspond pas a votre compte Discipolat",
                        HttpStatus.FORBIDDEN, "SOCIAL_EMAIL_MISMATCH"));

        mockMvc.perform(post("/api/v1/auth/social/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"google\",\"credential\":\"credential\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("SOCIAL_EMAIL_MISMATCH"));
    }

    private static boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }

    // ==================================================================
    @Test
    @DisplayName("Compatibilité : /auth/google répond toujours, et ne crée plus de compte")
    void legacyGoogleEndpointStillWorks() throws Exception {
        configureGoogle();
        allowRequests();
        User user = User.builder().id(USER_ID).email("paul@exemple.com")
                .tenantId(UUID.randomUUID()).role(UserRole.MEMBRE).statut(UserStatus.ACTIVE).build();
        when(socialIdentityService.login(eq(SocialProvider.GOOGLE), eq("credential")))
                .thenReturn(new SocialIdentityService.SocialLoginResult(
                        new AuthService.AuthResult("access", "refresh", user, "MEMBRE"),
                        SocialProvider.GOOGLE));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"credential\"}"))
                .andExpect(status().isOk())
                // Ancien format conservé : token / refreshToken / user
                .andExpect(jsonPath("$.token").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.user.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.user.email").value("paul@exemple.com"))
                .andExpect(jsonPath("$.user.role").value("MEMBRE"));
    }
}
