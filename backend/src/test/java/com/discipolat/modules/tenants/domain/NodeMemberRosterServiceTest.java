package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService.NodeMemberRow;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService.RosterCounters;
import com.discipolat.modules.tenants.domain.NodeMemberRosterService.RosterScope;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Audit hiérarchie ecclésiastique — BE-1/BE-3/BE-4 : le roster « les membres
 * d'une entité », à chaque échelle.
 *
 * <p><b>Ce que ces tests verrouillent réellement</b> (et qui serait
 * silencieusement faux sans eux) :
 * <ol>
 *   <li>le <b>sens</b> de la consultation : un membre rattaché à une cellule ne
 *       remonte pas dans la liste de ses ancêtres, et un membre rattaché à une
 *       région ne descend pas dans la liste de chaque cellule — c'est exactement
 *       l'inversion qui constituait le défaut B2 ;</li>
 *   <li>la <b>déduplication</b> : un membre joignable par deux chemins (adhésion
 *       + capacité) n'apparaît qu'une fois dans le sous-arbre, au point
 *       d'attachement le plus proche du terrain — sans disparaître pour autant
 *       de la liste « directs » de son nœud ;</li>
 *   <li>les <b>compteurs</b> : directs ⊆ sous-arbre, égaux à la longueur des
 *       listes servies, et indépendants de la portée consultée comme du filtre ;</li>
 *   <li>l'<b>isolation</b> : un nœud d'une autre église est invisible (404), et
 *       la lecture ne dégénère pas en N+1 ;</li>
 *   <li>le <b>repli explicite</b> : rien n'est inventé quand les données
 *       manquent (scénario « données manquantes » de la mission).</li>
 * </ol>
 *
 * <p>Les repositories sont des INTERFACES : mockés sans risque (contrainte
 * Byte Buddy/JDK du projet, cf. {@code TenantOrganizationServiceTest}).
 *
 * <p>Note de fixture : l'énumération {@link MembershipScopeType} est PLUS GROSSIÈRE
 * que {@link OrganizationNodeType} (pas de constante {@code GROUP}) — le roster
 * rattache par {@code scopeId}, pas par le nom du scope, ce que la mission
 * « consulter les membres à chaque échelle » impose comme comportement, quel que
 * soit la granularité du scope saisi.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NodeMemberRosterServiceTest {

    private static final UUID TENANT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID AUTRE_TENANT = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private static final UUID ROOT_ID = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    private static final UUID REGION_ID = UUID.fromString("c0000000-0000-0000-0000-000000000002");
    private static final UUID CELL_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    private static final UUID AILLEURS_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BLAISE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CAMILLE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DENIS = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID ROLE_RESPONSABLE = UUID.fromString("55555555-5555-5555-5555-555555555551");
    private static final UUID ROLE_DIACRE = UUID.fromString("55555555-5555-5555-5555-555555555552");

    private static final Instant T0 = Instant.parse("2026-01-10T08:00:00Z");

    @Mock private OrganizationNodeRepository nodeRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private MemberRoleAssignmentRepository assignmentRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;

    private NodeMemberRosterService service;

    private final List<TenantMembership> memberships = new ArrayList<>();
    private final List<MemberRoleAssignment> assignments = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new NodeMemberRosterService(nodeRepository, membershipRepository,
                assignmentRepository, roleRepository, userRepository);

        // Réseau : Racine (église) → Région Nord → Cellule A.
        OrganizationNode root = node(ROOT_ID, null, "Eglise Grace", OrganizationNodeType.ROOT_CHURCH, null);
        OrganizationNode region = node(REGION_ID, ROOT_ID, "Region Nord", OrganizationNodeType.REGION, null);
        OrganizationNode cell = node(CELL_ID, REGION_ID, "Cellule A", OrganizationNodeType.GROUP, null);

        Map<UUID, OrganizationNode> byId = Map.of(ROOT_ID, root, REGION_ID, region, CELL_ID, cell);
        when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(root, region, cell));
        when(nodeRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(byId.get(inv.getArgument(0))));

        memberships.clear();
        assignments.clear();
        when(membershipRepository.findByTenantIdAndStatus(TENANT, MembershipStatus.ACTIVE))
                .thenAnswer(inv -> List.copyOf(memberships));
        when(assignmentRepository.findByTenantIdAndStatus(TENANT, MemberRoleAssignment.AssignmentStatus.ACTIVE))
                .thenAnswer(inv -> List.copyOf(assignments));

        users.clear();
        users.add(user(ALICE, "Alice", "Alpha"));
        users.add(user(BLAISE, "Blaise", "Beta"));
        users.add(user(CAMILLE, "Camille", "Charlie"));
        users.add(user(DENIS, "Denis", "Delta"));
        when(userRepository.findAllById(any())).thenAnswer(inv -> List.copyOf(users));

        Map<UUID, Role> roles = Map.of(
                ROLE_RESPONSABLE, role(ROLE_RESPONSABLE, "Responsable"),
                ROLE_DIACRE, role(ROLE_DIACRE, "Diacre"));
        when(roleRepository.findAllById(any())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            return roles.entrySet().stream().filter(e -> ids.contains(e.getKey()))
                    .map(Map.Entry::getValue).toList();
        });
    }

    // ==================== SENS DE LA CONSULTATION ====================

    @Test
    @DisplayName("DIRECT : seuls les membres rattachés À CE NŒUD, pas ceux des sous-structures")
    void direct_neRetientQueLeNoeudConsulte() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));

        Map<String, Object> roster = service.roster(TENANT, REGION_ID, RosterScope.DIRECT, null, 0, 20);

        List<NodeMemberRow> rows = contentOf(roster);
        assertEquals(1, rows.size(), "la portée DIRECT ne doit pas faire descendre les cellules");
        assertEquals(ALICE, rows.get(0).userId());
        assertTrue(rows.get(0).direct());
        assertEquals("ADHESION_NOEUD", rows.get(0).origine());
        assertEquals(1L, roster.get("memberCountDirect"));
        assertEquals(1, roster.get("totalElements"));
    }

    @Test
    @DisplayName("SUBTREE : les sous-structures descendent, marquées indirectes avec leur nœud")
    void subtree_inclutLesDescendantsEtMarqueIndirect() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));

        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20));

        assertEquals(2, rows.size());
        NodeMemberRow blaise = rowOf(rows, BLAISE);
        assertFalse(blaise.direct(), "rattaché à une sous-structure ⇒ indirect");
        assertEquals("Cellule A", blaise.attachedNodeName());
        assertEquals("GROUP", blaise.attachedNodeType());
        assertEquals(1, blaise.viaChaine().size(), "le fil descendant doit un chaînon, jamais plus");
        assertEquals(CELL_ID, blaise.viaChaine().get(0).get("id"));
        assertEquals("GROUP", blaise.viaChaine().get(0).get("type"));
        // Le membre rattaché ICI reste direct et n'a pas de fil d'Ariane descendant.
        assertTrue(rowOf(rows, ALICE).direct());
        assertTrue(rowOf(rows, ALICE).viaChaine().isEmpty());
    }

    @Test
    @DisplayName("Preuve rouge B2 : un membre d'une REGION ne descend PAS dans la liste d'une CELLULE")
    void lesAncetresNeDescendentPasDansLeRoster() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));

        // L'ancienne sémantique (comparaison de `path` à l'envers dans
        // getMembersWithAccess) retenait ALICE ici : le roster d'une cellule était
        // inondé par les rattachements de tous ses ancêtres.
        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, CELL_ID, RosterScope.SUBTREE, null, 0, 20));

        assertEquals(1, rows.size());
        assertEquals(BLAISE, rows.get(0).userId());
        assertFalse(rows.stream().anyMatch(r -> ALICE.equals(r.userId())),
                "un rattachement d'ancêtre ne doit jamais descendre dans le roster d'une sous-structure");
    }

    @Test
    @DisplayName("Adhésion de portée TENANT : visible seulement sous la racine")
    void adhesionTenant_neInondePasLesNoeudsIntermediaires() {
        memberships.add(membership(CAMILLE, MembershipScopeType.TENANT, null));

        Map<String, Object> sousLaRacine = service.roster(TENANT, ROOT_ID, RosterScope.SUBTREE, null, 0, 20);
        List<NodeMemberRow> racine = contentOf(sousLaRacine);
        assertEquals(1, racine.size(), "la racine EST l'organisation : le membre y figure");
        assertTrue(racine.get(0).direct());
        assertEquals("ADHESION_TENANT", racine.get(0).origine());
        assertEquals(1L, sousLaRacine.get("memberCountDirect"));

        List<NodeMemberRow> sousLaRegion =
                contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20));
        assertTrue(sousLaRegion.isEmpty(),
                "une portée TENANT ne rattache à AUCUNE sous-structure (c'était le défaut B2)");
    }

    // ==================== DÉDUPLICATION ====================

    @Test
    @DisplayName("Deux chemins vers un même membre = UNE ligne, au point le plus proche du terrain")
    void dedup_auPointLePlusProche() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        assignments.add(assignment(ALICE, CELL_ID, ROLE_RESPONSABLE));

        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20));

        assertEquals(1, rows.size(), "aucun doublon quand adhésion et capacité mènent au même sous-arbre");
        NodeMemberRow alice = rows.get(0);
        assertEquals(ALICE, alice.userId());
        assertEquals(CELL_ID, alice.attachedNodeId(), "l'attachement retenu est le plus profond");
        assertEquals("ASSIGNATION_V3", alice.origine());
        assertEquals(List.of("Responsable"), alice.roles(), "le rôle porté à ce nœud est restitué");
    }

    @Test
    @DisplayName("Adhésion ici + capacité plus bas : direct ICI, et une seule ligne dans le sous-arbre")
    void leDirect_neDisparaitPasQuandUneCapaciteEstPlusProfonde() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        assignments.add(assignment(ALICE, CELL_ID, ROLE_RESPONSABLE));

        List<NodeMemberRow> directs =
                contentOf(service.roster(TENANT, REGION_ID, RosterScope.DIRECT, null, 0, 20));
        assertEquals(1, directs.size(), "son adhésion à CE nœud est un rattachement direct, même s'il "
                + "exerce une capacité plus bas");
        assertTrue(directs.get(0).direct());
        assertEquals("ADHESION_NOEUD", directs.get(0).origine());

        RosterCounters counters = service.counters(TENANT, REGION_ID);
        assertEquals(1L, counters.memberCountDirect());
        assertEquals(1L, counters.memberCountSousArbre(), "un membre, une fois, à chaque échelle");
    }

    @Test
    @DisplayName("Tous les rôles du périmètre sont restitués, pas seulement celui du nœud retenu")
    void rolesPortes_tousRestitues() {
        assignments.add(assignment(ALICE, REGION_ID, ROLE_DIACRE));
        assignments.add(assignment(ALICE, CELL_ID, ROLE_RESPONSABLE));

        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20));

        assertEquals(1, rows.size());
        List<String> roles = rows.get(0).roles();
        assertEquals(2, roles.size(), "une ligne = un membre, mais la ligne porte ses DEUX rôles du périmètre");
        assertTrue(roles.contains("Diacre") && roles.contains("Responsable"), roles.toString());
    }

    @Test
    @DisplayName("Responsable de nœud : signal autoritatif, même sans adhésion ni capacité")
    void responsableNoeud_apparait() {
        OrganizationNode cellAvecResponsable = node(CELL_ID, REGION_ID, "Cellule A",
                OrganizationNodeType.GROUP, DENIS);
        when(nodeRepository.findByTenantId(TENANT))
                .thenReturn(List.of(node(ROOT_ID, null, "Eglise Grace", OrganizationNodeType.ROOT_CHURCH, null),
                        node(REGION_ID, ROOT_ID, "Region Nord", OrganizationNodeType.REGION, null),
                        cellAvecResponsable));

        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20));

        assertEquals(1, rows.size());
        assertEquals(DENIS, rows.get(0).userId());
        assertEquals("RESPONSABLE_NOEUD", rows.get(0).origine());
        assertFalse(rows.get(0).direct());
    }

    // ==================== COMPTEURS (BE-3) ====================

    @Test
    @DisplayName("Compteurs : égaux aux listes servies, directs ⊆ sous-arbre")
    void compteurs_coherentsAvecLeRoster() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));
        memberships.add(membership(CAMILLE, MembershipScopeType.DEPARTMENT, CELL_ID));

        Map<String, Object> roster = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20);
        RosterCounters counters = service.counters(TENANT, REGION_ID);

        assertEquals(1L, roster.get("memberCountDirect"));
        assertEquals(3L, roster.get("memberCountSousArbre"));
        assertEquals(3, roster.get("totalElements"));
        assertEquals(1L, counters.memberCountDirect());
        assertEquals(3L, counters.memberCountSousArbre());
        assertTrue(counters.memberCountDirect() <= counters.memberCountSousArbre(),
                "les directs sont un sous-ensemble du sous-arbre, jamais un compteur parallèle");

        // Le compteur DIRECT doit être la longueur EXACTE de la liste DIRECT.
        assertEquals(contentOf(service.roster(TENANT, REGION_ID, RosterScope.DIRECT, null, 0, 20)).size(),
                counters.memberCountDirect());
    }

    @Test
    @DisplayName("Compteurs structurels : ni la portée ni le filtre ne les déplacent")
    void compteurs_independantsDeLaPorteeEtDuFiltre() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));
        memberships.add(membership(CAMILLE, MembershipScopeType.DEPARTMENT, CELL_ID));

        Map<String, Object> subtree = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20);
        Map<String, Object> direct = service.roster(TENANT, REGION_ID, RosterScope.DIRECT, null, 0, 20);
        Map<String, Object> filtre = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, "ali", 0, 20);

        assertEquals(subtree.get("memberCountDirect"), direct.get("memberCountDirect"),
                "les compteurs décrivent l'entité, pas la portée demandée");
        assertEquals(subtree.get("memberCountSousArbre"), direct.get("memberCountSousArbre"));
        assertEquals(subtree.get("memberCountSousArbre"), filtre.get("memberCountSousArbre"),
                " taper « ali » ne doit pas faire croire que la région n'a qu'un seul membre");
        assertEquals(3, subtree.get("totalElements"));
        assertEquals(1, filtre.get("totalElements"), "en revanche le résultat de recherche suit le filtre");
    }

    // ==================== FILTRE, PAGINATION, ORDRE ====================

    @Test
    @DisplayName("q : filtre prénom, nom, nom complet et email, insensible à la casse")
    void q_filtreNomEtEmail() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.REGION, REGION_ID));

        assertEquals(1, contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, "ali", 0, 20)).size());
        assertEquals(1, contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, "ALPHA", 0, 20)).size());
        assertEquals(1, contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, "beta", 0, 20)).size());
        assertEquals(1, contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, "blaise@", 0, 20)).size());
        assertEquals(2, contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, "  ", 0, 20)).size(),
                "un filtre blanc ne filtre rien");
    }

    @Test
    @DisplayName("Pagination : ordre stable (nom, prénom), taille bornée, page hors étendue = vide")
    void pagination_borneEtStable() {
        memberships.add(membership(CAMILLE, MembershipScopeType.REGION, REGION_ID)); // Camille Charlie
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));   // Alice Alpha
        memberships.add(membership(BLAISE, MembershipScopeType.REGION, REGION_ID));  // Blaise Beta

        Map<String, Object> page0 = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 2);
        List<NodeMemberRow> rows0 = contentOf(page0);
        assertEquals(2, rows0.size());
        assertEquals(ALICE, rows0.get(0).userId(), "ordre alphabétique stable sur le nom");
        assertEquals(BLAISE, rows0.get(1).userId());
        assertEquals(2, page0.get("size"));
        assertEquals(2, page0.get("totalPages"));
        assertEquals(3, page0.get("totalElements"), "total = ensemble filtré, pas la page");

        List<NodeMemberRow> rowsHorsEtendue =
                contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 9, 2));
        assertTrue(rowsHorsEtendue.isEmpty(), "une page au-delà de la fin est vide, jamais une erreur");

        Map<String, Object> bornee = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 5000);
        assertEquals(100, bornee.get("size"), "la taille de page est bornée côté serveur");

        Map<String, Object> tailleAberrante = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, -3, 0);
        assertEquals(1, tailleAberrante.get("size"), "une taille aberrante retombe sur 1, jamais division par zéro");
        assertEquals(0, tailleAberrante.get("page"), "une page négative retombe sur la première page");
    }

    // ==================== DÉGRADATION / ISOLATION ====================

    @Test
    @DisplayName("Compte supprimé (RGPD) : jamais affiché, jamais compté")
    void compteSupprime_exclu() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(DENIS, MembershipScopeType.REGION, REGION_ID));

        users.removeIf(u -> DENIS.equals(u.getId()));
        users.add(User.builder().id(DENIS).tenantId(TENANT).firstName("Denis").lastName("Delta")
                .email("denis@eglise.org").statut(UserStatus.INACTIVE).deleted(true).build());

        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20));

        assertEquals(1, rows.size());
        assertEquals(ALICE, rows.get(0).userId());
        Map<String, Object> roster = service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20);
        assertEquals(1L, roster.get("memberCountSousArbre"), "un compte supprimé ne reste pas dans les compteurs");
    }

    @Test
    @DisplayName("Cross-tenant : le nœud d'une autre église est INVISIBLE (404), jamais 403")
    void crossTenant_invisible() {
        when(nodeRepository.findById(AILLEURS_ID)).thenReturn(Optional.of(OrganizationNode.builder()
                .id(AILLEURS_ID).tenantId(AUTRE_TENANT).parentId(null)
                .type(OrganizationNodeType.ROOT_CHURCH).name("Eglise D ailleurs").code("AILLEURS")
                .status(OrganizationNodeStatus.ACTIVE).path("AILLEURS").level(0).sortOrder(0).build()));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> service.roster(TENANT, AILLEURS_ID, RosterScope.SUBTREE, null, 0, 20));
        assertNotNull(ex.getMessage());

        // Nœud inconnu : même réponse — pas d'énumération possible.
        assertThrows(EntityNotFoundException.class,
                () -> service.roster(TENANT, UUID.randomUUID(), RosterScope.SUBTREE, null, 0, 20));
        assertThrows(EntityNotFoundException.class, () -> service.counters(TENANT, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Aucun rattachement : liste vide explicite — rien n'est inventé")
    void donneesManquantes_repliExplicite() {
        Map<String, Object> roster = service.roster(TENANT, CELL_ID, RosterScope.SUBTREE, null, 0, 20);

        assertTrue(contentOf(roster).isEmpty());
        assertEquals(0, roster.get("totalElements"));
        assertEquals(0L, roster.get("memberCountDirect"));
        assertEquals(0L, roster.get("memberCountSousArbre"));
        assertEquals("Cellule A", roster.get("nodeName"), "le nœud consulté reste identifié");
        assertEquals("SUBTREE", roster.get("scope"));
        RosterCounters counters = service.counters(TENANT, CELL_ID);
        assertEquals(0L, counters.memberCountDirect());
        assertEquals(0L, counters.memberCountSousArbre());
    }

    @Test
    @DisplayName("Pas de N+1 : une lecture par source, quel que soit le volume du roster")
    void lectureEnUnSeulAller() {
        memberships.add(membership(ALICE, MembershipScopeType.REGION, REGION_ID));
        memberships.add(membership(BLAISE, MembershipScopeType.DEPARTMENT, CELL_ID));
        memberships.add(membership(CAMILLE, MembershipScopeType.DEPARTMENT, CELL_ID));
        assignments.add(assignment(BLAISE, CELL_ID, ROLE_RESPONSABLE));

        service.roster(TENANT, REGION_ID, RosterScope.SUBTREE, null, 0, 20);

        verify(nodeRepository, times(1)).findByTenantId(TENANT);
        verify(userRepository, times(1)).findAllById(any());
        verify(membershipRepository, times(1)).findByTenantIdAndStatus(TENANT, MembershipStatus.ACTIVE);
        verify(assignmentRepository, times(1)).findByTenantIdAndStatus(TENANT,
                MemberRoleAssignment.AssignmentStatus.ACTIVE);
        // Le N+1 historique faisait deux `findById` PAR membership : ici un seul
        // (la résolution du nœud consulté), quel que soit le volume du roster.
        verify(nodeRepository, times(1)).findById(any());
        verify(nodeRepository, never()).findDescendants(any(), any());
        // Les libellés de rôles sont préchargés en UNE requête, pas relus par
        // affectation — même famille de défaut que celui corrigé ci-dessus.
        verify(roleRepository, times(1)).findAllById(any());
        verify(roleRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Sous-arbre dérivé par parentId : une chaîne coupée n'invente pas de fil d'Ariane")
    void chaineCoupee_repliSansInvention() {
        memberships.add(membership(ALICE, MembershipScopeType.DEPARTMENT, CELL_ID));
        // Cellule A orpheline de sa région : le parent n'existe plus dans le tenant.
        when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                node(ROOT_ID, null, "Eglise Grace", OrganizationNodeType.ROOT_CHURCH, null),
                node(CELL_ID, REGION_ID, "Cellule A", OrganizationNodeType.GROUP, null)));

        List<NodeMemberRow> rows = contentOf(service.roster(TENANT, ROOT_ID, RosterScope.SUBTREE, null, 0, 20));

        assertTrue(rows.isEmpty(), "un nœud rattaché à une région absente n'est pas un descendant de la racine ;"
                + " le roster ne reconstruit pas une chaîne qu'on ne peut pas prouver");
    }

    // ==================== FIXTURES ====================

    private OrganizationNode node(UUID id, UUID parentId, String name, OrganizationNodeType type,
                                  UUID responsibleId) {
        return OrganizationNode.builder()
                .id(id).tenantId(TENANT).parentId(parentId).type(type).name(name)
                .code(name.toUpperCase().replace(' ', '_'))
                .status(OrganizationNodeStatus.ACTIVE)
                .path(name.toUpperCase().replace(' ', '_'))
                .level(parentId == null ? 0 : 1)
                .sortOrder(0)
                .responsibleId(responsibleId)
                .updatedAt(T0)
                .build();
    }

    private static Role role(UUID id, String label) {
        return Role.builder().id(id).tenantId(TENANT).key(label.toUpperCase()).label(label).build();
    }

    private static TenantMembership membership(UUID userId, MembershipScopeType scopeType, UUID scopeId) {
        return TenantMembership.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).userId(userId)
                .scopeType(scopeType).scopeId(scopeId)
                .status(MembershipStatus.ACTIVE).joinedAt(T0)
                .build();
    }

    private static MemberRoleAssignment assignment(UUID userId, UUID nodeId, UUID roleId) {
        return MemberRoleAssignment.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).userId(userId).nodeId(nodeId)
                .roleId(roleId).status(MemberRoleAssignment.AssignmentStatus.ACTIVE)
                .assignedAt(T0)
                .build();
    }

    private static User user(UUID id, String firstName, String lastName) {
        return User.builder().id(id).tenantId(TENANT).firstName(firstName).lastName(lastName)
                .email(firstName.toLowerCase() + "@eglise.org").statut(UserStatus.ACTIVE).build();
    }

    @SuppressWarnings("unchecked")
    private static List<NodeMemberRow> contentOf(Map<String, Object> roster) {
        return (List<NodeMemberRow>) roster.get("content");
    }

    private static NodeMemberRow rowOf(List<NodeMemberRow> rows, UUID userId) {
        return rows.stream().filter(r -> userId.equals(r.userId())).findFirst()
                .orElseThrow(() -> new AssertionError("membre absent du roster : " + userId));
    }
}
