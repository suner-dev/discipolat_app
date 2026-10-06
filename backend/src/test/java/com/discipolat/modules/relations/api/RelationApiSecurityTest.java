package com.discipolat.modules.relations.api;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestSecurityConfig;
import com.discipolat.modules.relations.api.dto.MemberRelationView;
import com.discipolat.modules.relations.domain.MemberRelation;
import com.discipolat.modules.relations.domain.MemberRelationService;
import com.discipolat.modules.relations.domain.UserHierarchyService;
import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V231 — surface HTTP de « Mon encadrement ».
 *
 * <p>Verrouille ce que les tests unitaires ne peuvent pas voir :
 * <ol>
 *   <li>les <b>gardes d'accès</b> réelles ({@code @PreAuthorize}) sur chaque
 *       endpoint, y compris le refus du membre ordinaire sur les endpoints
 *       d'encadrement ;</li>
 *   <li>l'isolation multi-tenant de bout en bout (le tenant vient du JWT,
 *       jamais d'un paramètre de requête) ;</li>
 *   <li>la <b>forme du JSON</b> published (contrat consommé par le web et
 *       le mobile) ;</li>
 *   <li>la <b>propagation des erreurs métier</b> en codes HTTP cohérents.</li>
 * </ol>
 */
@WebMvcTest(controllers = {RelationController.class, HierarchyController.class})
@Import(TestSecurityConfig.class)
class RelationApiSecurityTest {

    private static final UUID USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String EMAIL = "membre@eglise.org";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private MemberRelationService relationService;
    @MockBean private UserHierarchyService hierarchyService;

    private String bearer(String role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER, EMAIL, role, Set.of(role), false, null);
    }

    private static MemberRelationView view(String statut) {
        return new MemberRelationView(UUID.randomUUID(), OTHER, "Jean Maka", USER, "Awa Diallo",
                OTHER, "Jean Maka", "PASTEUR", "Mon pasteur", statut, null,
                "2026-01-01T10:00:00Z", "REVOKED".equals(statut) ? "2026-02-01T10:00:00Z" : null,
                OTHER, true);
    }

    private static MemberRelationView view() {
        return view("ACTIVE");
    }

    // ======================== TYPES ========================

    @Test
    @DisplayName("GET /relations/types : authentifié → 200, et les types DÉSACTIVÉS sont absents")
    void types_areFilteredByActif() throws Exception {
        when(relationService.typeCatalog(any())).thenReturn(List.of(
                new MemberRelationService.RelationType("PASTEUR", "Mon pasteur", true, 1),
                new MemberRelationService.RelationType("MENTOR", "Mon mentor", false, 4)));

        mockMvc.perform(get("/api/v1/relations/types").header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("PASTEUR"));
    }

    @Test
    @DisplayName("GET /relations/types : anonyme → 401")
    void types_requireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/relations/types")).andExpect(status().isUnauthorized());
    }

    // ======================== MES RELATIONS ========================

    @Test
    @DisplayName("GET /relations/me : un membre ordinaire voit ses rattachements")
    void me_allowedForMember() throws Exception {
        when(relationService.summary(any(), any(), any()))
                .thenReturn(Map.of("sortantes", List.of(view()), "entrantes", List.of()));

        mockMvc.perform(get("/api/v1/relations/me").header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortantes[0].typeLabel").value("Mon pasteur"))
                .andExpect(jsonPath("$.sortantes[0].fromNom").value("Jean Maka"));
    }

    @Test
    @DisplayName("GET /relations/me/members : paginé, la page est bornée")
    void myMembers_isPaged() throws Exception {
        when(relationService.listMembersOf(any(), any(), anyInt(), anyInt()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/relations/me/members")
                        .header("Authorization", bearer("PASTEUR"))
                        .param("page", "2").param("size", "25"))
                .andExpect(status().isOk());
        verify(relationService).listMembersOf(any(), eq(USER), eq(2), eq(25));
    }

    // ======================== DÉCLARATION ========================

    @Test
    @DisplayName("POST /relations/me : déclaration self-service → 201 avec typeLabel")
    void declare_selfService() throws Exception {
        MemberRelation saved = MemberRelation.builder().id(UUID.randomUUID())
                .fromUserId(USER).toUserId(OTHER).relationType("PASTEUR")
                .statut(MemberRelation.RelationStatus.ACTIVE).build();
        when(relationService.declare(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);
        when(relationService.viewOf(any(), any(), any())).thenReturn(view());

        mockMvc.perform(post("/api/v1/relations/me")
                        .header("Authorization", bearer("MEMBRE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toUserId":"%s","relationType":"PASTEUR","note":"depuis 2019"}
                                """.formatted(OTHER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType").value("PASTEUR"))
                .andExpect(jsonPath("$.typeLabel").value("Mon pasteur"));
    }

    @Test
    @DisplayName("POST /relations/me : refus d'auto-rattachement → 400 métier")
    void declare_rejectsSelfLink() throws Exception {
        when(relationService.declare(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException(
                        "Un membre ne peut pas se déclarer son propre encadrant", "RELATION_SELF"));

        mockMvc.perform(post("/api/v1/relations/me")
                        .header("Authorization", bearer("MEMBRE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toUserId":"%s","relationType":"MENTOR"}
                                """.formatted(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /relations/me : type désactivé par l'église → 400 métier")
    void declare_rejectsDisabledType() throws Exception {
        when(relationService.declare(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException(
                        "Le type « Mon mentor » est désactivé dans le paramétrage de cette église",
                        "RELATION_TYPE_DISABLED"));

        mockMvc.perform(post("/api/v1/relations/me")
                        .header("Authorization", bearer("MEMBRE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toUserId":"%s","relationType":"MENTOR"}
                                """.formatted(OTHER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /relations/users/{id} : réservé à ADMIN/PASTEUR — un membre est refusé")
    void declareFor_forbiddenForPlainMember() throws Exception {
        mockMvc.perform(post("/api/v1/relations/users/{userId}", OTHER)
                        .header("Authorization", bearer("MEMBRE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toUserId":"%s","relationType":"PASTEUR"}
                                """.formatted(USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /relations/users/{id} : PASTEUR autorisé (déclarer pour un membre)")
    void declareFor_allowedForPastor() throws Exception {
        MemberRelation saved = MemberRelation.builder().id(UUID.randomUUID())
                .fromUserId(OTHER).toUserId(USER).relationType("PASTEUR")
                .statut(MemberRelation.RelationStatus.ACTIVE).build();
        when(relationService.declare(any(), eq(OTHER), eq(USER), any(), any(), any(), eq(USER)))
                .thenReturn(saved);
        when(relationService.viewOf(any(), any(), any())).thenReturn(view());

        mockMvc.perform(post("/api/v1/relations/users/{userId}", OTHER)
                        .header("Authorization", bearer("PASTEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toUserId":"%s","relationType":"PASTEUR"}
                                """.formatted(USER)))
                .andExpect(status().isCreated());
    }

    // ======================== RÉVOCATION ========================

    @Test
    @DisplayName("DELETE /relations/me/{id} : le déclarant peut retirer (200 + vue)")
    void revoke_allowedForDeclarant() throws Exception {
        MemberRelation revoked = MemberRelation.builder().id(UUID.randomUUID())
                .fromUserId(USER).toUserId(OTHER).relationType("PASTEUR")
                .statut(MemberRelation.RelationStatus.REVOKED).build();
        when(relationService.revoke(any(), any(), eq(USER))).thenReturn(revoked);
        when(relationService.viewOf(any(), any(), any())).thenReturn(view("REVOKED"));

        mockMvc.perform(delete("/api/v1/relations/me/{id}", UUID.randomUUID())
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REVOKED"));
    }

    @Test
    @DisplayName("DELETE /relations/me/{id} : un tiers non concerné est refusé (400 métier)")
    void revoke_forbiddenForUnrelatedMember() throws Exception {
        when(relationService.revoke(any(), any(), eq(USER)))
                .thenThrow(new BusinessRuleException(
                        "Seul le membre concerné, son encadrant, un pasteur ou un administrateur "
                                + "peut retirer ce rattachement", "RELATION_NOT_OWNER"));

        mockMvc.perform(delete("/api/v1/relations/me/{id}", UUID.randomUUID())
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /relations/me/{id} : relation d'un autre tenant → 404")
    void revoke_crossTenantIsNotFound() throws Exception {
        when(relationService.revoke(any(), any(), any()))
                .thenThrow(new EntityNotFoundException("MemberRelation", UUID.randomUUID()));

        mockMvc.perform(delete("/api/v1/relations/me/{id}", UUID.randomUUID())
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isNotFound());
    }

    // ======================== LECTURE D'AUTRUI ========================

    @Test
    @DisplayName("GET /relations/users/{id} : refusé à un membre ordinaire, autorisé à un responsable")
    void relationsOf_roleGated() throws Exception {
        when(relationService.summary(any(), any(), any()))
                .thenReturn(Map.of("sortantes", List.of(), "entrantes", List.of()));

        mockMvc.perform(get("/api/v1/relations/users/{userId}", OTHER)
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/relations/users/{userId}", OTHER)
                        .header("Authorization", bearer("RESPONSABLE")))
                .andExpect(status().isOk());
    }

    // ======================== HIÉRARCHIE ========================

    @Test
    @DisplayName("GET /hierarchy/me : agrégat complet pour un membre ordinaire")
    void hierarchyMe_allowedForMember() throws Exception {
        java.util.Map<String, Object> suivi = new java.util.LinkedHashMap<>();
        suivi.put("faiseur", null); // aucun pasteur connu : la clé existe mais vaut null
        suivi.put("departementsDiriges", List.of());
        java.util.Map<String, Object> aggregate = new java.util.LinkedHashMap<>();
        aggregate.put("userId", USER);
        aggregate.put("nomComplet", "Awa Diallo");
        aggregate.put("rolePrincipal", "MEMBRE");
        aggregate.put("roles", List.of());
        aggregate.put("branches", List.of());
        aggregate.put("relations", Map.of("sortantes", List.of(), "entrantes", List.of()));
        aggregate.put("ascendants", List.of());
        aggregate.put("suivi", suivi);
        aggregate.put("resume", Map.of("hierarchieComplete", false,
                "origines", List.of("DECLARATIF")));
        when(hierarchyService.getHierarchy(any(), any(), any())).thenReturn(aggregate);

        mockMvc.perform(get("/api/v1/hierarchy/me").header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomComplet").value("Awa Diallo"))
                .andExpect(jsonPath("$.resume.hierarchieComplete").value(false));
    }

    @Test
    @DisplayName("GET /hierarchy/users/{id} : garde de rôle — un membre est refusé")
    void hierarchyOf_roleGated() throws Exception {
        when(hierarchyService.getHierarchy(any(), any(), any()))
                .thenReturn(Map.of("userId", OTHER));

        mockMvc.perform(get("/api/v1/hierarchy/users/{userId}", OTHER)
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/hierarchy/users/{userId}", OTHER)
                        .header("Authorization", bearer("PASTEUR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /hierarchy/users/{id} : membre d'une autre église → 404, pas 403 (pas de fuite)")
    void hierarchyOf_crossTenantNotFound() throws Exception {
        when(hierarchyService.getHierarchy(any(), any(), any()))
                .thenThrow(new EntityNotFoundException("User", OTHER));

        mockMvc.perform(get("/api/v1/hierarchy/users/{userId}", OTHER)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /hierarchy/* : anonyme → 401 (rien n'est public)")
    void hierarchy_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/hierarchy/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/hierarchy/users/{userId}", OTHER)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/relations/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/relations/me/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("le tenant vient du contexte serveur : un tenantId en query string est IGNORÉ")
    void tenantIsNeverTakenFromRequestParameter() throws Exception {
        when(relationService.summary(any(), any(), any()))
                .thenReturn(Map.of("sortantes", List.of(), "entrantes", List.of()));

        // Attaque classique : tenter de forcer un autre tenant par l'URL.
        mockMvc.perform(get("/api/v1/relations/me")
                        .header("Authorization", bearer("MEMBRE"))
                        .param("tenantId", OTHER.toString()))
                .andExpect(status().isOk());

        ArgumentCaptor<UUID> tenant = ArgumentCaptor.forClass(UUID.class);
        verify(relationService).summary(tenant.capture(), any(), any());
        assertThat(tenant.getValue())
                .as("le tenant injecté en query string ne doit jamais atteindre le service")
                .isNotEqualTo(OTHER);
    }
}
