package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.api.CreateDepartmentRequest;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.families.api.CreateFamilyRequest;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import com.discipolat.modules.tenants.domain.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class PlatformProvisioningService {

    private final TenantService tenantService;
    private final SaasPlanRepository planRepository;
    private final SaasPlanService saasPlanService;
    private final OrganizationNodeService organizationNodeService;
    private final DepartmentService departmentService;
    private final FamilyService familyService;
    private final AuditService auditService;

    public PlatformProvisioningService(TenantService tenantService,
                                      SaasPlanRepository planRepository,
                                      SaasPlanService saasPlanService,
                                      OrganizationNodeService organizationNodeService,
                                      DepartmentService departmentService,
                                      FamilyService familyService,
                                      AuditService auditService) {
        this.tenantService = tenantService;
        this.planRepository = planRepository;
        this.saasPlanService = saasPlanService;
        this.organizationNodeService = organizationNodeService;
        this.departmentService = departmentService;
        this.familyService = familyService;
        this.auditService = auditService;
    }

    @Transactional
    public ProvisioningResult provision(Command command) {
        validate(command);
        String plan = command.plan() == null || command.plan().isBlank() ? "free" : command.plan().toLowerCase();
        if (!"free".equals(plan) && planRepository.findById(plan).filter(SaasPlan::getIsActive).isEmpty()) {
            throw new BusinessRuleException("Le plan sélectionné n'existe pas ou n'est pas actif", "INVALID_PLAN");
        }

        UUID actorId = SecurityUtils.getCurrentUserId();
        TenantResponse tenant = tenantService.create(new CreateTenantRequest(
                command.name().trim(),
                command.slug().trim().toLowerCase(),
                plan,
                command.country(),
                command.currency(),
                command.timezone(),
                command.locale(),
                null,
                null,
                null
        ));
        UUID tenantId = tenant.id();
        if (!"free".equals(plan)) {
            saasPlanService.subscribe(tenantId, plan, "monthly", actorId);
        }

        UUID previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            String churchCode = "ROOT_CHURCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            OrganizationNode church = organizationNodeService.createRootChurch(
                    tenantId, command.churchName().trim(), churchCode, actorId);

            Department department = departmentService.create(new CreateDepartmentRequest(
                    command.departmentName().trim(),
                    command.departmentDescription(),
                    command.responsableId(),
                    Boolean.TRUE.equals(command.createNewResponsable()),
                    command.newResponsableFirstName(),
                    command.newResponsableLastName(),
                    command.newResponsableEmail(),
                    command.newResponsablePhone()
            ));
            OrganizationNode departmentNode = organizationNodeService.createNode(
                    tenantId,
                    OrganizationNodeType.DEPARTMENT,
                    department.getNom(),
                    "DEPARTMENT_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    church.getId(),
                    department.getResponsableId(),
                    actorId
            );

            Family family = familyService.create(new CreateFamilyRequest(
                    command.familyName().trim(),
                    command.chefFamilleId(),
                    command.chefAdjointId(),
                    Boolean.TRUE.equals(command.createNewChef()),
                    command.newChefFirstName(),
                    command.newChefLastName(),
                    command.newChefEmail(),
                    command.newChefPhone(),
                    command.newChefSexe(),
                    command.newChefDateNaissance(),
                    command.newChefAdresse()
            ));

            auditService.log(actorId, tenantId, "PROVISIONING_COMPLETED", "PLATFORM", tenantId, "SUCCESS",
                    Map.of("tenantId", tenantId.toString(), "departmentId", department.getId().toString(),
                            "familyId", family.getId().toString()), null, null, null);
            return new ProvisioningResult(tenant, church, department, departmentNode, family);
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    private void validate(Command command) {
        if (command == null || command.name() == null || command.name().isBlank()) {
            throw new BusinessRuleException("Le nom de l'organisation est requis", "NAME_REQUIRED");
        }
        if (command.slug() == null || !command.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new BusinessRuleException("Le slug est invalide", "INVALID_SLUG");
        }
        if (command.churchName() == null || command.churchName().isBlank()
                || command.departmentName() == null || command.departmentName().isBlank()
                || command.familyName() == null || command.familyName().isBlank()) {
            throw new BusinessRuleException("L'église, le département et la famille sont requis", "INCOMPLETE_STRUCTURE");
        }
        if (Boolean.TRUE.equals(command.createNewResponsable())
                && (blank(command.newResponsableFirstName()) || blank(command.newResponsableLastName())
                || blank(command.newResponsableEmail()))) {
            throw new BusinessRuleException("Le nouveau responsable est incomplet", "INCOMPLETE_RESPONSABLE");
        }
        if (Boolean.TRUE.equals(command.createNewChef())
                && (blank(command.newChefFirstName()) || blank(command.newChefLastName())
                || blank(command.newChefEmail()))) {
            throw new BusinessRuleException("Le nouveau chef de famille est incomplet", "INCOMPLETE_CHEF");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record Command(
            String name,
            String slug,
            String plan,
            String country,
            String currency,
            String timezone,
            String locale,
            String churchName,
            String departmentName,
            String departmentDescription,
            UUID responsableId,
            Boolean createNewResponsable,
            String newResponsableFirstName,
            String newResponsableLastName,
            String newResponsableEmail,
            String newResponsablePhone,
            String familyName,
            UUID chefFamilleId,
            UUID chefAdjointId,
            Boolean createNewChef,
            String newChefFirstName,
            String newChefLastName,
            String newChefEmail,
            String newChefPhone,
            String newChefSexe,
            String newChefDateNaissance,
            String newChefAdresse
    ) {}

    public record ProvisioningResult(TenantResponse tenant,
                                     OrganizationNode church,
                                     Department department,
                                     OrganizationNode departmentNode,
                                     Family family) {}
}
