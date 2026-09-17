package com.discipolat.modules.spaces.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.spaces.domain.Space;
import com.discipolat.modules.spaces.domain.SpaceService;
import com.discipolat.modules.spaces.domain.SpaceStatus;
import com.discipolat.modules.spaces.domain.SpaceType;
import com.discipolat.modules.spaces.domain.VisiblePeopleScope;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * G2.6 — API des espaces configurables unifiés.
 *
 * Toutes les décisions d'autorisation sont prises côté backend via
 * {@link SpaceService#canCustomize(UUID, Space)} ; le frontend ne fait
 * qu'afficher ce que le backend autorise (§0.3 n°4).
 */
@RestController
@RequestMapping("/api/v1/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    private UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
        return tenantId;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpaceResponse>> list(@RequestParam(required = false) SpaceType type) {
        UUID tenant = tenantId();
        List<Space> spaces = type == null
                ? spaceService.listSpaces(tenant)
                : spaceService.listSpacesByType(tenant, type);
        return ResponseEntity.ok(spaces.stream().map(this::toResponse).toList());
    }

    @GetMapping("/customizable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UUID>> customizable() {
        return ResponseEntity.ok(spaceService.resolveCustomizableSpaceIds(SecurityUtils.getCurrentUserId(), tenantId()));
    }

    @GetMapping("/organization-unit/{organizationUnitId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SpaceResponse>> byOrganizationUnit(@PathVariable UUID organizationUnitId) {
        return ResponseEntity.ok(spaceService.listSpacesByOrganizationUnit(tenantId(), organizationUnitId)
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/{spaceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpaceResponse> get(@PathVariable UUID spaceId) {
        return ResponseEntity.ok(toResponse(spaceService.getSpace(tenantId(), spaceId)));
    }

    @GetMapping("/{spaceId}/can-customize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> canCustomize(@PathVariable UUID spaceId) {
        Space space = spaceService.getSpace(tenantId(), spaceId);
        boolean allowed = spaceService.canCustomize(SecurityUtils.getCurrentUserId(), space);
        return ResponseEntity.ok(Map.of(
                "spaceId", spaceId.toString(),
                "canCustomize", allowed
        ));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpaceResponse> create(@RequestBody SpaceRequest request) {
        Space created = spaceService.createSpace(tenantId(), SecurityUtils.getCurrentUserId(), request.toCommand());
        return ResponseEntity.ok(toResponse(created));
    }

    @PutMapping("/{spaceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SpaceResponse> update(@PathVariable UUID spaceId, @RequestBody SpaceRequest request) {
        Space updated = spaceService.updateSpace(tenantId(), SecurityUtils.getCurrentUserId(),
                spaceId, request.toCommand());
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{spaceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archive(@PathVariable UUID spaceId) {
        spaceService.archiveSpace(tenantId(), SecurityUtils.getCurrentUserId(), spaceId);
        return ResponseEntity.noContent().build();
    }

    private SpaceResponse toResponse(Space space) {
        return new SpaceResponse(
                space.getId(),
                space.getTenantId(),
                space.getOrganizationUnitId(),
                space.getSpaceType(),
                space.getTemplateCode(),
                space.getName(),
                space.getCode(),
                space.getIcon(),
                space.getColor(),
                space.getDescription(),
                space.getStatus(),
                space.getVisiblePeopleScope(),
                space.getConfigurationJson(),
                space.getCreatedAt(),
                space.getUpdatedAt()
        );
    }

    public record SpaceResponse(
            UUID id,
            UUID tenantId,
            UUID organizationUnitId,
            SpaceType spaceType,
            String templateCode,
            String name,
            String code,
            String icon,
            String color,
            String description,
            SpaceStatus status,
            VisiblePeopleScope visiblePeopleScope,
            Map<String, Object> configuration,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record SpaceRequest(
            UUID organizationUnitId,
            SpaceType spaceType,
            String templateCode,
            String name,
            String code,
            String icon,
            String color,
            String description,
            SpaceStatus status,
            VisiblePeopleScope visiblePeopleScope,
            Map<String, Object> configuration
    ) {
        SpaceService.SpaceCommand toCommand() {
            return new SpaceService.SpaceCommand(
                    organizationUnitId, spaceType, templateCode, name, code,
                    icon, color, description, status, visiblePeopleScope, configuration);
        }
    }
}