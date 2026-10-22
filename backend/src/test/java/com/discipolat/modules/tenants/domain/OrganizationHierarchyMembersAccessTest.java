package com.discipolat.modules.tenants.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Audit hiérarchie ecclésiastique — BE-2 / BE-4 : la correction ciblée de
 * {@code getMembersWithAccess}, endpoint existant {@code GET /api/v1/org/nodes/{id}/members}.
 *
 * <p><b>Le défaut</b> : la comparaison était inversée. Le code retenait une
 * adhésion quand le <b>nœud consulté</b> était descendant de la portée du membre
 * ({@code isDescendantOf(nodeId, m.getScopeId())} sur les chemins matérialisés).
 * Sens obtenu : un membre rattaché à une RÉGION était rendu dans la liste de
 * CHAQUE cellule sous cette région — fuite descendante — et toute adhésion de
 * portée {@code TENANT} était retenue sans condition.
 *
 * <p><b>Le contrat conservé</b> : signature, type de retour, garde d'appel. Ces
 * tests verrouillent le SENS corrigé et l'absence de retour de la comparaison par
 * préfixe de {@code path} — la laisser disponible invitait à la rebrancher.
 *
 * <p><b>Distinction assumée avec {@link NodeMemberRosterService}</b> : cette
 * méthode répond à « quelles adhésions relèvent de / couvrent ce nœud » pour le
 * contrôle d'accès — une adhésion {@code TENANT} couvre bien chaque échelle de
 * l'église, elle est donc retenue partout. Le roster, lui, est une LISTE
 * NOMINATIVE par entité : une portée {@code TENANT} n'y figure que sous la
 * racine. Les deux lectures ne sont pas interchangeables ; aligner l'une sur
 * l'autre casserait soit l'accès, soit le listing.
 *
 * <p>Les collaborateurs étrangers à cette lecture (audit, propagation, comptes,
 * modules) sont passés à {@code null} : cette méthode ne les touche jamais, et
 * mocker une classe concrète échoue sur ce JDK (contrainte Byte Buddy, cf.
 * {@code TenantOrganizationServiceTest}) — un {@code null} explicite vaut ici un
 * faux silencieux.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationHierarchyMembersAccessTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID AUTRE_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private static final UUID ROOT_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID REGION_ID = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID CELL_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BLAISE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CAMILLE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Instant T0 = Instant.parse("2026-01-10T08:00:00Z");

    @Mock private OrganizationNodeRepository nodeRepository;
    @Mock private TenantMembershipRepository membershipRepository;

    private OrganizationHierarchyService service;
    private final List<TenantMembership> memberships = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new OrganizationHierarchyService(nodeRepository, null, null,
                membershipRepository, null, null);

        // Chemins matérialisés tels que createNode les construit (parent.code.enfant).
        OrganizationNode root = node(ROOT_ID, null, "EGLISE_GRACE", "Eglise Grace",
                OrganizationNodeType.ROOT_CHURCH);
        OrganizationNode region = node(REGION_ID, ROOT_ID, "EGLISE_GRACE.REGION_NORD", "Region Nord",
                OrganizationNodeType.REGION);
        OrganizationNode cell = node(CELL_ID, REGION_ID, "EGLISE_GRACE.REGION_NORD.CELLULE_A", "Cellule A",
                OrganizationNodeType.GROUP);

        Map<UUID, OrganizationNode> byId = Map.of(ROOT_ID, root, REGION_ID, region, CELL_ID, cell);
        when(nodeRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(byId.get(inv.getArgument(0))));
        when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(root, region, cell));

        memberships.clear();
        when(membershipRepository.findByTenantIdAndStatus(TENANT, MembershipStatus.ACTIVE))
                .thenAnswer(inv -> List.copyOf(memberships));
    }

    @Test
    @DisplayName("Preuve rouge B2 : un membre d'une REGION ne remonte plus dans la liste d'une CELLULE")
    void laFuiteDescendante_estCorrigee() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));

        List<TenantMembership> auNiveauDeLaCellule = service.getMembersWithAccess(TENANT, CELL_ID);

        assertEquals(1, auNiveauDeLaCellule.size(),
                "l'ancien comparateur retenait ALICE ici : le roster d'une cellule était inondé "
                        + "par les rattachements de tous ses ancêtres");
        assertEquals(BLAISE, auNiveauDeLaCellule.get(0).getUserId());
        assertFalse(auNiveauDeLaCellule.stream().anyMatch(m -> ALICE.equals(m.getUserId())),
                "un rattachement posé plus haut dans l'arbre ne descend pas dans une sous-structure");
    }

    @Test
    @DisplayName("Les adhésions posées DANS le sous-arbre relèvent bien du nœud consulté")
    void lesDescendantsRelventDuNoeud() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));

        List<TenantMembership> auNiveauDeLaRegion = service.getMembersWithAccess(TENANT, REGION_ID);

        assertEquals(2, auNiveauDeLaRegion.size(), "la région répond de ce qui est posé sur elle ET sur ses cellules");
        assertTrue(auNiveauDeLaRegion.stream().anyMatch(m -> ALICE.equals(m.getUserId())));
        assertTrue(auNiveauDeLaRegion.stream().anyMatch(m -> BLAISE.equals(m.getUserId())));
    }

    @Test
    @DisplayName("Portée TENANT : couvre chaque échelle (vue d'ACCÈS, à la différence du roster)")
    void porteeTenant_couvreChaqueEchelle() {
        memberships.add(membership(CAMILLE, MembershipScopeType.TENANT, null));

        assertEquals(1, service.getMembersWithAccess(TENANT, ROOT_ID).size());
        assertEquals(1, service.getMembersWithAccess(TENANT, REGION_ID).size(),
                "un membre de l'église sans secteur relève aussi de la région pour l'accès ;"
                        + " le roster nominatif, lui, ne l'affiche que sous la racine");
        assertEquals(1, service.getMembersWithAccess(TENANT, CELL_ID).size());
    }

    @Test
    @DisplayName("Portée OWN et adhésion sans nœud : ne rattachent à aucune entité")
    void ownEtPorteeSansNoeud_exclus() {
        memberships.add(membership(ALICE, MembershipScopeType.OWN, null));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, null));

        assertTrue(service.getMembersWithAccess(TENANT, REGION_ID).isEmpty(),
                "ni « ma famille », ni une adhésion sans portée de nœud ne se répartissent dans l'arbre");
    }

    @Test
    @DisplayName("Nœud inconnu ou d'un autre tenant : liste vide, aucune fuite inter-église")
    void noeudHorsTenant_listeVide() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));

        UUID ailleurs = UUID.fromString("99999999-9999-9999-9999-999999999999");
        // Le nœud lu est celui d'une AUTRE église : le filtre tenant s'applique
        // avant toute lecture des adhésions.
        when(nodeRepository.findById(ailleurs)).thenReturn(Optional.of(OrganizationNode.builder()
                .id(ailleurs).tenantId(AUTRE_TENANT).parentId(null)
                .type(OrganizationNodeType.ROOT_CHURCH).name("Eglise D ailleurs").code("AILLEURS")
                .status(OrganizationNodeStatus.ACTIVE).path("AILLEURS").level(0).sortOrder(0).build()));

        assertTrue(service.getMembersWithAccess(TENANT, ailleurs).isEmpty());
        assertTrue(service.getMembersWithAccess(TENANT, UUID.randomUUID()).isEmpty());
        verify(membershipRepository, never()).findByTenantIdAndStatus(eq(AUTRE_TENANT), any());
    }

    @Test
    @DisplayName("Une lecture par source, et plus jamais la comparaison par préfixe de path")
    void uneSeuleLectureParSource() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));
        memberships.add(membership(CAMILLE, MembershipScopeType.DEPARTMENT, CELL_ID));

        service.getMembersWithAccess(TENANT, REGION_ID);

        verify(nodeRepository, times(1)).findById(REGION_ID);
        verify(nodeRepository, times(1)).findByTenantId(TENANT);
        verify(membershipRepository, times(1)).findByTenantIdAndStatus(TENANT, MembershipStatus.ACTIVE);
        // L'ancien chemin faisait DEUX findById PAR adhésion (dont un via la requête
        // isDescendantOf) : la comparaison par préfixe de path ne doit pas revenir.
        verify(nodeRepository, never()).isDescendantOf(any(), any());
        verify(nodeRepository, never()).findDescendants(any(), any());
    }

    // ==================== FIXTURES ====================

    private static OrganizationNode node(UUID id, UUID parentId, String path, String name,
                                         OrganizationNodeType type) {
        return OrganizationNode.builder()
                .id(id).tenantId(TENANT).parentId(parentId).type(type).name(name)
                .code(name.toUpperCase().replace(' ', '_'))
                .status(OrganizationNodeStatus.ACTIVE)
                .path(path)
                .level(parentId == null ? 0 : 1)
                .sortOrder(0)
                .updatedAt(T0)
                .build();
    }

    private static TenantMembership membership(UUID userId, MembershipScopeType scopeType, UUID scopeId) {
        return TenantMembership.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).userId(userId)
                .scopeType(scopeType).scopeId(scopeId)
                .status(MembershipStatus.ACTIVE).joinedAt(T0)
                .build();
    }
}
