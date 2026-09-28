package com.discipolat.modules.platform.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.api.CreateDepartmentRequest;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.families.api.CreateFamilyRequest;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.OrganizationNodeStatus;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import com.discipolat.modules.tenants.domain.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformProvisioningServiceTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private SaasPlanRepository planRepository;
    @Mock
    private SaasPlanService saasPlanService;
    @Mock
    private OrganizationNodeService organizationNodeService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private FamilyService familyService;
    @Mock
    private AuditService auditService;
    @Mock
    private TenantOwnerProvisioningService ownerProvisioningService;
    @Mock
    private CrossTenantScopeAccess crossTenant;

    @BeforeEach
    void leScopeCrossTenantExecuteLEBloc() {
        // Le scope est un point d'entree de securite : un mock qui ne deleguerait
        // pas ferait passer le test sans executer le code reel. On le force donc a
        // deleguer, comme le fait la production.
        org.mockito.Mockito.lenient().when(crossTenant.callForTenantSwitch(
                org.mockito.ArgumentMatchers.any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void provisionsTheWholeOrganizationInsideOneServiceCall() {
        SecurityTestHelper.loginAs(UUID.randomUUID());
        UUID tenantId = UUID.randomUUID();
        UUID churchId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID departmentNodeId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        TenantResponse tenant = new TenantResponse(tenantId, "Église Bethel", "eglise-bethel",
                com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE, "free", "CM", "XAF",
                "Africa/Douala", "fr", null, null, null, null, Instant.now(), Instant.now());
        OrganizationNode church = OrganizationNode.builder().id(churchId).tenantId(tenantId)
                .name("Église Bethel").type(OrganizationNodeType.ROOT_CHURCH)
                .code("ROOT_CHURCH_TEST").path("ROOT_CHURCH_TEST").level(0)
                .status(OrganizationNodeStatus.ACTIVE).build();
        Department department = Department.builder().id(departmentId).tenantId(tenantId)
                .nom("Accueil").responsableId(UUID.randomUUID()).build();
        OrganizationNode departmentNode = OrganizationNode.builder().id(departmentNodeId)
                .tenantId(tenantId).name("Accueil").type(OrganizationNodeType.DEPARTMENT)
                .code("DEPARTMENT_TEST").path("ROOT_CHURCH_TEST.DEPARTMENT_TEST").level(1)
                .status(OrganizationNodeStatus.ACTIVE).build();
        Family family = Family.builder().id(familyId).tenantId(tenantId).nom("Famille Test")
                .chefFamilleId(UUID.randomUUID()).build();

        when(tenantService.create(any())).thenReturn(tenant);
        when(organizationNodeService.createRootChurch(eq(tenantId), any(), any(), any())).thenReturn(church);
        when(departmentService.create(any(CreateDepartmentRequest.class))).thenReturn(department);
        when(organizationNodeService.createNode(eq(tenantId), eq(OrganizationNodeType.DEPARTMENT), any(), any(), eq(churchId), eq(department.getResponsableId()), any())).thenReturn(departmentNode);
        when(familyService.create(any(CreateFamilyRequest.class))).thenReturn(family);

        UUID ownerUserId = UUID.randomUUID();
        when(ownerProvisioningService.provisionOwner(eq(tenantId), any(), any(), any(), any()))
                .thenReturn(new TenantOwnerProvisioningService.OwnerProvisioningResult(
                        ownerUserId, "jean@example.com", true, false));

        PlatformProvisioningService service = new PlatformProvisioningService(tenantService, planRepository,
                saasPlanService, organizationNodeService, departmentService, familyService, auditService,
                ownerProvisioningService, crossTenant);
        PlatformProvisioningService.ProvisioningResult result = service.provision(command());

        assertThat(result.tenant()).isEqualTo(tenant);
        assertThat(result.church()).isEqualTo(church);
        assertThat(result.department()).isEqualTo(department);
        assertThat(result.departmentNode()).isEqualTo(departmentNode);
        assertThat(result.family()).isEqualTo(family);
        assertThat(result.owner().userId()).isEqualTo(ownerUserId);
        assertThat(TenantContext.getTenantId()).isNull();
        verify(departmentService).create(any(CreateDepartmentRequest.class));
        verify(familyService).create(any(CreateFamilyRequest.class));
        // L'owner est provisionné APRÈS l'église racine et AVANT département/famille.
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(
                organizationNodeService, ownerProvisioningService, departmentService, familyService);
        order.verify(organizationNodeService).createRootChurch(eq(tenantId), any(), any(), any());
        order.verify(ownerProvisioningService).provisionOwner(eq(tenantId), any(), any(), any(), any());
        order.verify(departmentService).create(any(CreateDepartmentRequest.class));
        order.verify(familyService).create(any(CreateFamilyRequest.class));
    }

    @Test
    @DisplayName("Sans ownerEmail : OWNER_REQUIRED AVANT toute écriture (fail-closed)")
    void refusesToProvisionATenantWithoutOwnerAndWritesNothing() {
        SecurityTestHelper.loginAs(UUID.randomUUID());
        PlatformProvisioningService service = new PlatformProvisioningService(tenantService, planRepository,
                saasPlanService, organizationNodeService, departmentService, familyService, auditService,
                ownerProvisioningService, crossTenant);

        // Constructeur de compatibilité (avant A5) => aucun owner fourni.
        assertThatThrownBy(() -> service.provision(commandWithoutOwner()))
                .isInstanceOf(com.discipolat.common.domain.BusinessRuleException.class)
                .hasMessage("Le propriétaire (owner) de l'église est requis")
                .extracting(thrown -> ((com.discipolat.common.domain.BusinessRuleException) thrown).getCode())
                .isEqualTo("OWNER_REQUIRED");

        // Aucune écriture d'aucune sorte.
        verify(tenantService, never()).create(any());
        verify(ownerProvisioningService, never()).provisionOwner(any(), any(), any(), any(), any());
        verify(organizationNodeService, never()).createRootChurch(any(), any(), any(), any());
        verify(departmentService, never()).create(any(CreateDepartmentRequest.class));
        verify(familyService, never()).create(any(CreateFamilyRequest.class));
        verify(planRepository, never()).findByKeyIgnoreCaseAndIsActiveTrue(any());
    }

    private PlatformProvisioningService.Command commandWithoutOwner() {
        return new PlatformProvisioningService.Command(
                "Église Bethel", "eglise-bethel", "free", "CM", "XAF", "Africa/Douala", "fr",
                "Église Bethel", "Accueil", null, null, true, "Jean", "Mpoudi", "jean@example.com", null,
                "Famille Test", null, null, true, "Pierre", "Mbarga", "pierre@example.com", null,
                null, null, null
        );
    }

    private PlatformProvisioningService.Command command() {
        return new PlatformProvisioningService.Command(
                "Église Bethel", "eglise-bethel", "free", "CM", "XAF", "Africa/Douala", "fr",
                "Église Bethel", "Accueil", null, null, true, "Jean", "Mpoudi", "jean@example.com", null,
                "Famille Test", null, null, true, "Pierre", "Mbarga", "pierre@example.com", null,
                null, null, null,
                "jean@example.com", "Jean", "Mpoudi"
        );
    }
}
