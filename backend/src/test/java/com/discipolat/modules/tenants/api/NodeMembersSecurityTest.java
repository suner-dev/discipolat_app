package com.discipolat.modules.tenants.api;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.test.TestSecurityConfig;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService.NodeMemberRow;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService.RosterScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit hiérarchie ecclésiastique — BE-4 / SEC-1 / SEC-2 : la surface HTTP du
 * roster « les membres d'une entité ».
 *
 * <p>Les tests unitaires de service prouvent le calcul ; ils ne peuvent rien
 * prouver sur les trois points qui comptent ici :
 * <ol>
 *   <li><b>aucune ouverture nouvelle</b> (SEC-1) : le roster exige exactement ce
 *       qu'exige déjà l'annuaire {@code GET /api/v1/users} — un utilisateur
 *       authentifié du tenant. Un MEMBRE ordinaire doit donc passer, et un
 *       anonyme doit être refusé ;</li>
 *   <li><b>le tenant ne vient jamais du client</b> : ni paramètre d'URL, ni
 *       corps — seule la revendication du JWT, revérifiée sur le nœud ;</li>
 *   <li><b>contrat de forme</b> : le web (NodeRoster) et le mobile
 *       (node_member_roster.dart) lisent les mêmes clés ; une renomination
 *       silencieuse casserait les deux clients, ce que le compilateur ne voit
 *       pas.</li>
 * </ol>
 *
 * <p>Coupe {@code @WebMvcTest} + {@code TestSecurityConfig} : chaîne de
 * sécurité réelle (JWT RSA, filtres, {@code @PreAuthorize}), services mockés.
 */
@WebMvcTest(controllers = NodeMemberRosterController.class)
@Import(TestSecurityConfig.class)
class NodeMembersSecurityTest {

    private static final UUID USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID AUTRE_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID NODE = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID MEMBRE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "membre@eglise.org";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockBean private NodeMemberRosterService rosterService;

    private String bearer(String role) {
        return bearer(role, TENANT);
    }

    private String bearer(String role, UUID tenantId) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(
                USER, EMAIL, role, Set.of(role), false, tenantId);
    }

    private static Map<String, Object> roster(RosterScope scope) {
        Map<String, Object> chainon = new java.util.LinkedHashMap<>();
        chainon.put("id", NODE);
        chainon.put("nom", "Region Nord");
        chainon.put("type", "REGION");
        chainon.put("responsibleId", null);

        NodeMemberRow row = new NodeMemberRow(MEMBRE, "Alice", "Alpha", "alice@eglise.org", null,
                "ACTIVE", NODE, "Region Nord", "REGION", false, "ADHESION_NOEUD",
                List.of("Diacre"), List.of(chainon),
                Instant.parse("2026-01-10T08:00:00Z"));

        Map<String, Object> roster = new java.util.LinkedHashMap<>();
        roster.put("nodeId", NODE);
        roster.put("nodeName", "Region Nord");
        roster.put("nodeType", "REGION");
        roster.put("scope", scope.name());
        roster.put("content", List.of(row));
        roster.put("page", 0);
        roster.put("size", 20);
        roster.put("totalElements", 1);
        roster.put("totalPages", 1);
        roster.put("memberCountDirect", 1L);
        roster.put("memberCountSousArbre", 1L);
        return roster;
    }

    // ==================== GARDE D'ACCÈS (SEC-1) ====================

    @Test
    @DisplayName("anonyme → 401 : le roster nominatif n'est jamais public")
    void anonyme_refuse() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE))
                .andExpect(status().isUnauthorized());
        verify(rosterService, never()).roster(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("MEMBRE ordinaire → 200 : stricte parité avec l'annuaire /users, aucune donnée de plus")
    void membreOrdinaire_autorise() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));

        // L'exigence est « authentifié », pas « responsable » : ce que ce membre
        // voyait déjà dans l'annuaire, scopé sur une entité.
        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(MEMBRE.toString()));
    }

    @Test
    @DisplayName("JWT sans revendication tenant → refus, AUCUNE lecture servie (jamais de repli)")
    void sansContexteTenant_rienNestServi() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE", null)))
                .andExpect(status().isUnauthorized());
        verify(rosterService, never()).roster(any(), any(), any(), any(), anyInt(), anyInt());
    }

    // ==================== ISOLATION MULTI-TENANT ====================

    @Test
    @DisplayName("le tenant vient du seul JWT : un tenantId en query string est ignoré")
    void tenant_jamaisPrisDeLaRequete() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));

        // Attaque classique : faire lire le roster d'une autre église par l'URL.
        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE"))
                        .param("tenantId", AUTRE_TENANT.toString()))
                .andExpect(status().isOk());

        ArgumentCaptor<UUID> tenant = ArgumentCaptor.forClass(UUID.class);
        verify(rosterService).roster(tenant.capture(), eq(NODE), any(), any(), anyInt(), anyInt());
        assertThat(tenant.getValue())
                .as("le tenant injecté en query string ne doit jamais atteindre le service")
                .isEqualTo(TENANT)
                .isNotEqualTo(AUTRE_TENANT);
    }

    @Test
    @DisplayName("nœud d'une autre église → 404 (invisible), jamais 403 : pas d'énumération")
    void crossTenant_404() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("OrganizationNode", NODE));

        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isNotFound());
    }

    // ==================== PARAMÈTRES ====================

    @Test
    @DisplayName("scope : DIRECT explicite, casse ignorée, et valeur inconnue → SUBTREE")
    void scope_tolerantSansEtreOuvert() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.DIRECT));

        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE")).param("scope", "DIRECT"))
                .andExpect(status().isOk());
        verify(rosterService, times(1))
                .roster(eq(TENANT), eq(NODE), eq(RosterScope.DIRECT), any(), anyInt(), anyInt());

        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE")).param("scope", "direct"))
                .andExpect(status().isOk());
        // Deuxième appel, même portée : la casse du paramètre ne change rien.
        verify(rosterService, times(2))
                .roster(eq(TENANT), eq(NODE), eq(RosterScope.DIRECT), any(), anyInt(), anyInt());

        // Une valeur aberrante ne provoque pas de 500 ni d'injection : elle retombe
        // sur la portée par défaut, la plus large mais jamais hors tenant.
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));
        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE"))
                        .param("scope", "ALL_TENANTS; DROP TABLE users"))
                .andExpect(status().isOk());
        verify(rosterService).roster(eq(TENANT), eq(NODE), eq(RosterScope.SUBTREE), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("q / page / size sont transmis au service, qui est seul à borner")
    void parametres_transmisAuService() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));

        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE"))
                        .param("q", "ali")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(rosterService).roster(TENANT, NODE, RosterScope.SUBTREE, "ali", 2, 50);
    }

    @Test
    @DisplayName("défauts HTTP : scope=SUBTREE, page=0, size=20 quand le client ne dit rien")
    void defauts_appliquesParLeControleur() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));

        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk());

        verify(rosterService).roster(TENANT, NODE, RosterScope.SUBTREE, null, 0, 20);
    }

    // ==================== CONTRAT DE FORME (web + mobile) ====================

    @Test
    @DisplayName("forme du JSON : les clés lues par NodeRoster.tsx et node_member_roster.dart")
    void formeDuJson_estLeContratClients() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));

        mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("MEMBRE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value(NODE.toString()))
                .andExpect(jsonPath("$.nodeName").value("Region Nord"))
                .andExpect(jsonPath("$.nodeType").value("REGION"))
                .andExpect(jsonPath("$.scope").value("SUBTREE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.memberCountDirect").value(1))
                .andExpect(jsonPath("$.memberCountSousArbre").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(MEMBRE.toString()))
                .andExpect(jsonPath("$.content[0].firstName").value("Alice"))
                .andExpect(jsonPath("$.content[0].lastName").value("Alpha"))
                .andExpect(jsonPath("$.content[0].email").value("alice@eglise.org"))
                .andExpect(jsonPath("$.content[0].statut").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].direct").value(false))
                .andExpect(jsonPath("$.content[0].origine").value("ADHESION_NOEUD"))
                .andExpect(jsonPath("$.content[0].attachedNodeName").value("Region Nord"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("Diacre"))
                .andExpect(jsonPath("$.content[0].viaChaine[0].nom").value("Region Nord"))
                .andExpect(jsonPath("$.content[0].depuis").value("2026-01-10T08:00:00Z"));
    }

    @Test
    @DisplayName("jamais de mot de passe ni de secret dans une ligne de roster")
    void ligneDeRoster_neFuitAucunSecret() throws Exception {
        when(rosterService.roster(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(roster(RosterScope.SUBTREE));

        String body = mockMvc.perform(get("/api/v1/tenant/organization/nodes/{nodeId}/members", NODE)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("le roster projette une identité et un rattachement, jamais les credentials")
                .doesNotContainIgnoringCase("passwordhash")
                .doesNotContainIgnoringCase("twofactorsecret")
                .doesNotContainIgnoringCase("password")
                .doesNotContainIgnoringCase("token");
    }
}
