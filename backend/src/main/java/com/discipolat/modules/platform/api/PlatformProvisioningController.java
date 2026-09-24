package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.domain.PlatformProvisioningService;
import com.discipolat.modules.departments.api.CreateDepartmentRequest;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.families.api.CreateFamilyRequest;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Provisionnement guidé Super Admin — flux pas à pas :
 *   1. Tenant      → POST /api/v1/platform/admin/tenants (SuperAdminController, existant)
 *   2. Église      → POST /api/v1/platform/admin/provisioning/church      (ci-dessous)
 *   3. Département → POST /api/v1/platform/admin/provisioning/department  (ci-dessous)
 *   4. Famille     → POST /api/v1/platform/admin/provisioning/family      (ci-dessous)
 *
 * Chaque étape exécute la logique métier existante (aucune duplication) dans le
 * contexte du tenant cible via TenantContext (l'auto-listener Hibernate rattache
 * alors tenant_id aux entités créées), puis journalise l'action en audit avec
 * l'identité réelle du super admin.
 */
@RestController
@RequestMapping("/api/v1/platform/admin/provisioning")
public class PlatformProvisioningController {

    private final TenantRepository tenantRepository;
    private final OrganizationNodeService organizationNodeService;
    private final DepartmentService departmentService;
    private final FamilyService familyService;
    private final AuditService auditService;
    private final PlatformProvisioningService provisioningService;

    public PlatformProvisioningController(TenantRepository tenantRepository,
                                          OrganizationNodeService organizationNodeService,
                                           DepartmentService departmentService,
                                           FamilyService familyService,
                                           AuditService auditService,
                                           PlatformProvisioningService provisioningService) {
        this.tenantRepository = tenantRepository;
        this.organizationNodeService = organizationNodeService;
        this.departmentService = departmentService;
        this.familyService = familyService;
        this.auditService = auditService;
        this.provisioningService = provisioningService;
    }

    @PostMapping
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> provisionOrganization(@RequestBody AtomicProvisioningRequest request) {
        PlatformProvisioningService.ProvisioningResult result = provisioningService.provision(
                new PlatformProvisioningService.Command(
                        request.name(), request.slug(), request.plan(), request.country(), request.currency(),
                        request.timezone(), request.locale(), request.churchName(), request.departmentName(),
                        request.departmentDescription(), request.responsableId(), request.createNewResponsable(),
                        request.newResponsableFirstName(), request.newResponsableLastName(),
                        request.newResponsableEmail(), request.newResponsablePhone(), request.familyName(),
                        request.chefFamilleId(), request.chefAdjointId(), request.createNewChef(),
                        request.newChefFirstName(), request.newChefLastName(), request.newChefEmail(),
                        request.newChefPhone(), request.newChefSexe(), request.newChefDateNaissance(),
                        request.newChefAdresse()
                ));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant", result.tenant());
        body.put("church", toNodeMap(result.church()));
        body.put("department", Map.of(
                "id", result.department().getId().toString(),
                "tenantId", result.department().getTenantId().toString(),
                "nom", result.department().getNom(),
                "responsableId", result.department().getResponsableId().toString()
        ));
        body.put("departmentNode", toNodeMap(result.departmentNode()));
        body.put("family", Map.of(
                "id", result.family().getId().toString(),
                "tenantId", result.family().getTenantId().toString(),
                "nom", result.family().getNom(),
                "chefFamilleId", result.family().getChefFamilleId().toString()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ==================== ÉTAPE 2 : ÉGLISE (ROOT_CHURCH) ====================

    @PostMapping("/church")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> provisionChurch(@RequestBody ChurchProvisionRequest request) {
        if (request.tenantId() == null) {
            return badRequest("tenantId est requis", "tenantId");
        }
        if (request.name() == null || request.name().isBlank()) {
            return badRequest("Le nom de l'église est requis", "name");
        }
        Tenant tenant = requireTenant(request.tenantId());
        UUID actorId = SecurityUtils.getCurrentUserId();
        String code = "ROOT_CHURCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrganizationNode node = withTenantContext(request.tenantId(), () ->
                organizationNodeService.createRootChurch(
                        tenant.getId(), request.name().trim(), code, actorId));

        auditService.log(actorId, tenant.getId(), "PROVISIONING_CHURCH_CREATED", "ORGANIZATION_NODE",
                node.getId(), "SUCCESS",
                Map.of("tenantId", tenant.getId().toString(), "name", node.getName(), "code", code),
                null, null, null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", node.getId().toString());
        body.put("tenantId", tenant.getId().toString());
        body.put("name", node.getName());
        body.put("type", node.getType().name());
        body.put("code", node.getCode());
        body.put("path", node.getPath());
        body.put("level", node.getLevel());
        body.put("status", node.getStatus().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ==================== ÉTAPE 3 : DÉPARTEMENT ====================

    @PostMapping("/department")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> provisionDepartment(@RequestBody DepartmentProvisionRequest request) {
        if (request.tenantId() == null) {
            return badRequest("tenantId est requis", "tenantId");
        }
        if (request.nom() == null || request.nom().isBlank()) {
            return badRequest("Le nom du département est requis", "nom");
        }
        Tenant tenant = requireTenant(request.tenantId());
        UUID actorId = SecurityUtils.getCurrentUserId();

        CreateDepartmentRequest serviceRequest = new CreateDepartmentRequest(
                request.nom().trim(),
                request.description(),
                request.responsableId(),
                request.createNewResponsable(),
                request.newRespFirstName(),
                request.newRespLastName(),
                request.newRespEmail(),
                request.newRespPhone());

        Department department = withTenantContext(tenant.getId(),
                () -> departmentService.create(serviceRequest));

        auditService.log(actorId, tenant.getId(), "PROVISIONING_DEPARTMENT_CREATED", "DEPARTMENT",
                department.getId(), "SUCCESS",
                Map.of("tenantId", tenant.getId().toString(), "nom", department.getNom()),
                null, null, null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", department.getId().toString());
        body.put("tenantId", tenant.getId().toString());
        body.put("nom", department.getNom());
        body.put("description", department.getDescription());
        body.put("responsableId", department.getResponsableId() != null
                ? department.getResponsableId().toString() : null);
        body.put("statut", department.getStatut() != null ? department.getStatut().name() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ==================== ÉTAPE 4 : FAMILLE ====================

    @PostMapping("/family")
    @PreAuthorize("@authz.isPlatformSuperAdmin()")
    public ResponseEntity<Map<String, Object>> provisionFamily(@RequestBody FamilyProvisionRequest request) {
        if (request.tenantId() == null) {
            return badRequest("tenantId est requis", "tenantId");
        }
        if (request.nom() == null || request.nom().isBlank()) {
            return badRequest("Le nom de la famille est requis", "nom");
        }
        Tenant tenant = requireTenant(request.tenantId());
        UUID actorId = SecurityUtils.getCurrentUserId();

        CreateFamilyRequest serviceRequest = new CreateFamilyRequest(
                request.nom().trim(),
                request.chefFamilleId(),
                request.chefAdjointId(),
                request.createNewChef(),
                request.newChefFirstName(),
                request.newChefLastName(),
                request.newChefEmail(),
                request.newChefPhone(),
                request.newChefSexe(),
                request.newChefDateNaissance(),
                request.newChefAdresse());

        Family family = withTenantContext(tenant.getId(), () -> familyService.create(serviceRequest));

        auditService.log(actorId, tenant.getId(), "PROVISIONING_FAMILY_CREATED", "FAMILY",
                family.getId(), "SUCCESS",
                Map.of("tenantId", tenant.getId().toString(), "nom", family.getNom()),
                null, null, null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", family.getId().toString());
        body.put("tenantId", tenant.getId().toString());
        body.put("nom", family.getNom());
        body.put("chefFamilleId", family.getChefFamilleId() != null
                ? family.getChefFamilleId().toString() : null);
        body.put("statut", family.getStatut() != null ? family.getStatut().name() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }


    // ==================== HELPERS ====================

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant introuvable: " + tenantId));
    }

    /**
     * Exécute une action avec le ThreadLocal TenantContext positionné sur le
     * tenant cible, puis restaure le contexte précédent (fail-safe).
     */
    private <T> T withTenantContext(UUID tenantId, Supplier<T> action) {
        UUID previous = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            return action.get();
        } finally {
            if (previous != null) {
                TenantContext.setTenantId(previous);
            } else {
                TenantContext.clear();
            }
        }
    }

    private Map<String, Object> toNodeMap(OrganizationNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.getId().toString());
        map.put("tenantId", node.getTenantId().toString());
        map.put("name", node.getName());
        map.put("type", node.getType().name());
        map.put("code", node.getCode());
        map.put("path", node.getPath());
        map.put("level", node.getLevel());
        map.put("status", node.getStatus().name());
        return map;
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message, String field) {
        return ResponseEntity.badRequest().body(Map.of("error", message, "field", field));
    }

    // ==================== DTOs ====================

    public record AtomicProvisioningRequest(
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

    /** Étape 2 — création de l'église racine du tenant. */
    public record ChurchProvisionRequest(
            UUID tenantId,
            String name,
            UUID responsibleId
    ) {}

    /** Étape 3 — département (mêmes règles que CreateDepartmentRequest). */
    public record DepartmentProvisionRequest(
            UUID tenantId,
            String nom,
            String description,
            UUID responsableId,
            Boolean createNewResponsable,
            String newRespFirstName,
            String newRespLastName,
            String newRespEmail,
            String newRespPhone
    ) {}

    /** Étape 4 — famille (mêmes règles que CreateFamilyRequest). */
    public record FamilyProvisionRequest(
            UUID tenantId,
            String nom,
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
}

