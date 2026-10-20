package com.discipolat.modules.audit.api;

import com.discipolat.modules.audit.domain.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SURFACE {@code /api/v1/permissions} (Phase 9) — la remise en marche, et le
 * repousseur qui reste.
 *
 * <p><strong>Ce test a changé de côté.</strong> Il verrouillait d'abord le retrait
 * de la surface héritée (10 URL en 410 Gone) : son SQL visait {@code role_permissions}
 * dans sa forme pré-V135 et levait SQLSTATE 42703. La consigne projet a été retenue —
 * <em>ne pas supprimer, réparer</em> — et {@code PermissionController},
 * {@code PermissionGuard}, {@code audit.PermissionService} sont de retour, avec un SQL
 * écrit sur le modèle canonique {@code roles} / {@code permissions} /
 * {@code role_permissions(role_id, permission_id)}. Les quatre propriétés ci-dessous
 * sont les mêmes, retournées comme un gant :
 * <ol>
 *   <li><b>les dix URL historiques sont de nouveau servies</b> par le contrôleur
 *   réparé — pas par le repousseur&nbsp;;</li>
 *   <li><b>aucune ambiguïté de mapping</b> entre les deux contrôleurs&nbsp;: les deux
 *   sont enregistrés dans la même stack MVC, et un verbe+chemin déclaré deux fois
 *   empêcherait l'application de démarrer ({@code Ambiguous mapping}) — ce montage-là
 *   est donc le garde de démarrage&nbsp;;</li>
 *   <li><b>le repousseur reste borné</b> : il répond 404 sur les sous-chemins que
 *   personne ne sert, et ne mange ni {@code /api/v1/admin/roles/**} ni
 *   {@code /api/v1/me/permissions}. Un {@code 410} serait devenu un mensonge — ces
 *   chemins n'ont pas été retirés, ils n'ont jamais existé&nbsp;;</li>
 *   <li><b>les chemins promis au client existent</b> : chaque valeur réellement émise
 *   dans {@code replacements} est retrouvée dans les annotations des contrôleurs en
 *   service. Un payload ne se vérifie pas en le relisant.</li>
 * </ol>
 *
 * <p>Le garde d'autorisation {@code @authz.can('AUDIT_EXECUTE', …)} n'est pas exercé
 * ici : {@code standaloneSetup} ne monte pas la méthode-security. Il est verrouillé
 * par {@code PermissionKeyCatalogGuardTest}, qui lit les littéraux {@code @authz.can}
 * dans la source et exige leur présence au catalogue — une faute de frappe sur la clé
 * rougit donc cette suite-là.
 */
class RetiredPermissionSurfaceSunsetTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CANONICAL = "/api/v1/admin/roles";
    private static final String TITRE_REPUSSEUR = "Chemin inconnu sous la surface de permissions";

    /** Racine des sources, lue sur disque comme le fait PermissionKeyCatalogGuardTest. */
    private static final Path MODULES =
            Path.of("src", "main", "java", "com", "discipolat", "modules");

    private PermissionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PermissionService.class);
        lenient().when(service.getAllPermissions()).thenReturn(List.of());
        lenient().when(service.getPermissionsByRole(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
        lenient().when(service.listPermissionCatalog()).thenReturn(List.of());
        lenient().when(service.listRoles()).thenReturn(List.of());
        lenient().when(service.updatePermission(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(Map.of("enabled", true));
        lenient().when(service.updatePermissionRWD(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(Map.of("enabled", true));

        // Les DEUX contrôleurs dans la même stack : c'est le montage qui ferait échouer
        // la démarrage si un verbe+chemin était cartographié deux fois.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PermissionController(service), new RetiredPermissionSurfaceController())
                .build();
    }

    // ==================== 1. les dix URL sont servies par la surface réparée ====================

    @Test
    @DisplayName("Les 10 URL historiques répondent de nouveau, et ce n'est pas le repousseur qui parle")
    void lesDixUrlHeredeesSontServies() throws Exception {
        List<RequestBuilder> appels = List.of(
                get("/api/v1/permissions"),
                get("/api/v1/permissions/ADMIN"),
                put("/api/v1/permissions/ADMIN/USER_CREATE").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"),
                put("/api/v1/permissions/ADMIN/USER_CREATE/rwd").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canRead\":true,\"canWrite\":true,\"canDelete\":false}"),
                get("/api/v1/permissions/catalog"),
                get("/api/v1/permissions/roles"),
                post("/api/v1/permissions/roles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"diacre\",\"label\":\"Diacre\"}"),
                put("/api/v1/permissions/roles/diacre").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Diacre\"}"),
                delete("/api/v1/permissions/roles/diacre"),
                post("/api/v1/permissions/roles/duplicate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceKey\":\"Membre\",\"newKey\":\"diacre_bis\"}"));

        for (RequestBuilder appel : appels) {
            var reponse = mockMvc.perform(appel).andReturn();
            int statut = reponse.getResponse().getStatus();
            assertThat(statut)
                    .as("%s %s doit être servi par PermissionController, pas %s",
                            reponse.getRequest().getMethod(), reponse.getRequest().getRequestURI(),
                            reponse.getResponse().getContentAsString())
                    .isNotEqualTo(404);
            assertThat(reponse.getResponse().getContentAsString())
                    .as("le repousseur ne doit pas répondre sur une URL en service")
                    .doesNotContain("retired-permission-surface");
        }

        // La matrice, le catalogue et les roles sont bien lus aupres du service.
        mockMvc.perform(get("/api/v1/permissions")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        org.mockito.Mockito.verify(service, org.mockito.Mockito.atLeastOnce()).getAllPermissions();
        org.mockito.Mockito.verify(service).listPermissionCatalog();
        org.mockito.Mockito.verify(service).listRoles();
        org.mockito.Mockito.verify(service).createRole("diacre", "Diacre", "");
        org.mockito.Mockito.verify(service).duplicateRole("Membre", "diacre_bis", "diacre_bis");
    }

    // ==================== 2. le repousseur : les sous-chemins que personne ne sert ====================

    @Test
    @DisplayName("Un sous-chemin que la surface n'a jamais servi répond 404 problem+json, tout verbe confondu")
    void lesSousCheminsNonServisRepondent404() throws Exception {
        List<RequestBuilder> appels = List.of(
                get("/api/v1/permissions/ADMIN/USER_CREATE/EXTRA"),
                get("/api/v1/permissions/catalog/introuvable"),
                post("/api/v1/permissions/ADMIN"),
                put("/api/v1/permissions/roles/diacre/permissions"),
                delete("/api/v1/permissions/roles/diacre/permissions"),
                patch("/api/v1/permissions/ADMIN/USER_CREATE"));

        for (RequestBuilder appel : appels) {
            mockMvc.perform(appel)
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value(TITRE_REPUSSEUR))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value(Matchers.containsString("/api/v1/admin/roles")))
                    .andExpect(jsonPath("$.replacements").isArray());
        }
    }

    @Test
    @DisplayName("Le corps cite le chemin appelé et la cible canonique")
    void lePayloadExpliqueEtCiteLeChemin() throws Exception {
        mockMvc.perform(get("/api/v1/permissions/catalog/introuvable"))
                .andExpect(jsonPath("$.legacyPath").value("/api/v1/permissions/catalog/introuvable"))
                .andExpect(jsonPath("$.detail").value(Matchers.allOf(
                        Matchers.containsString("/api/v1/permissions"),
                        Matchers.containsString("/api/v1/admin/roles"))))
                .andExpect(jsonPath("$.replacements[0]").value("GET " + CANONICAL + "/permissions"))
                .andExpect(jsonPath("$.type").value(
                        "https://discipolat.com/api/errors/retired-permission-surface"));
    }

    // ==================== 3. contrôle négatif : le préfixe est borné ====================

    @Test
    @DisplayName("CONTRÔLE NÉGATIF — le catch-all ne mange ni /admin/roles/**, ni /me/permissions")
    void laSurfaceCanoniqueNestPasCaptures() throws Exception {
        for (String chemin : List.of("/api/v1/admin/roles/permissions",
                "/api/v1/admin/roles/for-editor", "/api/v1/me/permissions")) {
            String corps = mockMvc.perform(get(chemin)).andReturn().getResponse().getContentAsString();
            assertThat(corps)
                    .as("%s ne doit pas être servi par le repousseur de /permissions", chemin)
                    .doesNotContain("retired-permission-surface")
                    .doesNotContain(TITRE_REPUSSEUR);
        }
    }

    // ==================== 4. les promesses du payload sont tenues ====================

    @Test
    @DisplayName("Chaque « replacement » réellement émis est un endpoint déclaré")
    void chaquePromesseEstUnVraiEndpoint() throws Exception {
        Set<String> emis = new LinkedHashSet<>();
        for (String chemin : List.of("/catalog/introuvable", "/roles/diacre/permissions",
                "/ADMIN/USER_CREATE/EXTRA")) {
            String corps = mockMvc.perform(get("/api/v1/permissions" + chemin))
                    .andReturn().getResponse().getContentAsString();
            MAPPER.readTree(corps).get("replacements").forEach(n -> emis.add(n.asText()));
        }
        assertThat(emis).as("le repousseur doit orienter vers au moins un endpoint").isNotEmpty();

        String roleManagement = source("platform/api/RoleManagementController.java");
        String tenantAdmin = source("platform/api/TenantAdminController.java");
        for (String verbeEtChemin : emis) {
            assertDeclared(verbeEtChemin, roleManagement, tenantAdmin);
        }
    }

    private void assertDeclared(String verbeEtChemin, String roleManagement, String tenantAdmin) {
        // Le repousseur peut adosser un indice de corps après deux espaces :
        // « PUT /api/v1/admin/roles/{id}/permissions  {permissionKeys:[…]} ».
        String spec = verbeEtChemin.split("  ")[0];
        String verb = spec.substring(0, spec.indexOf(' '));
        assertThat(spec).as("format « VERBE /chemin » attendu, lu : %s", verbeEtChemin)
                .startsWith(verb + " " + CANONICAL);
        // Le chemin commence après le verbe : le décaler d'office de la longueur
        // de CANONICAL aurait découpé « roles » en « oles » et fait chercher une
        // annotation inexistante (faute vue a l'execution de ce garde).
        String tail = spec.substring(spec.indexOf(' ') + 1).substring(CANONICAL.length());

        Pattern attendu;
        String cible;
        if (tail.isEmpty() && "GET".equals(verb)) {
            // la projection éditeur est rendue par TenantAdminController
            attendu = Pattern.compile("@GetMapping\\(\"/roles\"\\)");
            cible = tenantAdmin;
        } else if (tail.isEmpty()) {
            // création de rôle : @PostMapping sans path, dans RoleManagementController
            attendu = Pattern.compile("@" + abbreger(verb) + "Mapping(?![\\w])(\\s|\\()");
            cible = roleManagement;
        } else {
            attendu = Pattern.compile("@" + abbreger(verb) + "Mapping\\(\"" + Pattern.quote(tail) + "\"\\)");
            cible = roleManagement;
        }
        assertThat(cible)
                .as("%s doit correspondre à une annotation de mapping réelle (%s)",
                        verbeEtChemin, attendu)
                .containsPattern(attendu);
    }

    private static String abbreger(String verb) {
        return switch (verb) {
            case "GET" -> "Get";
            case "POST" -> "Post";
            case "PUT" -> "Put";
            case "DELETE" -> "Delete";
            default -> throw new IllegalArgumentException("verbe inattendu : " + verb);
        };
    }

    /**
     * Lecture sur disque plutôt qu'un fixture recopié : un renommage de route doit
     * faire rougir ce garde, pas le laisser promener les clients vers un 404.
     */
    private static String source(String cheminRelatifSousModules) {
        Path candidat = MODULES.resolve(cheminRelatifSousModules);
        assertThat(candidat).as("source de référence pour les mappings").exists();
        try {
            return Files.readString(candidat);
        } catch (IOException e) {
            throw new IllegalStateException("lecture impossible : " + candidat, e);
        }
    }
}
