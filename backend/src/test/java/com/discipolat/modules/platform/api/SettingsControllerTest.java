package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.platform.domain.ChurchSettings;
import com.discipolat.modules.platform.domain.ChurchSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de régression API du {@link SettingsController} (identité & marque).
 *
 * Exerce la chaîne de sécurité RÉELLE (@WebMvcTest + SecurityConfig +
 * JwtTokenProvider réel) : publication publique, lecture authentifiée,
 * mise à jour ADMIN (avec validation bean) et réinitialisation.
 */
@WebMvcTest(SettingsController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ChurchSettingsService settingsService;

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "admin@discipolat.com";

    private ChurchSettings sample;

    @BeforeEach
    void setUp() {
        sample = ChurchSettings.builder()
                .id(UUID.randomUUID())
                .churchName("Église de la Grâce")
                .platformName("Discipolat")
                .slogan("Former des disciples")
                .description("Une église qui forme des disciples.")
                .logoUrl("https://cdn.eglise.org/logo.png")
                .faviconUrl("https://cdn.eglise.org/favicon.ico")
                .bannerUrl("https://cdn.eglise.org/banniere.png")
                .primaryColor("#0f766e")
                .accentColor("#f59e0b")
                .buttonColor("#16a34a")
                .fontFamily("Inter")
                .allowDarkMode(true)
                .address("Abidjan, Cocody")
                .phone("+2250707070707")
                .email("contact@eglise.org")
                .website("https://eglise.org")
                .socialLinks(Map.of("facebook", "https://facebook.com/eglise"))
                .contactNotes("Notes internes (ne doit pas être public)")
                .build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false);
    }

    // ======================== Publication publique ========================

    @Test
    @DisplayName("GET /public/settings sans token → 200 avec uniquement les champs publics")
    void publicBranding_sansToken_200_champsPublicsSeulement() throws Exception {
        when(settingsService.getSettings()).thenReturn(sample);

        mockMvc.perform(get("/api/v1/public/settings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.churchName").value("Église de la Grâce"))
                .andExpect(jsonPath("$.primaryColor").value("#0f766e"))
                .andExpect(jsonPath("$.socialLinks.facebook").value("https://facebook.com/eglise"))
                // La vue publique ne doit exposer ni l'id interne ni les notes de contact
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.contactNotes").doesNotExist());
    }

    @Test
    @DisplayName("GET /public/settings reste public même avec un token présent")
    void publicBranding_avecToken_200() throws Exception {
        when(settingsService.getSettings()).thenReturn(sample);

        mockMvc.perform(get("/api/v1/public/settings")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.churchName").value("Église de la Grâce"));
    }

    // ======================== Lecture authentifiée ========================

    @Test
    @DisplayName("GET /settings authentifié (non-ADMIN) → 200 vue complète (id + notes inclus)")
    void get_authentifie_200_vueComplete() throws Exception {
        when(settingsService.getSettings()).thenReturn(sample);

        // Un simple FAISEUR suffit : la règle est isAuthenticated(), pas hasRole('ADMIN')
        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.churchName").value("Église de la Grâce"))
                .andExpect(jsonPath("$.contactNotes").value("Notes internes (ne doit pas être public)"));
    }

    @Test
    @DisplayName("GET /settings sans token → 401")
    void get_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isUnauthorized());
    }

    // ======================== Mise à jour (ADMIN) ========================

    @Test
    @DisplayName("PUT /settings par ADMIN → 200 et délégation au service avec le corps reçu")
    void update_parAdmin_200_etDelegueAuService() throws Exception {
        when(settingsService.update(any(UpdateChurchSettingsRequest.class))).thenReturn(sample);

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"churchName":"Nouveau Nom","primaryColor":"#0f766e","allowDarkMode":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.churchName").value("Église de la Grâce"));

        verify(settingsService).update(argThat(req ->
                "Nouveau Nom".equals(req.churchName())
                        && "#0f766e".equals(req.primaryColor())
                        && Boolean.FALSE.equals(req.allowDarkMode())));
    }

    @Test
    @DisplayName("PUT /settings par non-ADMIN (FAISEUR) → 403 et aucune modification")
    void update_parNonAdmin_403() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", bearer("FAISEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"churchName\":\"Piraté\"}"))
                .andExpect(status().isForbidden());

        // Preuve que le blocage a lieu à la couche @PreAuthorize, avant le service
        verify(settingsService, never()).update(any(UpdateChurchSettingsRequest.class));
    }

    @Test
    @DisplayName("PUT /settings couleur invalide → 400 (validation bean hexadécimale)")
    void update_couleurInvalide_400() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryColor\":\"rouge\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buttonColor\":\"#12\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /settings sans token → 401")
    void update_sansToken_401() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"churchName\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ======================== Réinitialisation (ADMIN) ========================

    @Test
    @DisplayName("POST /settings/reset par ADMIN → 200 avec les valeurs par défaut")
    void reset_parAdmin_200() throws Exception {
        ChurchSettings defaults = ChurchSettings.builder()
                .id(UUID.randomUUID()).churchName("Discipolat").platformName("Discipolat")
                .primaryColor(ChurchSettings.DEFAULT_PRIMARY_COLOR)
                .accentColor(ChurchSettings.DEFAULT_ACCENT_COLOR)
                .buttonColor(ChurchSettings.DEFAULT_BUTTON_COLOR)
                .fontFamily(ChurchSettings.DEFAULT_FONT_FAMILY)
                .allowDarkMode(true)
                .socialLinks(Map.of()).build();
        when(settingsService.resetToDefaults()).thenReturn(defaults);

        mockMvc.perform(post("/api/v1/settings/reset")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.churchName").value("Discipolat"))
                .andExpect(jsonPath("$.primaryColor").value("#16a34a"));
    }

    @Test
    @DisplayName("POST /settings/reset par non-ADMIN → 403")
    void reset_parNonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/settings/reset")
                        .header("Authorization", bearer("CHEF_DE_FAMILLE")))
                .andExpect(status().isForbidden());

        verify(settingsService, never()).resetToDefaults();
    }
}
