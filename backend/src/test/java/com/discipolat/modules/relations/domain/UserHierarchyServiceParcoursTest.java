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
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.RoleTitleService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Axe 2 — « suivi pastoral temporel ». Couvre la composition EN LECTURE SEULE
 * du parcours ({@link UserHierarchyService#parcours}) à partir des seules tables
 * existantes : adhésions, transferts tracés, capacités (début/fin) et
 * rattachements déclarés/rompus. Vérifie l'ordre chronologique, le résumé par
 * nature, l'isolation multi-tenant stricte (404, jamais de fuite) et le fait
 * que rien n'est écrit ni réécrit.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserHierarchyServiceParcoursTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTRE_TENANT = UUID.randomUUID();
    private static final UUID MEMBRE = UUID.randomUUID();
    private static final UUID ROLE = UUID.randomUUID();
    private static final UUID NODE_CAMPUS = UUID.randomUUID();

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

    private static User membre() {
        return User.builder().id(MEMBRE).tenantId(TENANT).firstName("Awa").lastName("Diallo")
                .email("awa@x.org").role(UserRole.MEMBRE).roles(EnumSet.of(UserRole.MEMBRE))
                .statut(UserStatus.ACTIVE).build();
    }

    private static OrganizationNode campus() {
        return OrganizationNode.builder().id(NODE_CAMPUS).tenantId(TENANT).parentId(null)
                .path("ROOT.C1").level(1).type(OrganizationNodeType.CAMPUS).name("Campus Nord").build();
    }

    private void givenMembre() {
        when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(membre()));
        when(nodeRepository.findByTenantId(TENANT)).thenReturn(List.of(campus()));
    }

    private void noMemberships() {
        when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of());
    }

    private void noAssignments() {
        when(assignmentRepository.findByTenantIdAndUserId(TENANT, MEMBRE)).thenReturn(List.of());
    }

    private void noRelations() {
        when(relationService.relationEvents(TENANT, MEMBRE)).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> events(Map<String, Object> parcours) {
        return (List<Map<String, Object>>) parcours.get("evenements");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resume(Map<String, Object> parcours) {
        return (Map<String, Object>) parcours.get("resume");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parType(Map<String, Object> parcours) {
        return (Map<String, Object>) resume(parcours).get("parType");
    }

    @Test
    @DisplayName("récit chronologique : adhésion → capacité → rattachement, tri ascendant")
    void parcoursChronologique() {
        givenMembre();
        when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of(
                TenantMembership.builder().tenantId(TENANT).userId(MEMBRE)
                        .scopeType(MembershipScopeType.TENANT).status(MembershipStatus.ACTIVE)
                        .joinedAt(Instant.parse("2020-01-01T00:00:00Z")).build()));
        when(assignmentRepository.findByTenantIdAndUserId(TENANT, MEMBRE)).thenReturn(List.of(
                MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                        .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE)
                        .assignedAt(Instant.parse("2021-06-01T00:00:00Z")).build()));
        when(roleRepository.findAllById(any())).thenReturn(List.of(
                Role.builder().id(ROLE).key("BERGER").label("Berger").build()));
        when(relationService.relationEvents(TENANT, MEMBRE)).thenReturn(List.of(
                relation("ACTIVE", "Mon pasteur", "Jean Maka", "2022-03-01T00:00:00Z", null)));

        Map<String, Object> p = svc().parcours(TENANT, MEMBRE, MEMBRE);

        assertThat(p.get("nomComplet")).isEqualTo("Awa Diallo");
        assertThat(p.get("depuis")).isEqualTo("2020-01-01T00:00:00Z");
        assertThat(p.get("totalEvenements")).isEqualTo(3);

        List<Map<String, Object>> ev = events(p);
        assertThat(ev).extracting(e -> e.get("type"))
                .containsExactly("ADHESION", "CAPACITE", "RATTACHEMENT");
        assertThat(ev).allSatisfy(e -> assertThat(e.get("action")).isEqualTo("DEBUT"));
        assertThat((String) ev.get(1).get("libelle")).contains("Berger").contains("Campus Nord");
        assertThat((String) ev.get(2).get("libelle")).contains("Mon pasteur").contains("Jean Maka");

        assertThat(parType(p)).containsEntry("ADHESION", 1L)
                .containsEntry("CAPACITE", 1L).containsEntry("RATTACHEMENT", 1L);
        assertThat(resume(p)).containsEntry("transferts", 0L)
                .containsEntry("rattachementsRompus", 0L);
    }

    @Test
    @DisplayName("transfert tracé : l'appartenance est racontée, pas effacée (ADHESION + TRANSFERT FIN)")
    void transfertRaconte() {
        givenMembre();
        noAssignments();
        noRelations();
        when(membershipRepository.findAllByUserIdAndTenantId(MEMBRE, TENANT)).thenReturn(List.of(
                TenantMembership.builder().tenantId(TENANT).userId(MEMBRE)
                        .scopeType(MembershipScopeType.TENANT).status(MembershipStatus.REVOKED)
                        .joinedAt(Instant.parse("2019-01-01T00:00:00Z"))
                        .transferredAt(Instant.parse("2020-06-01T00:00:00Z"))
                        .transferredToTenantId(AUTRE_TENANT).transferReason("Déménagement").build()));

        Map<String, Object> p = svc().parcours(TENANT, MEMBRE, MEMBRE);

        List<Map<String, Object>> ev = events(p);
        assertThat(ev).hasSize(2);
        assertThat(ev.get(0).get("type")).isEqualTo("ADHESION");
        assertThat(ev.get(1).get("type")).isEqualTo("TRANSFERT");
        assertThat(ev.get(1).get("action")).isEqualTo("FIN");
        assertThat((String) ev.get(1).get("libelle")).contains("Déménagement");
        assertThat(resume(p)).containsEntry("transferts", 1L);
    }

    @Test
    @DisplayName("capacité finie + rattachement rompu : événements FIN émis, PAS de fin pour un actif daté")
    void finCapaciteEtRupture() {
        givenMembre();
        noMemberships();
        when(assignmentRepository.findByTenantIdAndUserId(TENANT, MEMBRE)).thenReturn(List.of(
                // Terminée : doit produire DEBUT + FIN.
                MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                        .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ENDED)
                        .assignedAt(Instant.parse("2021-01-01T00:00:00Z"))
                        .endedAt(Instant.parse("2023-01-01T00:00:00Z")).build(),
                // Actif avec endedAt résiduel : ne doit PAS produire de FIN (garde status != ACTIVE).
                MemberRoleAssignment.builder().tenantId(TENANT).userId(MEMBRE).roleId(ROLE)
                        .nodeId(NODE_CAMPUS).status(MemberRoleAssignment.AssignmentStatus.ACTIVE)
                        .assignedAt(Instant.parse("2024-01-01T00:00:00Z"))
                        .endedAt(Instant.parse("2024-06-01T00:00:00Z")).build()));
        when(roleRepository.findAllById(any())).thenReturn(List.of(
                Role.builder().id(ROLE).key("DIACRE").label("Diacre").build()));
        when(relationService.relationEvents(TENANT, MEMBRE)).thenReturn(List.of(
                relation("REVOKED", "Mon pasteur", "Jean Maka", "2022-03-01T00:00:00Z", "2025-01-01T00:00:00Z")));

        Map<String, Object> p = svc().parcours(TENANT, MEMBRE, MEMBRE);
        List<Map<String, Object>> ev = events(p);

        long capaciteDebut = ev.stream().filter(e -> "CAPACITE".equals(e.get("type"))
                && "DEBUT".equals(e.get("action"))).count();
        long capaciteFin = ev.stream().filter(e -> "CAPACITE".equals(e.get("type"))
                && "FIN".equals(e.get("action"))).count();
        assertThat(capaciteDebut).isEqualTo(2);
        assertThat(capaciteFin).isEqualTo(1); // seulement l'ENDED, pas l'ACTIF daté

        assertThat(ev).anyMatch(e -> "RATTACHEMENT".equals(e.get("type")) && "FIN".equals(e.get("action")));
        assertThat(resume(p)).containsEntry("rattachementsRompus", 1L);
    }

    @Test
    @DisplayName("membre d'une autre église : 404 logique, aucun rechargement en aval (pas de fuite)")
    void isolationMultiTenant() {
        when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(
                User.builder().id(MEMBRE).tenantId(AUTRE_TENANT).firstName("Awa").lastName("Diallo")
                        .email("awa@x.org").role(UserRole.MEMBRE).roles(EnumSet.of(UserRole.MEMBRE))
                        .statut(UserStatus.ACTIVE).build()));

        assertThatThrownBy(() -> svc().parcours(TENANT, MEMBRE, MEMBRE))
                .isInstanceOf(EntityNotFoundException.class);
        verify(relationService, never()).relationEvents(any(), any());
        verify(assignmentRepository, never()).findByTenantIdAndUserId(any(), any());
    }

    /** Une ligne brute telle que produite par {@link MemberRelationService#relationEvents}. */
    private static Map<String, Object> relation(String statut, String typeLabel, String enversNom,
                                                String declareLe, String termineLe) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("relationType", "PASTEUR");
        m.put("typeLabel", typeLabel);
        m.put("enversId", UUID.randomUUID());
        m.put("enversNom", enversNom);
        m.put("statut", statut);
        m.put("note", null);
        m.put("declareLe", declareLe);
        m.put("termineLe", termineLe);
        return m;
    }
}
