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
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
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
    private final TenantOwnerProvisioningService ownerProvisioningService;

    public PlatformProvisioningService(TenantService tenantService,
                                      SaasPlanRepository planRepository,
                                      SaasPlanService saasPlanService,
                                      OrganizationNodeService organizationNodeService,
                                      DepartmentService departmentService,
                                      FamilyService familyService,
                                      AuditService auditService,
                                      TenantOwnerProvisioningService ownerProvisioningService) {
        this.tenantService = tenantService;
        this.planRepository = planRepository;
        this.saasPlanService = saasPlanService;
        this.organizationNodeService = organizationNodeService;
        this.departmentService = departmentService;
        this.familyService = familyService;
        this.auditService = auditService;
        this.ownerProvisioningService = ownerProvisioningService;
    }

    @Transactional
    public ProvisioningResult provision(Command command) {
        // Fail-closed : l'owner est valide EN TETE, donc AVANT toute ecriture.
        // Un tenant sans propriétaire ne doit jamais exister.
        validate(command);
        requireOwner(command);
        String plan = TenantPlanPolicy.canonicalizePlanKey(command.plan());
        if (plan == null) {
            plan = "DISCOVERY";
        }
        if (!"DISCOVERY".equals(plan)
                && planRepository.findByKeyIgnoreCaseAndIsActiveTrue(plan).isEmpty()) {
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
        if (!"DISCOVERY".equals(plan)) {
            saasPlanService.subscribe(tenantId, plan, "monthly", actorId);
        }

        UUID previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            String churchCode = "ROOT_CHURCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            OrganizationNode church = organizationNodeService.createRootChurch(
                    tenantId, command.churchName().trim(), churchCode, actorId);

            // Étape 3 (A5.2) : le propriétaire est provisionné AVANT les
            // département/famille, car un département et une famille exigent un
            // responsable / chef valide.
            TenantOwnerProvisioningService.OwnerProvisioningResult owner =
                    ownerProvisioningService.provisionOwner(
                            tenantId,
                            command.ownerEmail(),
                            command.ownerFirstName(),
                            command.ownerLastName(),
                            actorId);

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
                            "familyId", family.getId().toString(),
                            "ownerUserId", owner.userId().toString()), null, null, null);
            return new ProvisioningResult(tenant, church, department, departmentNode, family, owner);
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    /**
     * L'email du propriétaire est obligatoire (constat B3, contrat §3.5).
     * Le refus intervient avant toute écriture : aucun tenant créé sans owner,
     * aucune création partielle.
     */
    private void requireOwner(Command command) {
        if (blank(command.ownerEmail())) {
            throw new BusinessRuleException(
                    "Le propriétaire (owner) de l'église est requis", "OWNER_REQUIRED");
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
            String newChefAdresse,
            // Additifs A5 (contrat §3.5) — en fin de record pour ne pas
            // décaler l'index des champs existants.
            String ownerEmail,
            String ownerFirstName,
            String ownerLastName
    ) {
        /** Constructeur de compatibilité (avant A5) : l'owner est alors absent,
         *  ce que {@code provision} refuse explicitement (OWNER_REQUIRED). */
        public Command(
                String name, String slug, String plan, String country, String currency, String timezone,
                String locale, String churchName, String departmentName, String departmentDescription,
                UUID responsableId, Boolean createNewResponsable, String newResponsableFirstName,
                String newResponsableLastName, String newResponsableEmail, String newResponsablePhone,
                String familyName, UUID chefFamilleId, UUID chefAdjointId, Boolean createNewChef,
                String newChefFirstName, String newChefLastName, String newChefEmail, String newChefPhone,
                String newChefSexe, String newChefDateNaissance, String newChefAdresse) {
            this(name, slug, plan, country, currency, timezone, locale, churchName, departmentName,
                    departmentDescription, responsableId, createNewResponsable, newResponsableFirstName,
                    newResponsableLastName, newResponsableEmail, newResponsablePhone, familyName,
                    chefFamilleId, chefAdjointId, createNewChef, newChefFirstName, newChefLastName,
                    newChefEmail, newChefPhone, newChefSexe, newChefDateNaissance, newChefAdresse,
                    null, null, null);
        }
    }

    public record ProvisioningResult(TenantResponse tenant,
                                     OrganizationNode church,
                                     Department department,
                                     OrganizationNode departmentNode,
                                     Family family,
                                     TenantOwnerProvisioningService.OwnerProvisioningResult owner) {

        /** Constructeur de compatibilité (avant A5) : aucun owner provisionné. */
        public ProvisioningResult(TenantResponse tenant, OrganizationNode church, Department department,
                                  OrganizationNode departmentNode, Family family) {
            this(tenant, church, department, departmentNode, family, null);
        }
    }
}
