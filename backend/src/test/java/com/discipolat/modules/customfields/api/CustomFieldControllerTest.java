package com.discipolat.modules.customfields.api;

import com.discipolat.common.infrastructure.config.SecurityConfig;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestJwtConfig;
import com.discipolat.modules.customfields.domain.CustomFieldDefinition;
import com.discipolat.modules.customfields.domain.CustomFieldService;
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
 * Tests de régression API du {@link CustomFieldController} (définitions de
 * champs personnalisés et valeurs par entité).
 *
 * Exerce la chaîne de sécurité RÉELLE : lecture des définitions actives pour
 * tout utilisateur authentifié, administration des définitions réservée à
 * l'ADMIN.
 */
@WebMvcTest(CustomFieldController.class)
@Import({SecurityConfig.class, TestJwtConfig.class})
class CustomFieldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomFieldService service;

    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String EMAIL = "admin@discipolat.com";

    private final UUID definitionId = UUID.randomUUID();
    private final UUID entiteId = UUID.randomUUID();

    private CustomFieldDefinition def;

    @BeforeEach
    void setUp() {
        def = CustomFieldDefinition.builder()
                .id(definitionId).entiteType("SOUL").code("LANGUE").label("Langue").type("TEXTE")
                .obligatoire(false).ordre(1).placeholder("Ex : français")
                .rolesLecture(List.of("ADMIN", "PASTEUR")).rolesEcriture(List.of())
                .actif(true).build();
    }

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER_ID, EMAIL, role, Set.of(role), false, null);
    }

    // ======================== Définitions (lecture authentifiée) ========================

    @Test
    @DisplayName("GET /custom-fields/definitions?entiteType= authentifié → 200 filtré par rôle")
    void getDefinitions_authentifie_200() throws Exception {
        when(service.getDefinitions("SOUL")).thenReturn(List.of(def));

        mockMvc.perform(get("/api/v1/custom-fields/definitions")
                        .header("Authorization", bearer("FAISEUR"))
                        .param("entiteType", "SOUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LANGUE"))
                .andExpect(jsonPath("$[0].type").value("TEXTE"))
                .andExpect(jsonPath("$[0].rolesLecture[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /custom-fields/definitions sans entiteType → 400 (paramètre requis)")
    void getDefinitions_sansEntiteType_400() throws Exception {
        mockMvc.perform(get("/api/v1/custom-fields/definitions")
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /custom-fields/definitions sans token → 401")
    void getDefinitions_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/custom-fields/definitions")
                        .param("entiteType", "SOUL"))
                .andExpect(status().isUnauthorized());
    }

    // ======================== Administration des définitions (ADMIN) ========================

    @Test
    @DisplayName("GET /custom-fields/definitions/all par ADMIN → 200 même pour les champs inactifs")
    void getAllDefinitions_admin_200() throws Exception {
        when(service.getAllDefinitions("SOUL")).thenReturn(List.of(def));

        mockMvc.perform(get("/api/v1/custom-fields/definitions/all")
                        .header("Authorization", bearer("ADMIN"))
                        .param("entiteType", "SOUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LANGUE"));
    }

    @Test
    @DisplayName("GET /custom-fields/definitions/all par PASTEUR → 200 (admin ouvert au pasteur)")
    void getAllDefinitions_parPasteur_200() throws Exception {
        when(service.getAllDefinitions("SOUL")).thenReturn(List.of(def));

        mockMvc.perform(get("/api/v1/custom-fields/definitions/all")
                        .header("Authorization", bearer("PASTEUR"))
                        .param("entiteType", "SOUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LANGUE"));

        verify(service).getAllDefinitions("SOUL");
    }

    @Test
    @DisplayName("POST /custom-fields/definitions par ADMIN → 201 et délégation au service")
    void createDefinition_admin_201() throws Exception {
        when(service.createDefinition(any(CustomFieldDefinition.class))).thenReturn(def);

        mockMvc.perform(post("/api/v1/custom-fields/definitions")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entiteType":"SOUL","code":"LANGUE","label":"Langue","type":"TEXTE",
                                 "obligatoire":false,"ordre":1,"placeholder":"Ex : français",
                                 "rolesLecture":["ADMIN","PASTEUR"],"rolesEcriture":[],"actif":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LANGUE"));

        verify(service).createDefinition(argThat(d ->
                "SOUL".equals(d.getEntiteType())
                        && "LANGUE".equals(d.getCode())
                        && "TEXTE".equals(d.getType())));
    }

    @Test
    @DisplayName("POST /custom-fields/definitions par non-ADMIN → 403")
    void createDefinition_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/custom-fields/definitions")
                        .header("Authorization", bearer("CHEF_DE_FAMILLE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entiteType":"SOUL","code":"X","label":"X","type":"TEXTE"}
                                """))
                .andExpect(status().isForbidden());

        verify(service, never()).createDefinition(any(CustomFieldDefinition.class));
    }

    @Test
    @DisplayName("PUT /custom-fields/definitions/{id} par ADMIN → 200 avec la définition mise à jour")
    void updateDefinition_admin_200() throws Exception {
        when(service.updateDefinition(eq(definitionId), any(CustomFieldDefinition.class))).thenReturn(def);

        mockMvc.perform(put("/api/v1/custom-fields/definitions/" + definitionId)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entiteType":"SOUL","code":"LANGUE","label":"Langue parlée","type":"TEXTE",
                                 "obligatoire":true,"actif":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LANGUE"));

        verify(service).updateDefinition(eq(definitionId), argThat(d ->
                "Langue parlée".equals(d.getLabel()) && d.isObligatoire()));
    }

    @Test
    @DisplayName("DELETE /custom-fields/definitions/{id} par ADMIN → 204")
    void deleteDefinition_admin_204() throws Exception {
        mockMvc.perform(delete("/api/v1/custom-fields/definitions/" + definitionId)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        verify(service).deleteDefinition(definitionId);
    }

    // ======================== Valeurs par entité (authentifié) ========================

    @Test
    @DisplayName("GET /custom-fields/{entiteType}/{entiteId} authentifié → 200 avec le bundle")
    void getBundle_authentifie_200() throws Exception {
        when(service.getBundle("SOUL", entiteId)).thenReturn(Map.of("definitions", List.of()));

        mockMvc.perform(get("/api/v1/custom-fields/SOUL/" + entiteId)
                        .header("Authorization", bearer("FAISEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.definitions").isArray());
    }

    @Test
    @DisplayName("GET /custom-fields/{entiteType}/{entiteId} sans token → 401")
    void getBundle_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/custom-fields/SOUL/" + entiteId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /custom-fields/{entiteType}/{entiteId} authentifié → 200 et sauvegarde des valeurs")
    void saveValues_authentifie_200() throws Exception {
        mockMvc.perform(put("/api/v1/custom-fields/SOUL/" + entiteId)
                        .header("Authorization", bearer("FAISEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"%s\":\"français\"}}".formatted(definitionId)))
                .andExpect(status().isOk());

        verify(service).saveValues(eq("SOUL"), eq(entiteId), argThat(values ->
                "français".equals(values.get(definitionId.toString()))));
    }
}
