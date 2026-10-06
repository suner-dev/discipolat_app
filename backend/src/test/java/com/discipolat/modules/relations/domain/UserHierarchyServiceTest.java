package com.discipolat.modules.relations.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.MemberRoleAssignment;
import com.discipolat.modules.tenants.domain.MemberRoleAssignmentRepository;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.RoleTitleService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V231 — agrégat « toute la hiérarchie d'un membre ».
 *
 * <p>Couvre : arbre multi-branches et chaîne des responsables jusqu'à la
 * racine, origines de rattachement (assignation V3 / adhésion à portée de
 * nœud / responsabilité d'un nœud), rôles capacités+legacy, ascendants
 * unifiés (organisation ∪ déclaratif), encadrement pastoral ({@code suivi}),
 * résumé de complétude, isolation multi-tenant et absence de N+1.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserHierarchyServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTRE_TENANT = UUID.randomUUID();
    private static final UUID MEMBRE = UUID.randomUUID();
    private static final UUID PASTEUR = UUID.randomUUID();
    private static final UUID ROLE = UUID.randomUUID();
    private static final UUID NODE_ROOT = UUID.randomUUID();
    private static final UUID NODE_REGION = UUID.randomUUID();
    private static final UUID NODE_CAMPUS = UUID.randomUUID();
    private static final UUID NODE_CAMPUS_2 = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private MemberRoleAssignmentRepository assignmentRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RoleTitleService roleTitleService;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private OrganizationNodeRepository nodeRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private MemberRelationService relationService;
    @Mock private com.discipolat.modules.platform.domain.DictionaryEntryRepository dictionaryEntryRepository;

    private UserHierarchyService svc() {
        return new UserHierarchyService(userRepository, assignmentRepository, roleRepository,
                roleTitleService, membershipRepository, nodeRepository, soulRepository,
                departmentRepository, familyRepository, relationService, dictionaryEntryRepository);
    }

    private static OrganizationNode node(UUID id, UUID parentId, String path,
                                         OrganizationNodeType type, int level,
                                         String name, UUID responsibleId) {
        return OrganizationNode.builder()
                .id(id).tenantId(TENANT).parentId(parentId).path(path).level(level)
                .type(type).name(name).responsibleId(responsibleId).build();
    }

    private static User user(UUID id, UUID tenantId, String first, String last, UserRole role) {
        return User.builder().id(id).tenantId(tenantId).firstName(first).lastName(last)
                .email(first.toLowerCase() + "@x.org").role(role)
                .roles(EnumSet.of(role)).statut(UserStatus.ACTIVE).build();
    }

    private void givenNoRelations() {
        Map<String, Object> relSummary = new LinkedHashMap<>();
        relSummary.put("sortantes", List.of());
        relSummary.put("entrantes", List.of());
        when(relationService.summary(any(), any(), any())).thenReturn(relSummary);
    }

    @Nested
    @DisplayName("Arbre multi-branches")
    class Arbre {

        @Test
        @DisplayName("chaîne des responsables jusqu'à la racine, ancêtre rattaché déduppliqué")
        void chaineCompleteJusquALaRacine() {
            User membre = user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE);
            membre.setRoles(EnumSet.of(UserRole.MEMBRE, UserRole.FAISEUR));
            when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(membre));
            // Les noms de responsables sont résolus par lot (un seul findAllById).
            when(userRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any()))
                    .thenReturn(List.of(user(PASTEUR, TENANT, "Jean", "Maka", UserRole.PASTEUR)));

            // Rattaché au campus (assignation V3) ET à la région (adhésion) :
            // la région, ancêtre du campus, ne doit PAS produire une 2e branche.
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, MEMBRE,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of(
                    TenantMembership.builder().tenantId(TENANT).userId(MEMBRE)
                            .scopeType(MembershipScopeType.CAMPUS).scopeId(NODE_REGION)
                            .status(MembershipStatus.ACTIVE).build()));

            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_ROOT, null, "ROOT", OrganizationNodeType.ROOT_CHURCH, 0, "Église mère", PASTEUR),
                    node(NODE_REGION, NODE_ROOT, "ROOT.R1", OrganizationNodeType.REGION, 1, "Région Est", null),
                    node(NODE_CAMPUS, NODE_REGION, "ROOT.R1.C1", OrganizationNodeType.CAMPUS, 2,
                            "Campus Nord", null)));
            when(nodeRepository.findByResponsibleId(MEMBRE)).thenReturn(List.of());
            givenNoRelations();
            when(roleTitleService.getEffectiveLabel(TENANT, ROLE, NODE_CAMPUS)).thenReturn("Berger de campus");

            Map<String, Object> h = svc().getHierarchy(TENANT, MEMBRE);

            List<?> branches = (List<?>) h.get("branches");
            assertThat(branches).hasSize(1); // la région ancêtre est couverte par la chaîne du campus
            Map<?, ?> branch = (Map<?, ?>) branches.get(0);
            assertThat(branch.get("origine")).isEqualTo("ASSIGNATION_V3");
            assertThat(branch.get("niveaux")).isEqualTo(3);

            List<?> chaine = (List<?>) branch.get("chaine");
            assertThat(chaine).hasSize(3);
            // Rendue feuille → racine : le membre lit « mon chef direct → pasteur ».
            assertThat(((Map<?, ?>) chaine.get(0)).get("nom")).isEqualTo("Campus Nord");
            Map<?, ?> last = (Map<?, ?>) chaine.get(chaine.size() - 1);
            assertThat(last.get("nom")).isEqualTo("Église mère");
            Map<?, ?> resp = (Map<?, ?>) last.get("responsable");
            assertThat(resp.get("id")).isEqualTo(PASTEUR);
            assertThat(resp.get("nom")).isEqualTo("Jean Maka");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roles = (List<Map<String, Object>>) h.get("roles");
            assertThat(roles).anyMatch(r -> "CAPACITE".equals(r.get("source"))
                    && "Berger de campus".equals(r.get("label"))
                    && "Campus Nord".equals(r.get("nodeName")));
            assertThat(roles).anyMatch(r -> "SYSTEME".equals(r.get("source")) && "FAISEUR".equals(r.get("code")));

            List<?> asc = (List<?>) h.get("ascendants");
            assertThat(asc).hasSize(1); // pasteur racine, déduppliqué
        }

        @Test
        @DisplayName("deux campuses = deux branches indépendantes (multi-branches réel)")
        void deuxCampusDeuxBranches() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE)));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, MEMBRE,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build(),
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                            .nodeId(NODE_CAMPUS_2).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of());
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_ROOT, null, "ROOT", OrganizationNodeType.ROOT_CHURCH, 0, "Église mère", PASTEUR),
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1, "Campus Nord", null),
                    node(NODE_CAMPUS_2, NODE_ROOT, "ROOT.C2", OrganizationNodeType.CAMPUS, 1, "Campus Sud", null)));
            givenNoRelations();

            List<?> branches = (List<?>) svc().getHierarchy(TENANT, MEMBRE).get("branches");
            assertThat(branches).hasSize(2);
            assertThat(branches).anySatisfy(b -> assertThat(((Map<?, ?>) b).get("noeud"))
                    .extracting(n -> ((Map<?, ?>) n).get("nom")).isEqualTo("Campus Nord"));
            assertThat(branches).anySatisfy(b -> assertThat(((Map<?, ?>) b).get("noeud"))
                    .extracting(n -> ((Map<?, ?>) n).get("nom")).isEqualTo("Campus Sud"));
        }

        @Test
        @DisplayName("responsabilité d'un nœud = branche même sans assignation V3 (signal pasteur)")
        void responsableDeNoeudSansAssignation() {
            User pasteur = user(PASTEUR, TENANT, "Jean", "Maka", UserRole.PASTEUR);
            when(userRepository.findById(PASTEUR)).thenReturn(Optional.of(pasteur));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, PASTEUR,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of());
            when(membershipRepository.findAllByUserIdAndTenantId(PASTEUR, TENANT)).thenReturn(List.of());
            // Le pasteur dirige le campus : c'est SA branche, même sans assignation.
            when(nodeRepository.findByResponsibleId(PASTEUR)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1,
                            "Campus Nord", PASTEUR)));
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_ROOT, null, "ROOT", OrganizationNodeType.ROOT_CHURCH, 0, "Église mère", null),
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1,
                            "Campus Nord", PASTEUR)));
            givenNoRelations();

            List<?> branches = (List<?>) svc().getHierarchy(TENANT, PASTEUR).get("branches");
            assertThat(branches).hasSize(1);
            assertThat(((Map<?, ?>) branches.get(0)).get("origine")).isEqualTo("RESPONSABLE_NOEUD");
        }

        @Test
        @DisplayName("priorité d'origine : assignation V3 > adhésion > responsabilité")
        void prioriteOrigine() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE)));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, MEMBRE,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of(
                    TenantMembership.builder().tenantId(TENANT).userId(MEMBRE)
                            .scopeType(MembershipScopeType.CAMPUS).scopeId(NODE_CAMPUS)
                            .status(MembershipStatus.ACTIVE).build()));
            when(nodeRepository.findByResponsibleId(MEMBRE)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1, "C", MEMBRE)));
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1, "C", MEMBRE)));
            givenNoRelations();

            List<?> branches = (List<?>) svc().getHierarchy(TENANT, MEMBRE).get("branches");
            assertThat(((Map<?, ?>) branches.get(0)).get("origine")).isEqualTo("ASSIGNATION_V3");
        }

        @Test
        @DisplayName("cycle parent/enfant corrompu : chaîne terminée, pas de boucle infinie")
        void cycleProtege() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE)));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, MEMBRE,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of());
            // A.parent = B, B.parent = A
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_REGION, "A", OrganizationNodeType.CAMPUS, 1, "A", null),
                    node(NODE_REGION, NODE_CAMPUS, "B", OrganizationNodeType.REGION, 2, "B", null)));
            givenNoRelations();

            Map<String, Object> h = svc().getHierarchy(TENANT, MEMBRE);
            List<?> branches = (List<?>) h.get("branches");
            assertThat(branches).hasSize(1);
            assertThat((List<?>) ((Map<?, ?>) branches.get(0)).get("chaine")).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Ascendants, suivi et complétude")
    class Enrichissements {

        @Test
        @DisplayName("ascendants unifiés : ORGANISATION ∪ DÉCLARATIF, sans soi ni doublons")
        void ascendantsUnifies() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, TENANT, "Awa", null, UserRole.MEMBRE)));
            UUID berger = UUID.randomUUID();
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, MEMBRE,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of());
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1, "Campus", berger)));
            when(userRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any())).thenReturn(List.of(
                    user(berger, TENANT, "Paul", "Beye", UserRole.RESPONSABLE)));

            Map<String, Object> relSummary = new LinkedHashMap<>();
            relSummary.put("sortantes", List.of(Map.of(
                    "otherUserId", PASTEUR, "otherNom", "Jean Maka",
                    "relationType", "PASTEUR", "typeLabel", "Mon pasteur")));
            relSummary.put("entrantes", List.of());
            when(relationService.summary(any(), any(), any())).thenReturn(relSummary);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> asc = (List<Map<String, Object>>) svc()
                    .getHierarchy(TENANT, MEMBRE).get("ascendants");
            assertThat(asc).hasSize(2);
            assertThat(asc).anyMatch(a -> "ORGANISATION".equals(a.get("via")));
            assertThat(asc).anyMatch(a -> "DECLARATIF".equals(a.get("via"))
                    && "Mon pasteur".equals(a.get("typeLabel")));
        }

        @Test
        @DisplayName("suivi : pasteur du candidat, chef de famille, département et nœud dirigés")
        void suiviComplet() {
            UUID departementId = UUID.randomUUID();
            UUID familleId = UUID.randomUUID();
            User membre = user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.FAISEUR);
            membre.setFamilleGereeId(familleId);
            when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(membre));

            com.discipolat.modules.souls.domain.Soul ame =
                    new com.discipolat.modules.souls.domain.Soul();
            ame.setUserId(MEMBRE);
            ame.setFaiseurId(PASTEUR);
            when(soulRepository.findAllByUserId(MEMBRE)).thenReturn(List.of(ame));
            when(userRepository.findById(PASTEUR))
                    .thenReturn(Optional.of(user(PASTEUR, TENANT, "Jean", "Maka", UserRole.PASTEUR)));

            com.discipolat.modules.families.domain.Family famille =
                    new com.discipolat.modules.families.domain.Family();
            famille.setId(familleId);
            famille.setTenantId(TENANT);
            famille.setNom("Famille Diallo");
            famille.setChefFamilleId(PASTEUR);
            when(familyRepository.findById(familleId)).thenReturn(Optional.of(famille));

            com.discipolat.modules.departments.domain.Department dep =
                    new com.discipolat.modules.departments.domain.Department();
            dep.setId(departementId);
            dep.setTenantId(TENANT);
            dep.setNom("Louange");
            dep.setResponsableId(MEMBRE);
            when(departmentRepository.findByResponsableId(MEMBRE)).thenReturn(List.of(dep));
            when(nodeRepository.findByResponsibleId(MEMBRE)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1, "Campus Nord", MEMBRE)));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(any(), any(), any())).thenReturn(List.of());
            when(membershipRepository.findAllByUserIdAndTenantId(any(), any())).thenReturn(List.of());
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_ROOT, "ROOT.C1", OrganizationNodeType.CAMPUS, 1, "Campus Nord", MEMBRE)));
            givenNoRelations();

            @SuppressWarnings("unchecked")
            Map<String, Object> suivi = (Map<String, Object>) svc().getHierarchy(TENANT, MEMBRE).get("suivi");
            assertThat(((Map<?, ?>) suivi.get("faiseur")).get("nom")).isEqualTo("Jean Maka");
            assertThat(((Map<?, ?>) suivi.get("familleGeree")).get("nom")).isEqualTo("Famille Diallo");
            assertThat(((Map<?, ?>) suivi.get("chefDeFamille")).get("chefFamilleNom")).isEqualTo("Jean Maka");
            assertThat((List<?>) suivi.get("departementsDiriges")).hasSize(1);
            assertThat((List<?>) suivi.get("noeudsDiriges")).hasSize(1);
        }

        @Test
        @DisplayName("résumé de complétude : l'interface sait dire ce qu'elle ignore")
        void resumeExposeLesOrigines() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE)));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(any(), any(), any())).thenReturn(List.of());
            when(membershipRepository.findAllByUserIdAndTenantId(any(), any())).thenReturn(List.of());
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of());

            Map<String, Object> relSummary = new LinkedHashMap<>();
            relSummary.put("sortantes", List.of(Map.of("otherUserId", PASTEUR)));
            relSummary.put("entrantes", List.of(Map.of("otherUserId", MEMBRE), Map.of("otherUserId", MEMBRE)));
            when(relationService.summary(any(), any(), any())).thenReturn(relSummary);

            @SuppressWarnings("unchecked")
            Map<String, Object> resume = (Map<String, Object>) svc().getHierarchy(TENANT, MEMBRE).get("resume");
            assertThat(resume.get("branchesOrganisationnelles")).isEqualTo(0);
            assertThat(resume.get("encadrantsDeclares")).isEqualTo(1);
            assertThat(resume.get("membresRattaches")).isEqualTo(2);
            assertThat(resume.get("hierarchieComplete")).isEqualTo(false);
            assertThat((List<String>) resume.get("origines")).containsExactly("DECLARATIF");
        }
    }

    @Nested
    @DisplayName("Sécurité et volumétrie")
    class Securite {

        @Test
        @DisplayName("membre d'une AUTRE église : invisible (404 logique), pas de fuite")
        void isolationMultiTenant() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, AUTRE_TENANT, "Awa", "Diallo", UserRole.MEMBRE)));

            assertThatThrownBy(() -> svc().getHierarchy(TENANT, MEMBRE))
                    .isInstanceOf(EntityNotFoundException.class);
            assertThatThrownBy(() -> svc().summarize(TENANT, MEMBRE))
                    .isInstanceOf(EntityNotFoundException.class);
            // Aucune fuite possible en aval.
            verify(relationService, never()).summary(any(), any(), any());
        }

        @Test
        @DisplayName("membre supprimé : invisible")
        void membreSupprime() {
            User deleted = user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE);
            deleted.setDeleted(true);
            when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> svc().getHierarchy(TENANT, MEMBRE))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("aucun N+1 : noms de responsables résolus en un seul findAllById")
        void pasDeNPlusUnSurLesResponsables() {
            when(userRepository.findById(MEMBRE))
                    .thenReturn(Optional.of(user(MEMBRE, TENANT, "Awa", "Diallo", UserRole.MEMBRE)));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(any(), any(), any())).thenReturn(List.of());
            when(membershipRepository.findAllByUserIdAndTenantId(any(), any())).thenReturn(List.of());
            // 20 nœuds, tous avec un responsable : 1 seul findAllById attendu.
            List<OrganizationNode> many = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                UUID responsable = UUID.randomUUID();
                many.add(node(UUID.randomUUID(), null, "N" + i, OrganizationNodeType.CAMPUS, 0,
                        "Campus " + i, responsable));
            }
            when(nodeRepository.findByTenantId(TENANT)).thenReturn(many);
            when(nodeRepository.findByResponsibleId(MEMBRE)).thenReturn(many);
            when(userRepository.findAllById(ArgumentMatchers.<Iterable<UUID>>any())).thenReturn(List.of());
            givenNoRelations();

            List<?> branches = (List<?>) svc().getHierarchy(TENANT, MEMBRE).get("branches");
            assertThat(branches).hasSize(20);
            verify(userRepository, times(1)).findAllById(ArgumentMatchers.<Iterable<UUID>>any());
        }
    }
}
