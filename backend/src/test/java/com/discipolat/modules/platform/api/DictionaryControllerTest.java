package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.platform.domain.DictionaryEntry;
import com.discipolat.modules.platform.domain.DictionaryService;
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
import static org.mockito.ArgumentMatchers.argThat;
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
 * Tests de régression API du {@link DictionaryController} (référentiels
 * configurables : types d'événements, statuts, raisons d'absence…).
 *
 * Exerce la chaîne de sécurité RÉELLE : lecture authentifiée pour tous les
 * rôles, administration (création/modification/suppression/reset) réservée
 * à l'ADMIN.
 */
@WebMvcTest(DictionaryController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class DictionaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private DictionaryService dictionaryService;

    @MockBean
    private SecurityUtils securityUtils;

    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String EMAIL = "admin@discipolat.com";

    private DictionaryEntry entry;

    @BeforeEach
    void setUp() {
        entry = DictionaryEntry.builder()
                .id(UUID.randomUUID())
                .dictKey("EVENT_TYPE")
                .code("CULTE")
                .label("Culte")
                .color("#22c55e")
                .ordre(1)
                .actif(true)
                .isDefault(true)
                .build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false, null);
    }

    // ======================== Lecture (application) ========================

    @Test
    @DisplayName("GET /dictionaries authentifié (non-ADMIN) → 200 avec les entrées actives groupées")
    void activeDictionaries_authentifie_200() throws Exception {
        when(dictionaryService.activeGrouped())
                .thenReturn(Map.of("EVENT_TYPE", List.of(entry)));

        mockMvc.perform(get("/api/v1/dictionaries")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.EVENT_TYPE[0].code").value("CULTE"))
                .andExpect(jsonPath("$.EVENT_TYPE[0].label").value("Culte"))
                .andExpect(jsonPath("$.EVENT_TYPE[0].actif").value(true));
    }

    @Test
    @DisplayName("GET /dictionaries sans token → 401")
    void activeDictionaries_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /dictionaries/{key} authentifié → 200 avec les entrées actives du dictionnaire")
    void activeByKey_authentifie_200() throws Exception {
        when(dictionaryService.activeByKey("EVENT_TYPE")).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/dictionaries/EVENT_TYPE")
                        .header("Authorization", bearer("RESPONSABLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CULTE"))
                .andExpect(jsonPath("$[0].label").value("Culte"));
    }

    // ======================== Administration (ADMIN) ========================

    @Test
    @DisplayName("GET /admin/dictionaries par ADMIN → 200 avec toutes les entrées")
    void allDictionaries_admin_200() throws Exception {
        when(dictionaryService.allGrouped())
                .thenReturn(Map.of("EVENT_TYPE", List.of(entry)));

        mockMvc.perform(get("/api/v1/admin/dictionaries")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.EVENT_TYPE[0].code").value("CULTE"));
    }

    @Test
    @DisplayName("GET /admin/dictionaries par non-ADMIN → 403")
    void allDictionaries_nonAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dictionaries")
                        .header("Authorization", bearer("RESPONSABLE")))
                .andExpect(status().isForbidden());

        verify(dictionaryService, never()).allGrouped();
    }

    @Test
    @DisplayName("POST /admin/dictionaries/{key} par ADMIN → 201 avec l'entrée créée")
    void create_admin_201() throws Exception {
        DictionaryEntry created = DictionaryEntry.builder()
                .id(UUID.randomUUID()).dictKey("EVENT_TYPE").code("BAPTEME")
                .label("Baptême").color("#3b82f6").ordre(14).actif(true).isDefault(false).build();
        when(dictionaryService.create(eq("EVENT_TYPE"), any(DictionaryEntry.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/dictionaries/EVENT_TYPE")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"BAPTEME","label":"Baptême","color":"#3b82f6","ordre":14,"actif":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BAPTEME"))
                .andExpect(jsonPath("$.label").value("Baptême"));

        verify(dictionaryService).create(eq("EVENT_TYPE"), argThat(e ->
                "BAPTEME".equals(e.getCode()) && "Baptême".equals(e.getLabel())));
    }

    @Test
    @DisplayName("POST /admin/dictionaries/{key} par non-ADMIN → 403")
    void create_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/dictionaries/EVENT_TYPE")
                        .header("Authorization", bearer("FAISEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"BAPTEME","label":"Baptême"}
                                """))
                .andExpect(status().isForbidden());

        verify(dictionaryService, never()).create(any(), any());
    }

    @Test
    @DisplayName("PUT /admin/dictionaries/{id} par ADMIN → 200 avec l'entrée mise à jour")
    void update_admin_200() throws Exception {
        when(dictionaryService.update(any(), any(DictionaryEntry.class))).thenReturn(entry);

        mockMvc.perform(put("/api/v1/admin/dictionaries/" + entry.getId())
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Culte dominical","color":"#16a34a","ordre":2,"actif":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Culte"));

        verify(dictionaryService).update(eq(entry.getId()), any(DictionaryEntry.class));
    }

    @Test
    @DisplayName("DELETE /admin/dictionaries/{id} par ADMIN → 204")
    void delete_admin_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/dictionaries/" + entry.getId())
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(dictionaryService).delete(entry.getId());
    }

    @Test
    @DisplayName("POST /admin/dictionaries/reset par ADMIN → 204 et restauration des défauts")
    void reset_admin_204() throws Exception {
        mockMvc.perform(post("/api/v1/admin/dictionaries/reset")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(dictionaryService).resetDefaults();
    }

    @Test
    @DisplayName("POST /admin/dictionaries/reset par non-ADMIN → 403")
    void reset_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/dictionaries/reset")
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isForbidden());

        verify(dictionaryService, never()).resetDefaults();
    }
}
