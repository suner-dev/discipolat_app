package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §9.9 — <b>T-Q1</b>.
 *
 * <p>Couvre les invariants V3 côté service, sans base (mocks de repository) :
 * <ul>
 *   <li><b>V3-B</b> capacité ≠ intitulé : résolution du libellé (nœud → défaut
 *       tenant → label global) et la permission ne dépend JAMAIS du label.</li>
 *   <li><b>V3-C</b> affiliation multi-nœuds : couverture par ancêtre
 *       ({@code path}), fin = ENDED (pas de purge), isolation stricte (404).</li>
 *   <li><b>V3-D</b> modules par nœud INDÉPENDANT par défaut (un enfant n'hérite
 *       pas de la racine sauf {@code INHERITED} explicite).</li>
 *   <li><b>V3-E / D7</b> agrégats recalculés, sans PII nominatif dans la vue.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationV3ServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID ROLE = UUID.randomUUID();
    private static final UUID NODE_ROOT = UUID.randomUUID();
    private static final UUID NODE_REGION = UUID.randomUUID();
    private static final UUID NODE_CAMPUS = UUID.randomUUID();

    private static OrganizationNode node(UUID id, UUID parentId, String path,
                                         OrganizationNodeType type,
                                         OrganizationNode.ConfigSource source) {
        return OrganizationNode.builder()
                .id(id).tenantId(TENANT).parentId(parentId).path(path)
                .level(path.split("\\.").length)
                .type(type).name("N" + id.toString().substring(0, 4))
                .configSource(source)
                .build();
    }

    // ================= V3-B : intitulé ≠ capacité =================

    @Nested
    class RoleTitles {
        @Mock RoleTitleRepository titleRepository;
        @Mock RoleRepository roleRepository;

        @Test
        @DisplayName("nœud → défaut tenant → label global, dans cet ordre")
        void resolvesLabelByFallbackChain() {
            RoleTitleService svc = new RoleTitleService(titleRepository, roleRepository);

            // Rien au niveau du nœud, rien en défaut → label global du rôle.
            when(titleRepository.findByTenantIdAndRoleIdAndNodeId(TENANT, ROLE, NODE_CAMPUS))
                    .thenReturn(Optional.empty());
            when(titleRepository.findByTenantIdAndRoleIdAndNodeIdIsNull(TENANT, ROLE))
                    .thenReturn(Optional.empty());
            when(roleRepository.findById(ROLE)).thenReturn(Optional.of(
                    Role.builder().id(ROLE).key("ELDER").label("Ancien").build()));
            assertThat(svc.getEffectiveLabel(TENANT, ROLE, NODE_CAMPUS)).isEqualTo("Ancien");

            // Défaut tenant prend le pas sur le label global.
            when(titleRepository.findByTenantIdAndRoleIdAndNodeIdIsNull(TENANT, ROLE))
                    .thenReturn(Optional.of(RoleTitle.builder().tenantId(TENANT).roleId(ROLE)
                            .label("Diacre").build()));
            assertThat(svc.getEffectiveLabel(TENANT, ROLE, NODE_CAMPUS)).isEqualTo("Diacre");

            // L'intitulé du NŒUD prend le pas sur le défaut tenant.
            when(titleRepository.findByTenantIdAndRoleIdAndNodeId(TENANT, ROLE, NODE_CAMPUS))
                    .thenReturn(Optional.of(RoleTitle.builder().tenantId(TENANT).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).label("Pasteur assistant").build()));
            assertThat(svc.getEffectiveLabel(TENANT, ROLE, NODE_CAMPUS)).isEqualTo("Pasteur assistant");
        }

        @Test
        @DisplayName("upsert ne touche JAMAIS les permissions : seule la capacité doit exister")
        void upsertValidatesRoleOnly() {
            RoleTitleService svc = new RoleTitleService(titleRepository, roleRepository);
            when(roleRepository.existsById(ROLE)).thenReturn(true);
            when(titleRepository.findByTenantIdAndRoleIdAndNodeIdIsNull(TENANT, ROLE))
                    .thenReturn(Optional.empty());
            when(titleRepository.save(any(RoleTitle.class))).thenAnswer(i -> i.getArgument(0));

            RoleTitle saved = svc.upsert(TENANT, ROLE, null, " Ancien ", null);
            assertThat(saved.getLabel()).isEqualTo("Ancien"); // trim
            verify(roleRepository).existsById(ROLE);
            verify(roleRepository, never()).findById(any()); // on ne lit pas les permissions

            assertThatThrownBy(() -> svc.upsert(TENANT, ROLE, null, "  ", null))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ================= V3-C : affiliation multi-nœuds =================

    @Nested
    class Assignments {
        @Mock MemberRoleAssignmentRepository assignmentRepository;
        @Mock OrganizationNodeRepository nodeRepository;
        @Mock RoleRepository roleRepository;

        private MemberRoleAssignmentService svc() {
            return new MemberRoleAssignmentService(assignmentRepository, nodeRepository, roleRepository);
        }

        @Test
        @DisplayName("une assignation posée sur un ANCESTRE couvre le descendant")
        void ancestorCoversDescendant() {
            when(roleRepository.existsById(ROLE)).thenReturn(true);
            when(nodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(
                    node(NODE_CAMPUS, NODE_REGION, "root.region.campus", OrganizationNodeType.CAMPUS,
                            OrganizationNode.ConfigSource.DEFAULT)));
            // Aucune ligne ACTIVE encore → on crée.
            when(assignmentRepository.findByUserIdAndRoleIdAndNodeIdAndStatus(eq(USER), eq(ROLE),
                    eq(NODE_REGION), any())).thenReturn(Optional.empty());
            when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Le membre est « ancien » sur la RÉGION.
            MemberRoleAssignment onRegion = MemberRoleAssignment.builder()
                    .tenantId(TENANT).userId(USER).roleId(ROLE).nodeId(NODE_REGION)
                    .status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build();
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, USER,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(onRegion));
            when(nodeRepository.findById(NODE_REGION)).thenReturn(Optional.of(
                    node(NODE_REGION, NODE_ROOT, "root.region", OrganizationNodeType.REGION,
                            OrganizationNode.ConfigSource.DEFAULT)));

            Set<UUID> covering = svc().roleIdsCoveringNode(TENANT, USER, NODE_CAMPUS);
            assertThat(covering).containsExactly(ROLE);
        }

        @Test
        @DisplayName("un nœud étranger au tenant est invisible (404, pas 403)")
        void foreignNodeIsolation() {
            OrganizationNode foreign = node(NODE_CAMPUS, null, "other", OrganizationNodeType.CAMPUS,
                    OrganizationNode.ConfigSource.DEFAULT);
            foreign.setTenantId(OTHER_TENANT);
            when(roleRepository.existsById(ROLE)).thenReturn(true);
            when(nodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(foreign));

            assertThatThrownBy(() -> svc().assign(TENANT, USER, ROLE, NODE_CAMPUS, USER))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("fin d'assignation = ENDED, jamais une purge")
        void endIsSoft() {
            MemberRoleAssignment active = MemberRoleAssignment.builder()
                    .id(UUID.randomUUID()).tenantId(TENANT).userId(USER).roleId(ROLE).nodeId(NODE_CAMPUS)
                    .status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build();
            when(assignmentRepository.findByTenantIdAndId(TENANT, active.getId()))
                    .thenReturn(Optional.of(active));

            svc().end(TENANT, active.getId());
            assertThat(active.getStatus()).isEqualTo(MemberRoleAssignment.AssignmentStatus.ENDED);
            assertThat(active.getEndedAt()).isNotNull();
            verify(assignmentRepository).save(active);
            verify(assignmentRepository, never()).delete(any());
        }
    }

    // ================= V3-D : modules indépendants par nœud =================

    @Nested
    class NodeFeatures {
        @Mock OrganizationNodeFeatureRepository featureRepository;
        @Mock OrganizationNodeRepository nodeRepository;

        private OrganizationNodeFeatureService svc(ConfigurationResolver resolver) {
            return new OrganizationNodeFeatureService(featureRepository, nodeRepository, resolver);
        }

        @Test
        @DisplayName("un campus sans ligne propre n'hérite PAS de la racine (indépendant par défaut)")
        void childIsIndependentByDefault() {
            ConfigurationResolver resolver = mock(ConfigurationResolver.class);
            when(nodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(
                    node(NODE_CAMPUS, NODE_REGION, "root.region.campus", OrganizationNodeType.CAMPUS,
                            OrganizationNode.ConfigSource.DEFAULT)));
            when(featureRepository.findByNodeIdAndModuleCode(NODE_CAMPUS, "sermons"))
                    .thenReturn(Optional.empty());

            assertThat(svc(resolver).isModuleEnabled(TENANT, NODE_CAMPUS, "sermons")).isFalse();
            // On ne doit JAMAIS remonter la chaîne quand le nœud n'est pas INHERITED.
            verify(nodeRepository, never()).findById(NODE_REGION);
        }

        @Test
        @DisplayName("la remontée n'opère que si le nœud déclare INHERITED")
        void inheritedWalksUpOnlyWhenDeclared() {
            ConfigurationResolver resolver = mock(ConfigurationResolver.class);
            when(nodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(
                    node(NODE_CAMPUS, NODE_REGION, "root.region.campus", OrganizationNodeType.CAMPUS,
                            OrganizationNode.ConfigSource.INHERITED)));
            when(featureRepository.findByNodeIdAndModuleCode(NODE_CAMPUS, "sermons"))
                    .thenReturn(Optional.empty());
            when(nodeRepository.findById(NODE_REGION)).thenReturn(Optional.of(
                    node(NODE_REGION, NODE_ROOT, "root.region", OrganizationNodeType.REGION,
                            OrganizationNode.ConfigSource.DEFAULT)));
            when(featureRepository.findByNodeIdAndModuleCode(NODE_REGION, "sermons"))
                    .thenReturn(Optional.of(OrganizationNodeFeature.builder()
                            .tenantId(TENANT).nodeId(NODE_REGION).moduleCode("sermons").enabled(true).build()));

            assertThat(svc(resolver).isModuleEnabled(TENANT, NODE_CAMPUS, "sermons")).isTrue();
        }

        @Test
        @DisplayName("setModules invalide le cache de config résolue")
        void setModulesInvalidatesCache() {
            ConfigurationResolver resolver = mock(ConfigurationResolver.class);
            when(nodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(
                    node(NODE_CAMPUS, NODE_REGION, "root.region.campus", OrganizationNodeType.CAMPUS,
                            OrganizationNode.ConfigSource.DEFAULT)));
            when(featureRepository.findByTenantIdAndNodeId(TENANT, NODE_CAMPUS)).thenReturn(List.of());
            when(featureRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            svc(resolver).setModules(TENANT, NODE_CAMPUS, List.of(
                    new OrganizationNodeFeatureService.ModuleSelection("sermons", true, Map.of())));
            verify(resolver).invalidateResolvedConfig(TENANT, NODE_CAMPUS);
        }
    }

    // ================= autorisation additive (V3-C × V3-B) =================

    @Nested
    class Authorization {
        @Mock TenantMembershipRepository membershipRepository;
        @Mock PermissionRepository permissionRepository;
        @Mock OrganizationNodeRepository orgNodeRepository;
        @Mock com.discipolat.modules.users.domain.UserRepository userRepository;
        @Mock MemberRoleAssignmentRepository assignmentRepository;

        @Test
        @DisplayName("une permission est accordée via une assignation couvrant le nœud (additif)")
        void assignmentGrantsPermission() {
            AuthorizationService authz = new AuthorizationService(membershipRepository, permissionRepository,
                    orgNodeRepository, userRepository, assignmentRepository);
            // Une appartenance « simple membre » sur le campus, SANS la permission SERMON.
            // L'appartenance = l'appartenance ; la capacité additive vient de l'assignation.
            TenantMembership plainMember = TenantMembership.builder()
                    .tenantId(TENANT).userId(USER)
                    .role(Role.builder().id(ROLE).key("MEMBER").build())
                    .scopeType(MembershipScopeType.CAMPUS).scopeId(NODE_CAMPUS)
                    .status(MembershipStatus.ACTIVE).build();
            when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(USER, TENANT, MembershipStatus.ACTIVE))
                    .thenReturn(List.of(plainMember));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, USER,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(USER).roleId(ROLE)
                            .nodeId(NODE_REGION).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            when(orgNodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(
                    node(NODE_CAMPUS, NODE_REGION, "root.region.campus", OrganizationNodeType.CAMPUS,
                            OrganizationNode.ConfigSource.DEFAULT)));
            when(orgNodeRepository.findById(NODE_REGION)).thenReturn(Optional.of(
                    node(NODE_REGION, NODE_ROOT, "root.region", OrganizationNodeType.REGION,
                            OrganizationNode.ConfigSource.DEFAULT)));
            when(permissionRepository.findByRoleId(ROLE)).thenReturn(List.of(
                    Permission.builder().key("SERMON_CREATE").build()));

            assertThat(authz.can(USER, TENANT, "SERMON_CREATE", MembershipScopeType.CAMPUS, NODE_CAMPUS)).isTrue();
            // Intitulé différent ne change rien : on ne lit jamais un label.
            assertThat(authz.can(USER, TENANT, "sermon_create", MembershipScopeType.CAMPUS, NODE_CAMPUS)).isTrue();
            // Une permission absente de la capacité reste refusée.
            assertThat(authz.can(USER, TENANT, "FINANCE_DELETE", MembershipScopeType.CAMPUS, NODE_CAMPUS)).isFalse();
        }

        @Test
        @DisplayName("scopeId étranger au tenant → aucune couverture par assignation")
        void foreignScopeNoCoverage() {
            AuthorizationService authz = new AuthorizationService(membershipRepository, permissionRepository,
                    orgNodeRepository, userRepository, assignmentRepository);
            when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(USER, TENANT, MembershipStatus.ACTIVE))
                    .thenReturn(List.of(TenantMembership.builder().tenantId(TENANT).userId(USER)
                            .role(Role.builder().key("MEMBER").build()).scopeType(MembershipScopeType.CAMPUS)
                            .scopeId(NODE_CAMPUS).status(MembershipStatus.ACTIVE).build()));
            when(assignmentRepository.findByTenantIdAndUserIdAndStatus(TENANT, USER,
                    MemberRoleAssignment.AssignmentStatus.ACTIVE)).thenReturn(List.of(
                    MemberRoleAssignment.builder().tenantId(TENANT).userId(USER).roleId(ROLE)
                            .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE).build()));
            OrganizationNode foreign = node(NODE_CAMPUS, null, "other.campus", OrganizationNodeType.CAMPUS,
                    OrganizationNode.ConfigSource.DEFAULT);
            foreign.setTenantId(OTHER_TENANT);
            when(orgNodeRepository.findById(NODE_CAMPUS)).thenReturn(Optional.of(foreign));

            assertThat(authz.can(USER, TENANT, "SERMON_CREATE", MembershipScopeType.CAMPUS, NODE_CAMPUS)).isFalse();
        }
    }

    // ================= V3-E / D7 : agrégats sans PII =================

    @Nested
    class Aggregates {
        @Mock OrganizationNodeRepository nodeRepository;
        @Mock TenantMembershipRepository membershipRepository;
        @Mock MemberRoleAssignmentRepository assignmentRepository;
        @Mock NodeAggregateSnapshotRepository snapshotRepository;

        @Test
        @DisplayName("le snapshot compte le sous-arbre et la vue n'expose aucun nom")
        void snapshotCountsSubtreeAndDropsPii() {
            NodeAggregateService svc = new NodeAggregateService(nodeRepository, membershipRepository,
                    assignmentRepository, snapshotRepository);
            OrganizationNode region = node(NODE_REGION, NODE_ROOT, "root.region", OrganizationNodeType.REGION,
                    OrganizationNode.ConfigSource.DEFAULT);
            when(nodeRepository.findById(NODE_REGION)).thenReturn(Optional.of(region));
            when(nodeRepository.findDescendants(eq(TENANT), anyString())).thenReturn(List.of(
                    node(NODE_CAMPUS, NODE_REGION, "root.region.campus", OrganizationNodeType.CAMPUS,
                            OrganizationNode.ConfigSource.DEFAULT),
                    node(UUID.randomUUID(), NODE_REGION, "root.region.other", OrganizationNodeType.ASSEMBLY,
                            OrganizationNode.ConfigSource.DEFAULT)));
            when(membershipRepository.findByTenantIdAndStatus(TENANT, MembershipStatus.ACTIVE))
                    .thenReturn(List.of());
            when(assignmentRepository.findByTenantIdAndStatus(TENANT, MemberRoleAssignment.AssignmentStatus.ACTIVE))
                    .thenReturn(List.of());
            when(snapshotRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(nodeRepository.findRootByTenantId(TENANT)).thenReturn(Optional.of(
                    node(NODE_ROOT, null, "root", OrganizationNodeType.ROOT_CHURCH,
                            OrganizationNode.ConfigSource.DEFAULT)));

            NodeAggregateSnapshot snap = svc.snapshot(TENANT, NODE_REGION);
            // region(ROOT? no) + campus(CAMPUS) + other(ASSEMBLY) → 2 églises/campus dans le sous-arbre.
            assertThat(snap.getChurchCount()).isEqualTo(2L);

            Map<String, Object> view = svc.toView(snap, "Région", "Jean Dupont");
            assertThat(view).containsKeys("memberCount", "churchCount", "leaderCount", "levelName");
            // Aucun champ nominatif de membre (les comptes ne sont que des nombres).
            assertThat(view).doesNotContainKeys("emails", "phones", "members", "userIds");
        }
    }
}
