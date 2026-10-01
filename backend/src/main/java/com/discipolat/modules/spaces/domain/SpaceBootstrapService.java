package com.discipolat.modules.spaces.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
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
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.TenantFeature;
import com.discipolat.modules.tenants.domain.TenantFeatureService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * G5.3 — Bootstrap d'un espace (niveau 2 : expérience générée).
 *
 * Construit depuis la configuration RÉELLE de l'espace (jamais codé par espace) :
 * template G2.3 + modules activés G1.3/G2.2 + statuts customisés G2.7 +
 * champs personnalisés G2.4 + espaces membres G3.2 + permissions résolues
 * serveur (§G4.4). Le frontend construit nav, widgets et actions à partir de
 * cette seule réponse : deux espaces = deux expériences distinctes.
 */
@Service
public class SpaceBootstrapService {

    private static final int MEMBERS_PREVIEW_LIMIT = 50;

    private final SpaceService spaceService;
    private final SpaceTemplateRepository templateRepository;
    private final TenantFeatureService tenantFeatureService;
    private final CustomStatusService statusService;
    private final CustomFieldService customFieldService;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final PersonRepository personRepository;
    private final AuthorizationService authorizationService;
    private final TenantMembershipRepository tenantMembershipRepository;

    public SpaceBootstrapService(
            SpaceService spaceService,
            SpaceTemplateRepository templateRepository,
            TenantFeatureService tenantFeatureService,
            CustomStatusService statusService,
            CustomFieldService customFieldService,
            SpaceMembershipRepository spaceMembershipRepository,
            PersonRepository personRepository,
            AuthorizationService authorizationService,
            TenantMembershipRepository tenantMembershipRepository) {
        this.spaceService = spaceService;
        this.templateRepository = templateRepository;
        this.tenantFeatureService = tenantFeatureService;
        this.statusService = statusService;
        this.customFieldService = customFieldService;
        this.spaceMembershipRepository = spaceMembershipRepository;
        this.personRepository = personRepository;
        this.authorizationService = authorizationService;
        this.tenantMembershipRepository = tenantMembershipRepository;
    }

    @Transactional(readOnly = true)
    public SpaceBootstrap bootstrap(UUID tenantId, UUID spaceId, String entityTypeParam) {
        Space space = spaceService.getSpace(tenantId, spaceId); // scope tenant strict (§0.3)
        UUID actorId = SecurityUtils.getCurrentUserId();

        String entityType = (entityTypeParam != null && !entityTypeParam.isBlank())
                ? entityTypeParam.trim()
                : space.getSpaceType().name();

        Map<String, Object> config = space.getConfigurationJson() != null
                ? space.getConfigurationJson() : Map.of();

        Optional<SpaceTemplate> template = space.getTemplateCode() != null
                ? templateRepository.findByCode(space.getTemplateCode())
                : Optional.empty();

        // ── Modules : template G2.3, surchargé par la config de l'espace,
        // borné par les modules activés au niveau tenant (G1.3/G2.2).
        Set<String> tenantEnabled = tenantFeatureService.getEnabledFeatures(tenantId).stream()
                .map(TenantFeature::getModuleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> baseModules;
        Object configModules = config.get("modules");
        if (configModules instanceof List<?> list && !list.isEmpty()) {
            baseModules = list.stream().map(String::valueOf).toList();
        } else if (template.isPresent() && template.get().getModulesJson() != null) {
            baseModules = template.get().getModulesJson();
        } else {
            baseModules = List.copyOf(tenantEnabled);
        }
        List<ModuleBootstrap> modules = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String code : baseModules) {
            if (code == null || code.isBlank() || !seen.add(code)) continue;
            modules.add(new ModuleBootstrap(code, tenantEnabled.contains(code)));
        }

        // ── Statuts customisés (G2.7) : jeu de l'espace, sinon hérité du tenant.
        List<Map<String, Object>> statuses = statusService.getStatusBoard(tenantId, entityType, spaceId);

        // ── Champs personnalisés (G2.4) : déjà filtrés par rôle de lecture serveur.
        List<FieldBootstrap> customFields = customFieldService.getDefinitions(entityType).stream()
                .map(d -> new FieldBootstrap(
                        d.getCode(), d.getLabel(), d.getType(), d.isObligatoire(),
                        d.getOptions() != null ? d.getOptions() : List.of(),
                        d.getPlaceholder(), d.getDefaultValue()))
                .toList();

        // ── Widgets : template par défaut, surchargés par la config de l'espace.
        List<Map<String, Object>> widgets;
        Object configWidgets = config.get("widgets");
        if (configWidgets instanceof List<?> list && !list.isEmpty()) {
            widgets = mapList(list);
        } else {
            widgets = template.map(SpaceTemplate::getDefaultDashboardsJson).orElse(List.of());
        }
        boolean widgetsLocked = Boolean.TRUE.equals(config.get("widgetsLocked"));

        // ── Permissions résolues serveur (§G4.4) — jamais autofinées par le client.
        boolean canCustomize = spaceService.canCustomize(actorId, space);
        Set<String> permissionKeys = authorizationService.getCurrentUserPermissions();
        List<String> roleKeys = tenantMembershipRepository
                .findAllByUserIdAndTenantIdAndStatus(actorId, tenantId, MembershipStatus.ACTIVE).stream()
                .filter(m -> m.getRole() != null)
                .map(m -> m.getRole().getKey())
                .distinct()
                .toList();

        // ── Membres actifs (G3.2) : aperçu borné + compte total réel.
        List<SpaceMembership> memberships =
                spaceMembershipRepository.findByTenantIdAndSpaceIdAndStatus(tenantId, spaceId, "ACTIVE");
        long memberCount = memberships.size();
        Map<UUID, Person> personsById = personRepository
                .findAllById(memberships.stream().limit(MEMBERS_PREVIEW_LIMIT).map(SpaceMembership::getPersonId).toList())
                .stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<MemberBootstrap> membersPreview = memberships.stream()
                .limit(MEMBERS_PREVIEW_LIMIT)
                .map(m -> {
                    Person p = personsById.get(m.getPersonId());
                    String fullName = p != null
                            ? (p.getDisplayName() != null && !p.getDisplayName().isBlank()
                                ? p.getDisplayName()
                                : (p.getFirstName() + " " + p.getLastName()).trim())
                            : null;
                    return new MemberBootstrap(m.getPersonId(), fullName, m.getResponsibility(),
                            m.getMembershipType(), m.getJoinedAt() != null ? m.getJoinedAt().toString() : null);
                })
                .toList();

        return new SpaceBootstrap(
                space.getId(), space.getTenantId(), space.getOrganizationUnitId(),
                space.getSpaceType().name(), space.getTemplateCode(),
                space.getName(), space.getCode(), space.getIcon(), space.getColor(),
                space.getDescription(),
                space.getStatus() != null ? space.getStatus().name() : null,
                space.getVisiblePeopleScope() != null ? space.getVisiblePeopleScope().name() : null,
                entityType,
                modules, statuses, customFields, widgets, widgetsLocked,
                new PermissionsBootstrap(canCustomize, permissionKeys, roleKeys),
                memberCount, membersPreview,
                // UI générée : ordre/visibilité/couleurs personnalisés (§G2.7 propagation)
                readConfigSection(config, "uiConfig"),
                readConfigSection(config, "actions"),
                space.getUpdatedAt() != null ? space.getUpdatedAt().toString() : null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readConfigSection(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return v instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(List<?> list) {
        return list.stream().filter(i -> i instanceof Map).map(i -> (Map<String, Object>) i).toList();
    }

    // ==================== RESPONSE RECORDS ====================

    public record SpaceBootstrap(
            UUID spaceId,
            UUID tenantId,
            UUID organizationUnitId,
            String spaceType,
            String templateCode,
            String name,
            String code,
            String icon,
            String color,
            String description,
            String status,
            String visiblePeopleScope,
            String entityType,
            List<ModuleBootstrap> modules,
            List<Map<String, Object>> statuses,
            List<FieldBootstrap> customFields,
            List<Map<String, Object>> widgets,
            boolean widgetsLocked,
            PermissionsBootstrap permissions,
            long memberCount,
            List<MemberBootstrap> membersPreview,
            Map<String, Object> uiConfig,
            Map<String, Object> actions,
            String updatedAt
    ) {}

    public record ModuleBootstrap(String code, boolean enabled) {}

    public record FieldBootstrap(
            String key, String label, String type, boolean required,
            List<String> options, String placeholder, String defaultValue) {}

    public record PermissionsBootstrap(boolean canCustomize, Set<String> permissionKeys, List<String> roleKeys) {}

    public record MemberBootstrap(UUID personId, String fullName, String responsibility,
                                  String membershipType, String joinedAt) {}
}
