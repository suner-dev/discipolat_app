package com.discipolat.modules.spaces.domain;

import com.discipolat.common.exception.ForbiddenException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * G2.6 — Matrice de droits de customisation des espaces.
 * DoD : « chef de famille ne configure QUE sa famille ; admin configure tout ».
 */
@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock private SpaceRepository spaceRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private ConfigurationResolver configurationResolver;

    private SpaceService spaceService;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID DEPT = UUID.randomUUID();
    private static final UUID FAMILY_A = UUID.randomUUID();
    private static final UUID FAMILY_B = UUID.randomUUID();

    private final UUID admin = UUID.randomUUID();
    private final UUID chefA = UUID.randomUUID();
    private final UUID chefB = UUID.randomUUID();

    private OrganizationNode node(UUID id, String path, UUID responsible) {
        return OrganizationNode.builder()
                .id(id).tenantId(TENANT).name(id.toString()).path(path)
                .level(path.split("\\.").length).responsibleId(responsible).build();
    }

    @BeforeEach
    void setUp() {
        spaceService = new SpaceService(spaceRepository, organizationNodeRepository,
                membershipRepository, authorizationService, auditService, eventPublisher,
                propagationPublisher, configurationResolver);
    }

    private Space space(UUID id, UUID orgUnit, SpaceType type, String code) {
        return Space.builder().id(id).tenantId(TENANT).organizationUnitId(orgUnit)
                .spaceType(type).code(code).name(code).build();
    }

    private TenantMembership membership(String roleKey, UUID scopeId) {
        Role role = Role.builder().key(roleKey).label(roleKey).tenantId(TENANT).build();
        return TenantMembership.builder().tenantId(TENANT).scopeId(scopeId).role(role)
                .status(MembershipStatus.ACTIVE).build();
    }

    @Test
    void chefDeFamille_neConfigureQueSaFamille() {
        OrganizationNode dept = node(DEPT, "root.dept", null);
        OrganizationNode familyA = node(FAMILY_A, "root.dept.fam-a", chefA);
        OrganizationNode familyB = node(FAMILY_B, "root.dept.fam-b", chefB);

        when(authorizationService.isPlatformSuperAdmin(chefA)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(chefA, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership("CHEF_DE_FAMILLE", FAMILY_A)));
        when(organizationNodeRepository.findById(FAMILY_A)).thenReturn(Optional.of(familyA));
        when(organizationNodeRepository.findByTenantId(TENANT)).thenReturn(List.of(dept, familyA, familyB));

        Space spaceA = space(UUID.randomUUID(), FAMILY_A, SpaceType.FAMILY, "FAM_A");
        Space spaceB = space(UUID.randomUUID(), FAMILY_B, SpaceType.FAMILY, "FAM_B");

        assertTrue(spaceService.canCustomize(chefA, spaceA), "le chef configure sa famille");
        assertFalse(spaceService.canCustomize(chefA, spaceB), "le chef ne configure PAS une autre famille");
    }

    @Test
    void admin_configureTout() {
        when(authorizationService.isPlatformSuperAdmin(admin)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(admin, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership("ADMIN", null)));

        Space anySpace = space(UUID.randomUUID(), FAMILY_B, SpaceType.FAMILY, "FAM_B");

        assertTrue(spaceService.canCustomize(admin, anySpace));
    }

    @Test
    void superAdminPlateforme_configureTout() {
        when(authorizationService.isPlatformSuperAdmin(admin)).thenReturn(true);

        Space anySpace = space(UUID.randomUUID(), DEPT, SpaceType.DEPARTMENT, "DEPT");

        assertTrue(spaceService.canCustomize(admin, anySpace));
    }

    @Test
    void responsableDUnAncetre_configureLaSousUnite() {
        OrganizationNode dept = node(DEPT, "root.dept", chefA);
        OrganizationNode family = node(FAMILY_A, "root.dept.fam-a", null);

        when(authorizationService.isPlatformSuperAdmin(chefA)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(chefA, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of());
        when(organizationNodeRepository.findById(FAMILY_A)).thenReturn(Optional.of(family));
        when(organizationNodeRepository.findByTenantId(TENANT)).thenReturn(List.of(dept, family));

        Space spaceFamily = space(UUID.randomUUID(), FAMILY_A, SpaceType.FAMILY, "FAM_A");

        assertTrue(spaceService.canCustomize(chefA, spaceFamily),
                "le responsable du département configure la famille qu'il héberge");
    }

    @Test
    void updateSpace_refuseSiNonAutorise() {
        Space spaceB = space(UUID.randomUUID(), FAMILY_B, SpaceType.FAMILY, "FAM_B");
        when(spaceRepository.findByIdAndTenantId(spaceB.getId(), TENANT)).thenReturn(Optional.of(spaceB));
        when(authorizationService.isPlatformSuperAdmin(chefA)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(chefA, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership("CHEF_DE_FAMILLE", FAMILY_A)));
        OrganizationNode familyB = node(FAMILY_B, "root.dept.fam-b", chefB);
        when(organizationNodeRepository.findById(FAMILY_B)).thenReturn(Optional.of(familyB));
        when(organizationNodeRepository.findByTenantId(TENANT)).thenReturn(List.of(familyB));

        SpaceService.SpaceCommand cmd = new SpaceService.SpaceCommand(
                null, null, null, "Nouveau nom", null, null, "#ff0000", null, null, null, null);

        assertThrows(ForbiddenException.class,
                () -> spaceService.updateSpace(TENANT, chefA, spaceB.getId(), cmd));
        verify(spaceRepository, never()).save(any());
    }

    @Test
    void updateSpace_autoriseEtPublieEvenementTempsReel() {
        Space spaceA = space(UUID.randomUUID(), FAMILY_A, SpaceType.FAMILY, "FAM_A");
        when(spaceRepository.findByIdAndTenantId(spaceA.getId(), TENANT)).thenReturn(Optional.of(spaceA));
        when(authorizationService.isPlatformSuperAdmin(chefA)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(chefA, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership("CHEF_DE_FAMILLE", FAMILY_A)));
        OrganizationNode familyA = node(FAMILY_A, "root.dept.fam-a", chefA);
        when(organizationNodeRepository.findById(FAMILY_A)).thenReturn(Optional.of(familyA));
        when(organizationNodeRepository.findByTenantId(TENANT)).thenReturn(List.of(familyA));
        when(spaceRepository.save(any(Space.class))).thenAnswer(inv -> inv.getArgument(0));

        SpaceService.SpaceCommand cmd = new SpaceService.SpaceCommand(
                null, null, null, null, null, null, "#123456", null, null, null, null);

        Space updated = spaceService.updateSpace(TENANT, chefA, spaceA.getId(), cmd);

        assertEquals("#123456", updated.getColor());
        verify(eventPublisher).publishEvent(any(SpaceConfigChangedEvent.class));
    }

    @Test
    void resolveCustomizableSpaceIds_restreintPourChef() {
        Space spaceA = space(UUID.randomUUID(), FAMILY_A, SpaceType.FAMILY, "FAM_A");
        Space spaceB = space(UUID.randomUUID(), FAMILY_B, SpaceType.FAMILY, "FAM_B");
        when(spaceRepository.findByTenantIdAndDeletedAtIsNull(TENANT)).thenReturn(List.of(spaceA, spaceB));
        when(authorizationService.isPlatformSuperAdmin(chefA)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(chefA, TENANT, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership("CHEF_DE_FAMILLE", FAMILY_A)));
        OrganizationNode familyA = node(FAMILY_A, "root.dept.fam-a", chefA);
        when(organizationNodeRepository.findById(FAMILY_A)).thenReturn(Optional.of(familyA));
        when(organizationNodeRepository.findByTenantId(TENANT)).thenReturn(List.of(familyA));
        OrganizationNode familyB = node(FAMILY_B, "root.dept.fam-b", chefB);
        when(organizationNodeRepository.findById(FAMILY_B)).thenReturn(Optional.of(familyB));

        List<UUID> ids = spaceService.resolveCustomizableSpaceIds(chefA, TENANT);

        assertEquals(Set.of(spaceA.getId()), Set.copyOf(ids));
    }
}