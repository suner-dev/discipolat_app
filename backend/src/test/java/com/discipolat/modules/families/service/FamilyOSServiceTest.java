package com.discipolat.modules.families.service;

import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.families.repository.FamilyActivityRepository;
import com.discipolat.modules.families.repository.FamilyMeetingRepository;
import com.discipolat.modules.families.repository.FamilyReceptionRepository;
import com.discipolat.modules.families.repository.FamilyVisitRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.people.repository.RoleAssignmentRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G4.1/G4.2 — Garde d'accès par famille (périmètre, pas juste rôle), journal
 * scopé, recherche minimisée/non affectée, scope CAMPUS réel, ajout RÉEL de
 * membre (rattachement + notification + événement temps réel).
 */
@ExtendWith(MockitoExtension.class)
class FamilyOSServiceTest {

    @Mock private FamilyVisitRepository familyVisitRepository;
    @Mock private FamilyReceptionRepository familyReceptionRepository;
    @Mock private FamilyMeetingRepository familyMeetingRepository;
    @Mock private FamilyActivityRepository familyActivityRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private TenantMembershipRepository tenantMembershipRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    @Mock private RoleAssignmentRepository roleAssignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private OutboxPublisher outboxPublisher;

    private FamilyOSService service;

    private UUID tenantId;
    private UUID familyId;
    private UUID chefId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new FamilyOSService(
                familyVisitRepository, familyReceptionRepository, familyMeetingRepository,
                familyActivityRepository, familyRepository, soulRepository, userRepository,
                securityUtils, tenantMembershipRepository, orgNodeRepository,
                roleAssignmentRepository, notificationService, outboxPublisher);
        tenantId = UUID.randomUUID();
        familyId = UUID.randomUUID();
        chefId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, "credentials", List.of()));
    }

    private Family family() {
        return Family.builder()
                .id(familyId).tenantId(tenantId).nom("Famille Lumière")
                .chefFamilleId(chefId).dateCreation(LocalDate.of(2020, 1, 1))
                .build();
    }

    private Soul soul(UUID id, String prenom, String nom, UUID familleId, UUID userId, UUID faiseurId) {
        return Soul.builder()
                .id(id).tenantId(tenantId).prenom(prenom).nom(nom)
                .familleId(familleId).userId(userId).faiseurId(faiseurId)
                .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI)
                .dateIntegration(LocalDate.of(2021, 1, 1))
                .build();
    }

    // ========== G4.1 — GARDE D'ACCÈS PAR FAMILLE ==========

    @Test
    void dashboard_deniedToUnrelatedUserEvenWithRole() {
        authenticateAs(actorId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR", "MEMBRE"));
        when(soulRepository.findAllByFamilleIdAndDeletedFalse(familyId)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.getFamilyDashboard(tenantId, familyId));
    }

    @Test
    void dashboard_allowedForChefDeFamille() {
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));
        when(familyVisitRepository.findByFamilyIdAndVisitDateBetweenAndDeletedFalse(any(), any(), any()))
                .thenReturn(List.of());
        when(familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId)).thenReturn(List.of());
        when(familyReceptionRepository.findByFamilyIdAndReceptionDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(familyMeetingRepository.findByFamilyIdAndMeetingDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> dash = service.getFamilyDashboard(tenantId, familyId);
        assertEquals(familyId, dash.get("familyId"));
    }

    @Test
    void dashboard_allowedForFaiseurOfAFamilySoul() {
        UUID faiseur = UUID.randomUUID();
        authenticateAs(faiseur);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("FAISEUR"));
        when(soulRepository.findAllByFamilleIdAndDeletedFalse(familyId))
                .thenReturn(List.of(soul(UUID.randomUUID(), "Paul", "BAH", familyId, null, faiseur)));
        when(familyVisitRepository.findByFamilyIdAndVisitDateBetweenAndDeletedFalse(any(), any(), any()))
                .thenReturn(List.of());
        when(familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId)).thenReturn(List.of());
        when(familyReceptionRepository.findByFamilyIdAndReceptionDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(familyMeetingRepository.findByFamilyIdAndMeetingDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        assertTrue(service.getFamilyDashboard(tenantId, familyId).containsKey("upcomingVisits"));
    }

    @Test
    void dashboard_allowedForAdminRole() {
        authenticateAs(actorId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("ADMIN"));
        when(familyVisitRepository.findByFamilyIdAndVisitDateBetweenAndDeletedFalse(any(), any(), any()))
                .thenReturn(List.of());
        when(familyVisitRepository.findByFamilyIdAndDeletedFalse(familyId)).thenReturn(List.of());
        when(familyReceptionRepository.findByFamilyIdAndReceptionDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(familyMeetingRepository.findByFamilyIdAndMeetingDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.getFamilyDashboard(tenantId, familyId); // ne lève pas
    }

    @Test
    void dashboard_crossTenantFamilyNotFound() {
        authenticateAs(actorId);
        Family other = family();
        other.setTenantId(UUID.randomUUID());
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(other));

        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> service.getFamilyDashboard(tenantId, familyId));
    }

    // ========== G4.1 — JOURNAL SCOPÉ (fix fuite tenant) ==========

    @Test
    void activities_areScopedToTheFamily_notTheWholeTenant() {
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));
        when(familyActivityRepository.findByTenantIdAndFamilyIdOrderByActivityDateDesc(
                eq(tenantId), eq(familyId), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.getFamilyActivities(tenantId, familyId, 0, 20);

        verify(familyActivityRepository)
                .findByTenantIdAndFamilyIdOrderByActivityDateDesc(eq(tenantId), eq(familyId), any());
        verify(familyActivityRepository, never()).findByTenantIdOrderByActivityDateDesc(any(), any());
    }

    // ========== G4.2 — RECHERCHE MINIMISÉE, NON AFFECTÉS, SCOPE CAMPUS ==========

    @Test
    void searchSouls_returnsOnlyUnassignedAndMinimalFields() {
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));
        Soul unassigned = soul(UUID.randomUUID(), "Paul", "BAH", null, null, chefId);
        Soul inAnotherFamily = soul(UUID.randomUUID(), "Marie", "DUB", UUID.randomUUID(), null, chefId);
        Soul deleted = soul(UUID.randomUUID(), "Jean", "X", null, null, chefId);
        deleted.setDeleted(true);
        when(soulRepository.findByTenantId(tenantId))
                .thenReturn(List.of(unassigned, inAnotherFamily, deleted));

        List<Map<String, Object>> results =
                service.searchSoulsForFamily(tenantId, familyId, null, "CHURCH");

        assertEquals(1, results.size());
        Map<String, Object> row = results.get(0);
        assertEquals(unassigned.getId(), row.get("soulId"));
        assertTrue(row.containsKey("prenom") && row.containsKey("nom"));
        // Minimisation : aucune coordonnée exposée
        assertFalse(row.containsKey("email"));
        assertFalse(row.containsKey("telephone"));
        assertFalse(row.containsKey("adresse"));
    }

    @Test
    void searchSouls_campusScope_filtersByChefCampusSubtree() {
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));

        UUID campusId = UUID.randomUUID();
        UUID onCampusUser = UUID.randomUUID();
        UUID outsideUser = UUID.randomUUID();

        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(chefId, tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(campusMembership(chefId, campusId)));
        when(orgNodeRepository.findDescendantsByNodeId(tenantId, campusId)).thenReturn(List.of());
        when(tenantMembershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        campusMembership(onCampusUser, campusId),
                        campusMembership(outsideUser, UUID.randomUUID())));
        when(roleAssignmentRepository.findByTenantIdAndOrganizationUnitIdAndStatus(tenantId, campusId, "ACTIVE"))
                .thenReturn(List.of());

        Soul inside = soul(UUID.randomUUID(), "Alice", "IN", null, onCampusUser, chefId);
        Soul outside = soul(UUID.randomUUID(), "Bob", "OUT", null, outsideUser, chefId);
        when(soulRepository.findByTenantId(tenantId)).thenReturn(List.of(inside, outside));

        List<Map<String, Object>> results =
                service.searchSoulsForFamily(tenantId, familyId, null, "CAMPUS");

        assertEquals(1, results.size());
        assertEquals(inside.getId(), results.get(0).get("soulId"));
    }

    @Test
    void searchSouls_campusUnresolvable_fallsBackToChurch() {
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(chefId, tenantId, MembershipStatus.ACTIVE))
                .thenReturn(List.of());
        when(soulRepository.findByTenantId(tenantId)).thenReturn(List.of(
                soul(UUID.randomUUID(), "Alice", "A", null, null, chefId)));

        List<Map<String, Object>> results =
                service.searchSoulsForFamily(tenantId, familyId, null, "CAMPUS");
        assertEquals(1, results.size());
    }

    private TenantMembership campusMembership(UUID userId, UUID campusId) {
        return TenantMembership.builder()
                .tenantId(tenantId).userId(userId)
                .scopeType(MembershipScopeType.CAMPUS).scopeId(campusId)
                .status(MembershipStatus.ACTIVE)
                .build();
    }

    // ========== G4.2 — AJOUT RÉEL DE MEMBRE ==========

    @Test
    void addSoulToFamily_actuallyAttachesSoulAndNotifies() {
        UUID soulId = UUID.randomUUID();
        UUID soulUserId = UUID.randomUUID();
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));
        Soul s = soul(soulId, "Paul", "BAH", null, soulUserId, chefId);
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(s));

        service.addSoulToFamily(tenantId, chefId, familyId, soulId, null);

        // Rattachement RÉEL
        ArgumentCaptor<Soul> captor = ArgumentCaptor.forClass(Soul.class);
        verify(soulRepository).save(captor.capture());
        assertEquals(familyId, captor.getValue().getFamilleId());
        // Notification de l'intéressé (IN_APP MEMBRE_AJOUTE)
        verify(notificationService).create(eq(soulUserId), eq(com.discipolat.common.enums.TypeNotification.MEMBRE_AJOUTE),
                eq(com.discipolat.common.enums.CanalNotification.IN_APP), any(), any(), eq(familyId), eq("FAMILY"));
        // Temps réel : FamilyMemberAdded publié sur le tenant
        verify(outboxPublisher).publish(eq(tenantId), eq("FAMILY"), eq(familyId),
                eq("FamilyMemberAdded"), any(Map.class));
    }

    @Test
    void addSoulToFamily_deniedToRandomFamilyMember() {
        UUID memberId = UUID.randomUUID();
        authenticateAs(memberId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("MEMBRE"));
        when(soulRepository.findAllByFamilleIdAndDeletedFalse(familyId))
                .thenReturn(List.of(soul(UUID.randomUUID(), "Me", "MBRE", familyId, memberId, chefId)));

        assertThrows(AccessDeniedException.class,
                () -> service.addSoulToFamily(tenantId, memberId, familyId, UUID.randomUUID(), null));
    }

    @Test
    void addSoulToFamily_crossTenantSoulRejected() {
        UUID soulId = UUID.randomUUID();
        authenticateAs(chefId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family()));
        when(securityUtils.getAllUserRoles()).thenReturn(List.of("CHEF_DE_FAMILLE"));
        Soul foreign = soul(soulId, "Bad", "CTOR", null, null, chefId);
        foreign.setTenantId(UUID.randomUUID());
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(foreign));

        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> service.addSoulToFamily(tenantId, chefId, familyId, soulId, null));
        verify(soulRepository, never()).save(any());
    }
}
