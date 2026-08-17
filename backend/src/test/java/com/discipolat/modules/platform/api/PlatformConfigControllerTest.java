package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.platform.domain.ConfigRevision;
import com.discipolat.modules.platform.domain.ConfigRevisionService;
import com.discipolat.modules.platform.domain.MenuEntry;
import com.discipolat.modules.platform.domain.PlatformConfigService;
import com.discipolat.modules.platform.domain.PlatformModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyBoolean;
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
 * Tests de régression API du {@link PlatformConfigController} (modules
 * activables/désactivables et menus configurables).
 *
 * Exerce la chaîne de sécurité RÉELLE : accès authentifié pour la lecture,
 * réservé ADMIN pour toute écriture (création, modification, suppression,
 * réordonnancement).
 */
@WebMvcTest(PlatformConfigController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class PlatformConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PlatformConfigService platformService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private ConfigRevisionService revisionService;

    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "admin@discipolat.com";

    private MenuEntry menu;
    private PlatformModule module;

    @BeforeEach
    void setUp() {
        menu = MenuEntry.builder()
                .id(UUID.randomUUID()).key("souls").label("Âmes").href("/souls")
                .icon("heart").section("Général").ordre(1)
                .roles(List.of("ADMIN", "PASTEUR")).moduleKey("SOULS").enabled(true).build();
        module = PlatformModule.builder()
                .key("SOULS").label("Âmes").description("Suivi des âmes")
                .icon("heart").section("Général").enabled(true).ordre(1).build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false, null);
    }

    // ======================== Menus (lecture authentifiée) ========================

    @Test
    @DisplayName("GET /platform/menus authentifié (non-ADMIN) → 200 avec les menus du rôle actif")
    void myMenus_authentifie_200() throws Exception {
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR"));
        when(platformService.menusForRoles(List.of("FAISEUR"))).thenReturn(List.of(menu));

        // Un simple FAISEUR suffit : la règle est isAuthenticated(), pas hasRole('ADMIN')
        mockMvc.perform(get("/api/v1/platform/menus")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("souls"))
                .andExpect(jsonPath("$[0].href").value("/souls"))
                .andExpect(jsonPath("$[0].moduleKey").value("SOULS"));
    }

    @Test
    @DisplayName("GET /platform/menus sans token → 401")
    void myMenus_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/platform/menus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /platform/gating authentifié → 200 avec l'état d'activation des modules")
    void gating_authentifie_200() throws Exception {
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR"));
        when(platformService.gateInfo(List.of("FAISEUR")))
                .thenReturn(List.of(new MenuGateInfo("/souls", "SOULS", false)));

        mockMvc.perform(get("/api/v1/platform/gating")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].href").value("/souls"))
                .andExpect(jsonPath("$[0].moduleKey").value("SOULS"))
                .andExpect(jsonPath("$[0].moduleEnabled").value(false));
    }

    // ======================== Administration des menus (ADMIN) ========================

    @Test
    @DisplayName("GET /platform/admin/menus par ADMIN → 200 tous les menus")
    void allMenus_admin_200() throws Exception {
        when(platformService.listAllMenus()).thenReturn(List.of(menu));

        mockMvc.perform(get("/api/v1/platform/admin/menus")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("souls"));
    }

    @Test
    @DisplayName("GET /platform/admin/menus par non-ADMIN → 403")
    void allMenus_nonAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/platform/admin/menus")
                        .header("Authorization", bearer("RESPONSABLE")))
                .andExpect(status().isForbidden());

        verify(platformService, never()).listAllMenus();
    }

    @Test
    @DisplayName("POST /platform/menus par ADMIN → 201 avec le menu créé")
    void createMenu_admin_201() throws Exception {
        MenuEntry created = MenuEntry.builder()
                .id(UUID.randomUUID()).key("temoignages").label("Témoignages").href("/temoignages")
                .icon("chat").section("Vie de l'église").ordre(9)
                .roles(List.of("ADMIN")).moduleKey(null).enabled(true).build();
        when(platformService.createMenu(any(MenuEntry.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/platform/menus")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"temoignages","label":"Témoignages","href":"/temoignages",
                                 "icon":"chat","section":"Vie de l'église","ordre":9,
                                 "roles":["ADMIN"],"enabled":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("temoignages"))
                .andExpect(jsonPath("$.href").value("/temoignages"));

        verify(platformService).createMenu(argThat(m ->
                "temoignages".equals(m.getKey()) && "/temoignages".equals(m.getHref())));
    }

    @Test
    @DisplayName("POST /platform/menus par non-ADMIN → 403")
    void createMenu_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/platform/menus")
                        .header("Authorization", bearer("FAISEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"x","label":"X","href":"/x","section":"Général"}
                                """))
                .andExpect(status().isForbidden());

        verify(platformService, never()).createMenu(any(MenuEntry.class));
    }

    @Test
    @DisplayName("PUT /platform/menus/{id} par ADMIN → 200 avec le menu mis à jour")
    void updateMenu_admin_200() throws Exception {
        when(platformService.updateMenu(any(), any(MenuEntry.class))).thenReturn(menu);

        mockMvc.perform(put("/api/v1/platform/menus/" + menu.getId())
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"souls","label":"Disciples","href":"/souls","section":"Général",
                                 "roles":["ADMIN"],"enabled":false,"ordre":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("souls"));
    }

    @Test
    @DisplayName("DELETE /platform/menus/{id} par ADMIN → 204")
    void deleteMenu_admin_204() throws Exception {
        mockMvc.perform(delete("/api/v1/platform/menus/" + menu.getId())
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(platformService).deleteMenu(menu.getId());
    }

    @Test
    @DisplayName("POST /platform/menus/reorder par ADMIN → 200 avec la nouvelle liste")
    void reorderMenus_admin_200() throws Exception {
        MenuOrderItem item = new MenuOrderItem(menu.getId(), 2, "Suivi");
        when(platformService.reorderMenus(anyList())).thenReturn(List.of(menu));

        mockMvc.perform(post("/api/v1/platform/menus/reorder")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"id":"%s","ordre":2,"section":"Suivi"}]
                                """.formatted(menu.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("souls"));

        verify(platformService).reorderMenus(argThat(items ->
                items.size() == 1 && item.ordre() == items.get(0).ordre()
                        && "Suivi".equals(items.get(0).section())));
    }

    // ======================== Modules (lecture authentifiée) ========================

    @Test
    @DisplayName("GET /platform/modules authentifié (non-ADMIN) → 200 avec l'état de tous les modules")
    void modules_authentifie_200() throws Exception {
        when(platformService.listModules()).thenReturn(List.of(module));

        // Un simple FAISEUR suffit : la règle est isAuthenticated(), pas hasRole('ADMIN')
        mockMvc.perform(get("/api/v1/platform/modules")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("SOULS"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @DisplayName("GET /platform/modules sans token → 401")
    void modules_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/platform/modules"))
                .andExpect(status().isUnauthorized());
    }

    // ======================== Administration des modules (ADMIN) ========================

    @Test
    @DisplayName("PUT /platform/modules/{key} par ADMIN → 200 et bascule d'activation transmise")
    void toggleModule_admin_200() throws Exception {
        PlatformModule disabled = PlatformModule.builder()
                .key("SOULS").label("Âmes").section("Général").enabled(false).ordre(1).build();
        when(platformService.toggleModule("SOULS", false)).thenReturn(disabled);

        mockMvc.perform(put("/api/v1/platform/modules/SOULS")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("SOULS"))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(platformService).toggleModule("SOULS", false);
    }

    @Test
    @DisplayName("PUT /platform/modules/{key} par non-ADMIN → 403")
    void toggleModule_nonAdmin_403() throws Exception {
        mockMvc.perform(put("/api/v1/platform/modules/SOULS")
                        .header("Authorization", bearer("FAISEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());

        verify(platformService, never()).toggleModule(any(), anyBoolean());
    }

    @Test
    @DisplayName("POST /platform/modules par ADMIN → 201 avec le module créé")
    void createModule_admin_201() throws Exception {
        PlatformModule created = PlatformModule.builder()
                .key("TESTIMONIES").label("Témoignages").description("Partage de témoignages")
                .icon("chat").section("Vie de l'église").enabled(true).ordre(5).build();
        when(platformService.createModule(any(PlatformModule.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/platform/modules")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"testimonies","label":"Témoignages","description":"Partage de témoignages",
                                 "icon":"chat","section":"Vie de l'église","enabled":true,"ordre":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("TESTIMONIES"));

        verify(platformService).createModule(argThat(m ->
                "testimonies".equals(m.getKey()) && "Témoignages".equals(m.getLabel())));
    }

    @Test
    @DisplayName("PUT /platform/modules/{key}/edit par ADMIN → 200 avec le module mis à jour")
    void updateModule_admin_200() throws Exception {
        PlatformModule updated = PlatformModule.builder()
                .key("SOULS").label("Disciples").description("Suivi spirituel")
                .icon("heart").section("Suivi").enabled(true).ordre(3).build();
        when(platformService.updateModule(eq("SOULS"), any(PlatformModule.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/platform/modules/SOULS/edit")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"SOULS","label":"Disciples","description":"Suivi spirituel",
                                 "icon":"heart","section":"Suivi","enabled":true,"ordre":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Disciples"))
                .andExpect(jsonPath("$.section").value("Suivi"));
    }

    @Test
    @DisplayName("DELETE /platform/modules/{key} par ADMIN → 204")
    void deleteModule_admin_204() throws Exception {
        mockMvc.perform(delete("/api/v1/platform/modules/SOULS")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(platformService).deleteModule("SOULS");
    }

    // ===================== Versionnage (révisions) =====================

    @Test
    @DisplayName("GET /platform/revisions par ADMIN → 200 avec la page de révisions")
    void revisions_admin_200() throws Exception {
        ConfigRevision revision = ConfigRevision.builder()
                .id(UUID.randomUUID())
                .entityType("PLATFORM_MODULE")
                .entityKey("SOULS")
                .action("MODULE_ENABLED")
                .payload(Map.of("enabled", true))
                .createdAt(java.time.LocalDateTime.of(2026, 8, 17, 10, 0))
                .build();
        Page<ConfigRevision> page = new PageImpl<>(List.of(revision),
                PageRequest.of(0, 20), 1);
        when(revisionService.list(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/platform/revisions")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("MODULE_ENABLED"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(revisionService).list(isNull(), any());
    }

    @Test
    @DisplayName("GET /platform/revisions filtré par entityType → le filtre est transmis")
    void revisions_filtered_200() throws Exception {
        when(revisionService.list(eq("PLATFORM_MENU"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/platform/revisions")
                        .param("entityType", "PLATFORM_MENU")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk());

        verify(revisionService).list(eq("PLATFORM_MENU"), any());
    }

    @Test
    @DisplayName("GET /platform/revisions par non-ADMIN → 403")
    void revisions_nonAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/platform/revisions")
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isForbidden());

        verify(revisionService, never()).list(any(), any());
    }
}
