package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.platform.domain.CustomPage;
import com.discipolat.modules.platform.domain.PageBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de régression API du {@link PageBuilderController} (pages
 * personnalisées). Exerce la chaîne de sécurité RÉELLE : lecture
 * authentifiée du rendu public, écritures réservées ADMIN.
 */
@WebMvcTest(PageBuilderController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class PageBuilderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PageBuilderService pageBuilderService;

    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "admin@discipolat.com";

    private CustomPage page;

    @BeforeEach
    void setUp() {
        page = CustomPage.builder()
                .id(UUID.randomUUID())
                .key("APERCU")
                .title("Vue d'ensemble")
                .description("Synthèse")
                .slug("apercu-eglise")
                .layout("GRID_2")
                .blocks(List.of(Map.of("type", "KPI", "config", Map.of("label", "Âmes", "source", "SOULS_TOTAL"))))
                .roles(List.of("ADMIN", "PASTEUR"))
                .enabled(true).published(true).version(2)
                .build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false);
    }

    // ======================== Rendu public (authentifié) ========================

    @Test
    @DisplayName("GET /pages/{slug} authentifié → 200 avec la page résolue")
    void render_authentifie_200() throws Exception {
        ResolvedBlock kpi = new ResolvedBlock("KPI",
                Map.of("label", "Âmes", "source", "SOULS_TOTAL"), Map.of("value", 42L));
        ResolvedPage resolved = new ResolvedPage(page, List.of(kpi));
        when(pageBuilderService.resolve("apercu-eglise")).thenReturn(resolved);

        mockMvc.perform(get("/api/v1/pages/apercu-eglise")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.slug").value("apercu-eglise"))
                .andExpect(jsonPath("$.blocks[0].type").value("KPI"))
                .andExpect(jsonPath("$.blocks[0].data.value").value(42));
    }

    @Test
    @DisplayName("GET /pages/{slug} sans token → 401")
    void render_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/pages/apercu-eglise"))
                .andExpect(status().isUnauthorized());

        verify(pageBuilderService, never()).resolve(any());
    }

    // ======================== Administration (ADMIN) ========================

    @Test
    @DisplayName("GET /pages par ADMIN → 200 avec toutes les pages")
    void listPages_admin_200() throws Exception {
        when(pageBuilderService.listAll()).thenReturn(List.of(page));

        mockMvc.perform(get("/api/v1/pages")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("APERCU"))
                .andExpect(jsonPath("$[0].published").value(true));
    }

    @Test
    @DisplayName("GET /pages par non-ADMIN → 403")
    void listPages_nonAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/pages")
                        .header("Authorization", bearer("RESPONSABLE")))
                .andExpect(status().isForbidden());

        verify(pageBuilderService, never()).listAll();
    }

    @Test
    @DisplayName("POST /pages par ADMIN → 201 avec la page créée")
    void createPage_admin_201() throws Exception {
        CustomPage created = CustomPage.builder()
                .id(UUID.randomUUID()).key("PRIERES").title("Prière").slug("priere")
                .layout("STACK").blocks(List.of()).roles(List.of()).enabled(true).published(false).version(1)
                .build();
        when(pageBuilderService.create(any(CustomPage.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/pages")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"prieres","title":"Prière","slug":"priere","layout":"STACK",
                                 "blocks":[],"roles":[],"enabled":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("PRIERES"));

        verify(pageBuilderService).create(any(CustomPage.class));
    }

    @Test
    @DisplayName("POST /pages par non-ADMIN → 403")
    void createPage_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/pages")
                        .header("Authorization", bearer("PASTEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"x\",\"title\":\"X\",\"slug\":\"x\"}"))
                .andExpect(status().isForbidden());

        verify(pageBuilderService, never()).create(any());
    }

    @Test
    @DisplayName("PUT /pages/{id} par ADMIN → 200 avec la page mise à jour")
    void updatePage_admin_200() throws Exception {
        when(pageBuilderService.update(eq(page.getId()), any(CustomPage.class))).thenReturn(page);

        mockMvc.perform(put("/api/v1/pages/" + page.getId())
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"APERCU","title":"Nouveau titre","slug":"apercu-eglise",
                                 "layout":"GRID_2","blocks":[],"roles":["ADMIN"],"enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("apercu-eglise"));
    }

    @Test
    @DisplayName("DELETE /pages/{id} par ADMIN → 204")
    void deletePage_admin_204() throws Exception {
        mockMvc.perform(delete("/api/v1/pages/" + page.getId())
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(pageBuilderService).delete(page.getId());
    }

    @Test
    @DisplayName("POST /pages/{id}/publish par ADMIN → 200 et publication transmise")
    void publishPage_admin_200() throws Exception {
        CustomPage published = CustomPage.builder()
                .id(page.getId()).key("APERCU").title("Vue").slug("apercu-eglise")
                .layout("STACK").blocks(List.of()).roles(List.of())
                .enabled(true).published(false).version(3).build();
        when(pageBuilderService.setPublished(page.getId(), true)).thenReturn(published);

        mockMvc.perform(post("/api/v1/pages/" + page.getId() + "/publish")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));

        verify(pageBuilderService).setPublished(page.getId(), true);
    }

    @Test
    @DisplayName("GET /pages/sources par ADMIN → 200 avec le catalogue")
    void sources_admin_200() throws Exception {
        when(pageBuilderService.sources()).thenReturn(List.of(
                new PageDataSource("SOULS_TOTAL", "Âmes suivies", "KPI", "Total des âmes", false)));

        mockMvc.perform(get("/api/v1/pages/sources")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("SOULS_TOTAL"))
                .andExpect(jsonPath("$[0].type").value("KPI"));
    }

    @Test
    @DisplayName("GET /pages/sources par non-ADMIN → 403")
    void sources_nonAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/pages/sources")
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isForbidden());

        verify(pageBuilderService, never()).sources();
    }

    @Test
    @DisplayName("GET /pages/preview/{id} par ADMIN → 200 avec la page résolue (même non publiée)")
    void preview_admin_200() throws Exception {
        ResolvedPage resolved = new ResolvedPage(page, List.of());
        when(pageBuilderService.resolvePreview(page.getId())).thenReturn(resolved);

        mockMvc.perform(get("/api/v1/pages/preview/" + page.getId())
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.key").value("APERCU"));
    }
}
