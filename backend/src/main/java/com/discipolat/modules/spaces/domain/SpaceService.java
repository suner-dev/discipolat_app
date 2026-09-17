package com.discipolat.modules.spaces.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.ForbiddenException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.ConfigurationResolver;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * G2.6 — Service des espaces configurables unifiés.
 *
 * Règle non négociable §0.3 n°4 : les permissions sont vérifiées côté backend
 * uniquement. {@link #canCustomize(UUID, Space)} est la seule source de vérité,
 * exposée à l'UI mais jamais substituable par elle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SpaceService {

    /** Rôles tenant qui configurent TOUS les espaces (admin/pasteur et assimilés). */
    private static final Set<String> TENANT_ADMIN_ROLE_KEYS = Set.of(
            "ADMIN", "PASTEUR", "PASTOR", "TENANT_OWNER", "TENANT_ADMIN",
            "SUPER_ADMIN", "PLATFORM_SUPER_ADMIN", "RESPONSABLE");
    /** Marqueurs de rôle indiquant une responsabilité d'espace (« chef de… »). */
    private static final Set<String> SPACE_MANAGER_MARKERS = Set.of(
            "CHEF", "RESPONSABLE", "DIRIGEANT", "LEAD", "PASTEUR", "PASTOR", "ADMIN",
            "CHEF_DE_FAMILLE");

    private final SpaceRepository spaceRepository;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityPropagationPublisher propagationPublisher;
    private final ConfigurationResolver configurationResolver;

    // ==================== Lectures ====================

    @Transactional(readOnly = true)
    public List<Space> listSpaces(UUID tenantId) {
        requireTenant(tenantId);
        return spaceRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Space> listSpacesByType(UUID tenantId, SpaceType type) {
        requireTenant(tenantId);
        return spaceRepository.findByTenantIdAndSpaceTypeAndDeletedAtIsNull(tenantId, type);
    }

    @Transactional(readOnly = true)
    public List<Space> listSpacesByOrganizationUnit(UUID tenantId, UUID organizationUnitId) {
        requireTenant(tenantId);
        return spaceRepository.findByTenantIdAndOrganizationUnitId(tenantId, organizationUnitId).stream()
                .filter(s -> !s.isDeleted())
                .toList();
    }

    /** Jamais findById(id) nu sur une donnée multi-tenant : §0.3 n°3. */
    @Transactional(readOnly = true)
    public Space getSpace(UUID tenantId, UUID spaceId) {
        requireTenant(tenantId);
        Space space = spaceRepository.findByIdAndTenantId(spaceId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Space", spaceId));
        if (space.isDeleted()) {
            throw new EntityNotFoundException("Space", spaceId);
        }
        return space;
    }

    // ==================== Autorisation (backend only) ====================

    /**
     * G2.6 — `canCustomize(actor, space)` : admin/pasteur configurent tout,
     * un chef ne configure que son espace (unité dont il est responsable,
     * ou une sous-unité de celle-ci).
     */
    @Transactional(readOnly = true)
    public boolean canCustomize(UUID actorId, Space space) {
        if (actorId == null || space == null || space.isDeleted()) {
            return false;
        }
        UUID tenantId = space.getTenantId();
        if (authorizationService.isPlatformSuperAdmin(actorId)) {
            return true;
        }
        if (isTenantAdmin(actorId, tenantId)) {
            return true;
        }
        return canConfigureUnit(actorId, tenantId, space.getOrganizationUnitId());
    }

    /**
     * Un acteur peut configurer une unité s'il en est le responsable,
     * responsable d'un ancêtre, ou s'il détient une appartenance scopée
     * à cette unité (ou un ancêtre) avec un rôle de responsable.
     */
    @Transactional(readOnly = true)
    public boolean canConfigureUnit(UUID actorId, UUID tenantId, UUID organizationUnitId) {
        if (actorId == null || tenantId == null || organizationUnitId == null) {
            return false;
        }
        if (authorizationService.isPlatformSuperAdmin(actorId) || isTenantAdmin(actorId, tenantId)) {
            return true;
        }

        OrganizationNode unit = organizationNodeRepository.findById(organizationUnitId).orElse(null);
        if (unit == null || !tenantId.equals(unit.getTenantId())) {
            return false;
        }

        Set<UUID> unitAndAncestors = unitAndAncestorIds(tenantId, unit);

        // 1. Responsable explicite de l'unité ou d'un ancêtre
        if (managedByResponsibility(tenantId, unitAndAncestors, actorId)) {
            return true;
        }

        // 2. Appartenance scopée sur l'unité ou un ancêtre avec rôle de responsable
        List<TenantMembership> memberships =
                membershipRepository.findAllByUserIdAndTenantIdAndStatus(actorId, tenantId, MembershipStatus.ACTIVE);
        for (TenantMembership membership : memberships) {
            if (membership.getScopeId() == null || !unitAndAncestors.contains(membership.getScopeId())) {
                continue;
            }
            String roleKey = membership.getRole() != null ? membership.getRole().getKey() : membership.getRoleLegacy();
            if (isSpaceManagerRole(roleKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Espaces configurables par un acteur (alimente le sélecteur « mes espaces »
     * de l'UI). Un admin/pasteur voit tous les espaces du tenant.
     */
    @Transactional(readOnly = true)
    public List<UUID> resolveCustomizableSpaceIds(UUID actorId, UUID tenantId) {
        requireTenant(tenantId);
        List<Space> all = spaceRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
        if (authorizationService.isPlatformSuperAdmin(actorId) || isTenantAdmin(actorId, tenantId)) {
            return all.stream().map(Space::getId).toList();
        }
        return all.stream()
                .filter(s -> canConfigureUnit(actorId, tenantId, s.getOrganizationUnitId()))
                .map(Space::getId)
                .toList();
    }

    // ==================== Écritures ====================

    public Space createSpace(UUID tenantId, UUID actorId, SpaceCommand command) {
        requireTenant(tenantId);
        if (command.organizationUnitId() == null) {
            throw new IllegalArgumentException("organizationUnitId est obligatoire");
        }
        if (!canConfigureUnit(actorId, tenantId, command.organizationUnitId())) {
            throw new ForbiddenException("Vous ne pouvez pas créer un espace dans cette unité");
        }

        OrganizationNode unit = organizationNodeRepository.findById(command.organizationUnitId())
                .filter(n -> tenantId.equals(n.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", command.organizationUnitId()));

        if (command.code() != null && spaceRepository.findByTenantIdAndCode(tenantId, command.code()).isPresent()) {
            throw new IllegalStateException("Un espace avec le code " + command.code() + " existe déjà");
        }

        Space space = Space.builder()
                .tenantId(tenantId)
                .organizationUnitId(unit.getId())
                .spaceType(command.spaceType() != null ? command.spaceType() : SpaceType.DEPARTMENT)
                .templateCode(command.templateCode())
                .name(command.name() != null ? command.name() : unit.getName())
                .code(command.code())
                .icon(command.icon())
                .color(command.color())
                .description(command.description())
                .status(SpaceStatus.ACTIVE)
                .visiblePeopleScope(command.visiblePeopleScope() != null
                        ? command.visiblePeopleScope() : VisiblePeopleScope.CHURCH)
                .configurationJson(command.configurationJson() != null
                        ? new LinkedHashMap<>(command.configurationJson()) : new LinkedHashMap<>())
                .build();

        Space saved = spaceRepository.save(space);
        auditService.logSimple("SPACE_CREATED", "SPACE", saved.getId());
        publish(saved, "CREATED", actorId);
        return saved;
    }

    public Space updateSpace(UUID tenantId, UUID actorId, UUID spaceId, SpaceCommand command) {
        Space space = getSpace(tenantId, spaceId);
        if (!canCustomize(actorId, space)) {
            throw new ForbiddenException("Vous ne pouvez pas configurer cet espace");
        }

        if (command.name() != null) space.setName(command.name());
        if (command.icon() != null) space.setIcon(command.icon());
        if (command.color() != null) space.setColor(command.color());
        if (command.description() != null) space.setDescription(command.description());
        if (command.templateCode() != null) space.setTemplateCode(command.templateCode());
        if (command.status() != null) space.setStatus(command.status());
        if (command.visiblePeopleScope() != null) space.setVisiblePeopleScope(command.visiblePeopleScope());
        if (command.configurationJson() != null) {
            Map<String, Object> merged = new LinkedHashMap<>(space.getConfigurationJson());
            merged.putAll(command.configurationJson());
            space.setConfigurationJson(merged);
        }

        Space saved = spaceRepository.save(space);
        auditService.logSimple("SPACE_CONFIG_UPDATED", "SPACE", saved.getId());
        publish(saved, "UPDATED", actorId);
        return saved;
    }

    /** Soft delete (§0.3 n°5) : la ligne reste en base pour l'historique. */
    public void archiveSpace(UUID tenantId, UUID actorId, UUID spaceId) {
        Space space = getSpace(tenantId, spaceId);
        if (!canCustomize(actorId, space)) {
            throw new ForbiddenException("Vous ne pouvez pas archiver cet espace");
        }
        space.setStatus(SpaceStatus.ARCHIVED);
        space.setDeletedAt(Instant.now());
        spaceRepository.save(space);
        auditService.logSimple("SPACE_ARCHIVED", "SPACE", space.getId());
        publish(space, "ARCHIVED", actorId);
    }

    // ==================== Helpers ====================

    private void publish(Space space, String changeType, UUID actorId) {
        eventPublisher.publishEvent(new SpaceConfigChangedEvent(
                space.getTenantId(), space.getId(), space.getCode(), changeType, actorId));

        // Propagation temps réel (< 5s) via le canal d'entités existant (SSE/tenant) :
        // les clients web + mobile invalident leur cache de configuration.
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("name", space.getName());
        newValues.put("color", space.getColor());
        newValues.put("icon", space.getIcon());
        if (space.getSpaceType() != null) newValues.put("spaceType", space.getSpaceType().name());
        newValues.put("organizationUnitId", space.getOrganizationUnitId().toString());
        String description = "Espace " + space.getName() + " (" + changeType + ")";

        switch (changeType) {
            case "CREATED" -> propagationPublisher.publishCreated("SPACE", space.getId(), newValues, description);
            case "ARCHIVED" -> propagationPublisher.publishSoftDeleted("SPACE", space.getId(), newValues, description);
            default -> propagationPublisher.publishUpdated("SPACE", space.getId(), Map.of(), newValues, description);
        }

        // Invalidation du cache de configuration résolue pour l'unité et ses descendants (§G1.7).
        configurationResolver.invalidateResolvedConfig(space.getTenantId(), space.getOrganizationUnitId());
    }

    private boolean isTenantAdmin(UUID actorId, UUID tenantId) {
        List<TenantMembership> memberships =
                membershipRepository.findAllByUserIdAndTenantIdAndStatus(actorId, tenantId, MembershipStatus.ACTIVE);
        return memberships.stream().anyMatch(m -> {
            String roleKey = m.getRole() != null ? m.getRole().getKey() : m.getRoleLegacy();
            return roleKey != null && TENANT_ADMIN_ROLE_KEYS.contains(roleKey.toUpperCase(Locale.ROOT));
        });
    }

    private boolean isSpaceManagerRole(String roleKey) {
        if (roleKey == null) return false;
        String upper = roleKey.toUpperCase(Locale.ROOT);
        return SPACE_MANAGER_MARKERS.stream().anyMatch(upper::contains);
    }

    private boolean managedByResponsibility(UUID tenantId, Set<UUID> unitAndAncestors, UUID actorId) {
        return organizationNodeRepository.findByTenantId(tenantId).stream()
                .filter(n -> unitAndAncestors.contains(n.getId()))
                .anyMatch(n -> actorId.equals(n.getResponsibleId()));
    }

    private Set<UUID> unitAndAncestorIds(UUID tenantId, OrganizationNode unit) {
        Set<UUID> ids = new LinkedHashSet<>();
        ids.add(unit.getId());
        if (unit.getPath() != null) {
            organizationNodeRepository.findByTenantId(tenantId).stream()
                    .filter(candidate -> candidate.getPath() != null
                            && !candidate.getId().equals(unit.getId())
                            && unit.getPath().startsWith(candidate.getPath() + "."))
                    .forEach(candidate -> ids.add(candidate.getId()));
        }
        return ids;
    }

    private void requireTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new SecurityException("Aucun tenant dans le contexte");
        }
    }

    /** Commande de création/mise à jour d'un espace. */
    public record SpaceCommand(
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
            Map<String, Object> configurationJson
    ) {}
}