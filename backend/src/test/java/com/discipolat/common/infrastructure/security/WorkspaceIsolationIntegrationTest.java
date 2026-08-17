package com.discipolat.common.infrastructure.security;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.modules.dashboard.api.DashboardController;
import com.discipolat.modules.dashboard.domain.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration de l'isolation des espaces métiers au niveau API.
 *
 * Il exerce la chaîne de sécurité RÉELLE : {@link JwtAuthenticationFilter} +
 * {@link SecurityConfig} (avec {@code @EnableMethodSecurity}) + un vrai
 * {@link JwtTokenProvider} (clés RSA générées pour le test) + le contrôleur
 * réel. Le scénario central : un utilisateur MULTI-RÔLES (FAISEUR +
 * RESPONSABLE) dont le rôle ACTIF est FAISEUR doit recevoir 403 sur une API
 * réservée au RESPONSABLE — le filtre n'accorde que les autorités du rôle
 * actif, donc {@code @PreAuthorize} évalue l'espace métier courant.
 */
@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, WorkspaceIsolationIntegrationTest.TestJwtConfig.class})
class WorkspaceIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private DashboardService dashboardService;

    private static final UUID MULTI_ROLE_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "paul.multi@discipolat.com";

    @BeforeEach
    void setUp() {
        // Les endpoints RESPONSABLE/FAISEUR atteints (cas positifs) appellent le service.
        when(dashboardService.getResponsableDashboard(null)).thenReturn(Map.of("ok", true));
        when(dashboardService.getCrmFaiseurDashboard()).thenReturn(Map.of("ok", true));
    }

    private String bearerToken(String activeRole, Set<String> roles) {
        String token = jwtTokenProvider.generateAccessToken(
                MULTI_ROLE_USER_ID, EMAIL, activeRole, roles, false, null);
        return "Bearer " + token;
    }

    // ========================================================================
    // SCÉNARIO CENTRAL : multi-rôles en mode FAISEUR → 403 sur une API RESPONSABLE
    // ========================================================================

    @Test
    @DisplayName("Multi-rôles (FAISEUR+RESPONSABLE) en mode FAISEUR → 403 sur /dashboard/responsable")
    void multiRoleUser_activeRoleFaiseur_recoit403SurApiResponsable() throws Exception {
        // Le même utilisateur possède les deux rôles mais travaille DANS l'espace FAISEUR
        String token = bearerToken("FAISEUR", Set.of("FAISEUR", "RESPONSABLE"));

        mockMvc.perform(get("/api/v1/dashboard/responsable")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Le rôle RESPONSABLE possédé (non actif) ne suffit pas : 403 même avec roles[] complet")
    void activeRoleFaiseur_rolesCompletsNElargissentPasLesAutorites() throws Exception {
        // Preuve que le filtre n'accorde QUE ROLE_FAISEUR malgré le claim roles[]
        // contenant aussi RESPONSABLE — l'isolation vient du filtre, pas du contrôleur.
        String token = bearerToken("FAISEUR", Set.of("FAISEUR", "RESPONSABLE", "CHEF_DE_FAMILLE"));

        mockMvc.perform(get("/api/v1/dashboard/responsable")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());

        // L'espace FAISEUR, lui, reste accessible avec le même token
        mockMvc.perform(get("/api/v1/dashboard/crm-faiseur")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    // ========================================================================
    // CONTRE-ÉPREUVES : le même utilisateur change de rôle actif → l'API s'ouvre
    // ========================================================================

    @Test
    @DisplayName("Même utilisateur en mode RESPONSABLE → 200 sur /dashboard/responsable")
    void multiRoleUser_activeRoleResponsable_accedeALApiResponsable() throws Exception {
        String token = bearerToken("RESPONSABLE", Set.of("FAISEUR", "RESPONSABLE"));

        mockMvc.perform(get("/api/v1/dashboard/responsable")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Multi-rôles en mode FAISEUR → 200 sur /dashboard/crm-faiseur (espace courant)")
    void multiRoleUser_activeRoleFaiseur_accedeAUneApiFaiseur() throws Exception {
        String token = bearerToken("FAISEUR", Set.of("FAISEUR", "RESPONSABLE"));

        mockMvc.perform(get("/api/v1/dashboard/crm-faiseur")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    // ========================================================================
    // SUPER-UTILISATEURS : l'Admin actif reçoit ROLE_PASTEUR en plus (vue complète)
    // ========================================================================

    @Test
    @DisplayName("Admin actif (super-utilisateur) → 200 sur /dashboard/responsable")
    void adminActif_superUser_accedeALApiResponsable() throws Exception {
        // L'Admin actif reçoit ROLE_ADMIN + ROLE_PASTEUR (invariant super-utilisateur)
        String token = bearerToken("ADMIN", Set.of("ADMIN", "PASTEUR"));

        mockMvc.perform(get("/api/v1/dashboard/responsable")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    // ========================================================================
    // AUTHENTIFICATION : pas de token → 401 (et non 403)
    // ========================================================================

    @Test
    @DisplayName("Sans token → 401 sur l'API RESPONSABLE")
    void sansToken_recoit401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/responsable"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token invalide → 401 (signature inconnue du provider)")
    void tokenInvalide_recoit401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/responsable")
                        .header("Authorization", "Bearer token.invalide.signature"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Clé RSA dédiée au test : le {@link JwtTokenProvider} RÉEL valide et signe
     * des tokens (même format que setup-keys.sh : PEM en base64). Sans cette
     * config, le bean ne peut pas être construit (aucune clé JWT fournie).
     */
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            String privDerB64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(pair.getPrivate().getEncoded());
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + privDerB64 + "\n-----END PRIVATE KEY-----";

            String pubDerB64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(pair.getPublic().getEncoded());
            String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + pubDerB64 + "\n-----END PUBLIC KEY-----";

            String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyPem.getBytes(StandardCharsets.UTF_8));
            String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyPem.getBytes(StandardCharsets.UTF_8));

            return new JwtTokenProvider(privateKeyBase64, publicKeyBase64, "", "");
        }
    }
}
