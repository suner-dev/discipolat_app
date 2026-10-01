package com.discipolat.modules.spaces.domain;

import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.config.domain.SpaceTemplate;
import com.discipolat.modules.config.repository.SpaceTemplateRepository;
import com.discipolat.modules.customfields.domain.CustomFieldDefinition;
import com.discipolat.modules.customfields.domain.CustomFieldService;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.domain.SpaceMembership;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.statuses.domain.CustomStatusService;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.TenantFeature;
import com.discipolat.modules.tenants.domain.TenantFeatureService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * G5.3 — Le bootstrap génère l'expérience depuis la CONFIGURATION de l'espace :
 * deux espaces = deux expériences distinctes depuis la même base de code ;
 * la surcharge de l'espace bat le template ; les modules désactivés au niveau
 * tenant sont signalés enabled=false (jamais masqués silencieusement).
 */
@ExtendWith(MockitoExtension.class)
class SpaceBootstrapServiceTest {

    @Mock private SpaceService spaceService;
    @Mock private SpaceTemplateRepository templateRepository;
    @Mock private TenantFeatureService tenantFeatureService;
    @Mock private CustomStatusService statusService;
    @Mock private CustomFieldService customFieldService;
    @Mock private SpaceMembershipRepository spaceMembershipRepository;
    @Mock private PersonRepository personRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private TenantMembershipRepository tenantMembershipRepository;

    private SpaceBootstrapService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SPACE_A = UUID.randomUUID();
    private static final UUID SPACE_B = UUID.randomUUID();
    private static final UUID PERSON_1 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SpaceBootstrapService(
                spaceService, templateRepository, tenantFeatureService, statusService,
                customFieldService, spaceMembershipRepository, personRepository,
                authorizationService, tenantMembershipRepository);
        SecurityTestHelper.loginAs(ACTOR, "ADMIN");
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.logout();
    }

    private Space space(UUID id, SpaceType type, String templateCode, Map<String, Object> config) {
        return Space.builder()
                .id(id).tenantId(TENANT).spaceType(type).templateCode(templateCode)
                .name("Espace " + type).code("SP-" + type)
                .status(SpaceStatus.ACTIVE).visiblePeopleScope(VisiblePeopleScope.CHURCH)
                .configurationJson(new java.util.LinkedHashMap<>(config))
                .build();
    }

    private TenantFeature feature(String code) {
        return TenantFeature.builder().moduleCode(code).enabled(true).build();
    }

    @Test
    void twoSpacesWithDifferentConfigsProduceDifferentExperiences() {
        // Espace A : template audiovisuel (ÉQUIPEMENTS + ÉVÉNEMENTS), pas de surcharge.
        Space a = space(SPACE_A, SpaceType.DEPARTMENT, "TPL_AUDIOVISUEL", Map.of());
        // Espace B : même type d'espace, surcharge config → modules différents + widgets verrouillés.
        Space b = space(SPACE_B, SpaceType.DEPARTMENT, "TPL_AUDIOVISUEL", Map.of(
                "modules", List.of("TACHES"),
                "widgetsLocked", true));

        when(spaceService.getSpace(TENANT, SPACE_A)).thenReturn(a);
        when(spaceService.getSpace(TENANT, SPACE_B)).thenReturn(b);
        when(templateRepository.findByCode("TPL_AUDIOVISUEL")).thenReturn(Optional.of(
                SpaceTemplate.builder().code("TPL_AUDIOVISUEL").name("Audiovisuel")
                        .modulesJson(List.of("EQUIPEMENTS", "EVENEMENTS"))
                        .defaultDashboardsJson(List.of(Map.of("type", "MATERIEL_SORTI")))
                        .defaultWorkflowsJson(List.of()).defaultStatusesJson(List.of())
                        .build()));
        when(tenantFeatureService.getEnabledFeatures(TENANT))
                .thenReturn(List.of(feature("EQUIPEMENTS"), feature("EVENEMENTS"), feature("TACHES")));
        when(statusService.getStatusBoard(eq(TENANT), anyString(), any())).thenReturn(List.of());
        when(customFieldService.getDefinitions(anyString())).thenReturn(List.of());
        when(spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(TENANT, SPACE_A, "ACTIVE"))
                .thenReturn(List.of());
        when(spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(TENANT, SPACE_B, "ACTIVE"))
                .thenReturn(List.of());
        when(spaceService.canCustomize(any(), any())).thenReturn(true);
        when(authorizationService.getCurrentUserPermissions()).thenReturn(Set.of("SPACE_UPDATE"));
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), eq(TENANT), any()))
                .thenReturn(List.of());

        SpaceBootstrapService.SpaceBootstrap bootA = service.bootstrap(TENANT, SPACE_A, null);
        SpaceBootstrapService.SpaceBootstrap bootB = service.bootstrap(TENANT, SPACE_B, null);

        assertEquals(List.of("EQUIPEMENTS", "EVENEMENTS"),
                bootA.modules().stream().map(SpaceBootstrapService.ModuleBootstrap::code).toList());
        assertEquals(List.of("TACHES"),
                bootB.modules().stream().map(SpaceBootstrapService.ModuleBootstrap::code).toList());
        assertNotEquals(bootA.modules(), bootB.modules());
        assertFalse(bootA.widgetsLocked());
        assertTrue(bootB.widgetsLocked());
    }

    @Test
    void templateModulesDisabledAtTenantLevelAreReportedNotHidden() {
        Space a = space(SPACE_A, SpaceType.DEPARTMENT, "TPL_FINANCE", Map.of());
        when(spaceService.getSpace(TENANT, SPACE_A)).thenReturn(a);
        when(templateRepository.findByCode("TPL_FINANCE")).thenReturn(Optional.of(
                SpaceTemplate.builder().code("TPL_FINANCE").name("Finance")
                        .modulesJson(List.of("FINANCE", "PAIEMENTS"))
                        .defaultDashboardsJson(List.of()).defaultWorkflowsJson(List.of())
                        .defaultStatusesJson(List.of()).build()));
        // PAIEMENTS désactivé au niveau tenant → présent mais enabled=false (pas de bouton mort).
        when(tenantFeatureService.getEnabledFeatures(TENANT)).thenReturn(List.of(feature("FINANCE")));
        when(statusService.getStatusBoard(eq(TENANT), anyString(), any())).thenReturn(List.of());
        when(customFieldService.getDefinitions(anyString())).thenReturn(List.of());
        when(spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(TENANT, SPACE_A, "ACTIVE"))
                .thenReturn(List.of());
        when(spaceService.canCustomize(any(), any())).thenReturn(false);
        when(authorizationService.getCurrentUserPermissions()).thenReturn(Set.of());
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), eq(TENANT), any()))
                .thenReturn(List.of());

        var boot = service.bootstrap(TENANT, SPACE_A, null);
        var byCode = boot.modules().stream()
                .collect(java.util.stream.Collectors.toMap(SpaceBootstrapService.ModuleBootstrap::code,
                        SpaceBootstrapService.ModuleBootstrap::enabled));
        assertEquals(Map.of("FINANCE", true, "PAIEMENTS", false), byCode);
    }

    @Test
    void entityTypeDefaultsToSpaceTypeAndIsOverridable() {
        Space fam = space(SPACE_A, SpaceType.FAMILY, null, Map.of());
        when(spaceService.getSpace(TENANT, SPACE_A)).thenReturn(fam);
        when(tenantFeatureService.getEnabledFeatures(TENANT)).thenReturn(List.of(feature("TACHES")));
        when(statusService.getStatusBoard(eq(TENANT), anyString(), any())).thenReturn(List.of());
        when(customFieldService.getDefinitions(anyString())).thenReturn(List.of());
        when(spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(TENANT, SPACE_A, "ACTIVE"))
                .thenReturn(List.of());
        when(spaceService.canCustomize(any(), any())).thenReturn(false);
        when(authorizationService.getCurrentUserPermissions()).thenReturn(Set.of());
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), eq(TENANT), any()))
                .thenReturn(List.of());

        var bootDefault = service.bootstrap(TENANT, SPACE_A, null);
        assertEquals("FAMILY", bootDefault.entityType());
        verify(customFieldService).getDefinitions("FAMILY");

        var bootOverride = service.bootstrap(TENANT, SPACE_A, "  VISITE  ");
        assertEquals("VISITE", bootOverride.entityType());
        verify(customFieldService).getDefinitions("VISITE");
    }

    @Test
    void membersPreviewCarriesRealNamesAndTotalCount() {
        Space a = space(SPACE_A, SpaceType.DEPARTMENT, null, Map.of());
        when(spaceService.getSpace(TENANT, SPACE_A)).thenReturn(a);
        when(tenantFeatureService.getEnabledFeatures(TENANT)).thenReturn(List.of());
        when(statusService.getStatusBoard(eq(TENANT), anyString(), any())).thenReturn(List.of());
        when(customFieldService.getDefinitions(anyString())).thenReturn(List.of());
        when(spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(TENANT, SPACE_A, "ACTIVE"))
                .thenReturn(List.of(
                        SpaceMembership.builder().id(UUID.randomUUID()).tenantId(TENANT)
                                .personId(PERSON_1).spaceId(SPACE_A).status("ACTIVE")
                                .membershipType("RESPONSABLE").build()));
        when(personRepository.findAllById(anyIterable())).thenReturn(List.of(
                Person.builder().id(PERSON_1).tenantId(TENANT)
                        .firstName("Marie").lastName("Dupont").build()));
        when(spaceService.canCustomize(any(), any())).thenReturn(true);
        when(authorizationService.getCurrentUserPermissions()).thenReturn(Set.of("ORG_NODE_UPDATE"));
        Role role = new Role();
        role.setKey("ADMIN");
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(ACTOR, TENANT,
                com.discipolat.modules.tenants.domain.MembershipStatus.ACTIVE))
                .thenReturn(List.of(TenantMembership.builder().role(role).build()));

        var boot = service.bootstrap(TENANT, SPACE_A, null);
        assertEquals(1, boot.memberCount());
        assertEquals("Marie Dupont", boot.membersPreview().get(0).fullName());
        assertEquals("RESPONSABLE", boot.membersPreview().get(0).membershipType());
        assertTrue(boot.permissions().canCustomize());
        assertTrue(boot.permissions().roleKeys().contains("ADMIN"));
    }

    @Test
    void customFieldsFlowFromG24WithServerSideRoleFiltering() {
        Space a = space(SPACE_A, SpaceType.DEPARTMENT, null, Map.of());
        when(spaceService.getSpace(TENANT, SPACE_A)).thenReturn(a);
        when(tenantFeatureService.getEnabledFeatures(TENANT)).thenReturn(List.of());
        when(statusService.getStatusBoard(eq(TENANT), anyString(), any())).thenReturn(List.of());
        CustomFieldDefinition def = new CustomFieldDefinition();
        def.setCode("NIVEAU_MATERNEL");
        def.setLabel("Niveau maternel");
        def.setType("PICKLIST");
        def.setObligatoire(true);
        def.setOptions(List.of("1", "2", "3"));
        when(customFieldService.getDefinitions("DEPARTMENT")).thenReturn(List.of(def));
        when(spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(TENANT, SPACE_A, "ACTIVE"))
                .thenReturn(List.of());
        when(spaceService.canCustomize(any(), any())).thenReturn(false);
        when(authorizationService.getCurrentUserPermissions()).thenReturn(Set.of());
        when(tenantMembershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), eq(TENANT), any()))
                .thenReturn(List.of());

        var boot = service.bootstrap(TENANT, SPACE_A, null);
        assertEquals(1, boot.customFields().size());
        assertEquals("NIVEAU_MATERNEL", boot.customFields().get(0).key());
        assertTrue(boot.customFields().get(0).required());
        assertEquals(List.of("1", "2", "3"), boot.customFields().get(0).options());
    }
}
