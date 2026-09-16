package com.discipolat.modules.tenants.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.ModuleCatalogService;
import com.discipolat.modules.tenants.domain.ModuleDefinition;
import com.discipolat.modules.tenants.domain.ModuleRouter;
import com.discipolat.modules.tenants.domain.SpaceModuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * G2.2 — API pour le catalogue de modules et la gestion des modules par espace
 * GET /api/modules/catalog
 * GET/PUT /api/spaces/:id/modules
 */
@RestController
@RequestMapping("/api/v1")
public class ModuleCatalogController {

    private final ModuleCatalogService catalogService;
    private final SpaceModuleService spaceModuleService;
    private final ModuleRouter moduleRouter;

    public ModuleCatalogController(ModuleCatalogService catalogService,
                                   SpaceModuleService spaceModuleService,
                                   ModuleRouter moduleRouter) {
        this.catalogService = catalogService;
        this.spaceModuleService = spaceModuleService;
        this.moduleRouter = moduleRouter;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return tenantId;
    }

    // ==================== CATALOGUE GLOBAL ====================

    @GetMapping("/modules/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ModuleDefinition>> getCatalog() {
        return ResponseEntity.ok(catalogService.getEnabledDefinitions());
    }

    @GetMapping("/modules/catalog/grouped")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<ModuleDefinition>>> getCatalogGrouped() {
        return ResponseEntity.ok(catalogService.getGroupedByCategory());
    }

    @GetMapping("/modules/catalog/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ModuleDefinition> getModuleDefinition(@PathVariable String code) {
        return catalogService.getByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/modules/catalog/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getCatalogStats() {
        return ResponseEntity.ok(Map.of(
                "total", catalogService.totalCount(),
                "core", catalogService.countBySource("CORE"),
                "existing", catalogService.countBySource("EXISTING"),
                "engine", catalogService.countBySource("ENGINE")
        ));
    }

    // ==================== MODULES PAR ESPACE ====================

    @GetMapping("/spaces/{spaceId}/modules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getSpaceModules(@PathVariable UUID spaceId) {
        UUID tenantId = getCurrentTenantId();
        List<Map<String, Object>> modules = spaceModuleService.getCatalogForSpace(tenantId, spaceId);
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/spaces/{spaceId}/modules/enabled")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpaceModuleResponse>> getEnabledSpaceModules(@PathVariable UUID spaceId) {
        UUID tenantId = getCurrentTenantId();
        List<SpaceModuleResponse> modules = spaceModuleService.getEnabledModulesBySpace(tenantId, spaceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/spaces/{spaceId}/modules/{moduleCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getSpaceModule(@PathVariable UUID spaceId, @PathVariable String moduleCode) {
        UUID tenantId = getCurrentTenantId();
        Map<String, Object> config = spaceModuleService.getModuleDefinitionWithSpaceConfig(tenantId, spaceId, moduleCode);
        return ResponseEntity.ok(config);
    }

    @PutMapping("/spaces/{spaceId}/modules/{moduleCode}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<SpaceModuleResponse> toggleSpaceModule(
            @PathVariable UUID spaceId,
            @PathVariable String moduleCode,
            @RequestBody SpaceModuleToggleRequest request) {

        UUID tenantId = getCurrentTenantId();

        SpaceModuleResponse response;
        if (request.enabled()) {
            response = toResponse(spaceModuleService.enableModule(
                    tenantId, spaceId, moduleCode, request.configuration(), request.limits()));
        } else {
            response = toResponse(spaceModuleService.disableModule(tenantId, spaceId, moduleCode));
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/spaces/{spaceId}/modules/{moduleCode}/config")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<SpaceModuleResponse> updateSpaceModuleConfig(
            @PathVariable UUID spaceId,
            @PathVariable String moduleCode,
            @RequestBody SpaceModuleConfigRequest request) {

        UUID tenantId = getCurrentTenantId();
        SpaceModuleResponse response = toResponse(spaceModuleService.updateConfiguration(
                tenantId, spaceId, moduleCode, request.configuration()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/spaces/{spaceId}/modules/{moduleCode}/limits")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<SpaceModuleResponse> updateSpaceModuleLimits(
            @PathVariable UUID spaceId,
            @PathVariable String moduleCode,
            @RequestBody SpaceModuleLimitsRequest request) {

        UUID tenantId = getCurrentTenantId();
        SpaceModuleResponse response = toResponse(spaceModuleService.updateLimits(
                tenantId, spaceId, moduleCode, request.limits()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/spaces/{spaceId}/modules/{moduleCode}/order")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<SpaceModuleResponse> updateSpaceModuleOrder(
            @PathVariable UUID spaceId,
            @PathVariable String moduleCode,
            @RequestBody SpaceModuleOrderRequest request) {

        UUID tenantId = getCurrentTenantId();
        SpaceModuleResponse response = toResponse(spaceModuleService.updateDisplayOrder(
                tenantId, spaceId, moduleCode, request.displayOrder()));
        return ResponseEntity.ok(response);
    }

    // ==================== ROUTER ====================

    @GetMapping("/modules/router/{moduleCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ModuleRouter.ModuleRoute> getModuleRoute(
            @PathVariable String moduleCode,
            @RequestParam UUID spaceId) {

        UUID tenantId = getCurrentTenantId();
        ModuleRouter.ModuleRoute route = moduleRouter.resolveRoute(tenantId, spaceId, moduleCode);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/spaces/{spaceId}/routes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ModuleRouter.ModuleRoute>> getSpaceRoutes(@PathVariable UUID spaceId) {
        UUID tenantId = getCurrentTenantId();
        List<ModuleRouter.ModuleRoute> routes = moduleRouter.getAvailableRoutes(tenantId, spaceId);
        return ResponseEntity.ok(routes);
    }

    // ==================== DTOs ====================

    public record SpaceModuleResponse(
            UUID id,
            UUID tenantId,
            UUID spaceId,
            String moduleCode,
            boolean enabled,
            Map<String, Object> configuration,
            Map<String, Object> limits,
            Integer displayOrder,
            java.time.Instant createdAt,
            java.time.Instant updatedAt
    ) {}

    public record SpaceModuleToggleRequest(
            boolean enabled,
            Map<String, Object> configuration,
            Map<String, Object> limits
    ) {}

    public record SpaceModuleConfigRequest(
            Map<String, Object> configuration
    ) {}

    public record SpaceModuleLimitsRequest(
            Map<String, Object> limits
    ) {}

    public record SpaceModuleOrderRequest(
            int displayOrder
    ) {}

    private SpaceModuleResponse toResponse(com.discipolat.modules.tenants.domain.SpaceModule sm) {
        return new SpaceModuleResponse(
                sm.getId(),
                sm.getTenantId(),
                sm.getSpaceId(),
                sm.getModuleCode(),
                sm.getEnabled(),
                sm.getConfigurationJson(),
                sm.getLimitsJson(),
                sm.getDisplayOrder(),
                sm.getCreatedAt(),
                sm.getUpdatedAt()
        );
    }
}