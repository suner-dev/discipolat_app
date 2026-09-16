package com.discipolat.modules.tenants.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * G2.2 — ModuleRouter : Routage feature-flag pour modules CORE | EXISTING | ENGINE
 *
 * Un module EXISTING route vers l'implémentation actuelle (legacy controller)
 * Un module ENGINE route vers le nouveau code (engine controller)
 * Un module CORE route vers le noyau platform
 */
@Component
@RequiredArgsConstructor
public class ModuleRouter {

    private final ModuleCatalogService catalogService;
    private final SpaceModuleRepository spaceModuleRepository;

    /**
     * Résout le routeur pour un module donné dans un espace donné.
     * Vérifie le catalogue global ET l'activation dans l'espace.
     */
    public ModuleRoute resolveRoute(UUID tenantId, UUID spaceId, String moduleCode) {
        Optional<ModuleDefinition> defOpt = catalogService.getByCode(moduleCode);
        if (defOpt.isEmpty()) {
            return ModuleRoute.notFound(moduleCode);
        }

        ModuleDefinition def = defOpt.get();

        if (!def.getEnabled()) {
            return ModuleRoute.disabled(moduleCode);
        }

        // Vérifier l'activation dans l'espace spécifique
        boolean enabledInSpace = spaceModuleRepository
                .findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .map(SpaceModule::getEnabled)
                .orElse(true); // Par défaut activé si pas de config spécifique

        if (!enabledInSpace) {
            return ModuleRoute.disabledInSpace(moduleCode);
        }

        return new ModuleRoute(
                moduleCode,
                def.getSource(),
                def.getName(),
                def.getCategory(),
                buildRoutePrefix(def),
                def.getFeaturesJson(),
                true
        );
    }

    private String buildRoutePrefix(ModuleDefinition def) {
        String category = def.getCategory() != null ? def.getCategory().toLowerCase() : "general";
        String code = def.getCode().toLowerCase();
        return "/api/v1/" + category + "/" + code;
    }

    /**
     * Vérifie si un module est activé pour un espace
     */
    public boolean isModuleEnabledForSpace(UUID tenantId, UUID spaceId, String moduleCode) {
        Optional<ModuleDefinition> defOpt = catalogService.getByCode(moduleCode);
        if (defOpt.isEmpty() || !defOpt.get().getEnabled()) {
            return false;
        }
        return spaceModuleRepository
                .findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .map(SpaceModule::getEnabled)
                .orElse(true);
    }

    /**
     * Liste tous les modules accessibles pour un espace (catalog filtré par activation)
     */
    public List<ModuleRoute> getAvailableRoutes(UUID tenantId, UUID spaceId) {
        return catalogService.getEnabledDefinitions().stream()
                .map(def -> resolveRoute(tenantId, spaceId, def.getCode()))
                .filter(Objects::nonNull)
                .filter(ModuleRoute::isEnabled)
                .toList();
    }

    // Records
    public record ModuleRoute(
            String moduleCode,
            String source,
            String name,
            String category,
            String routePrefix,
            Map<String, Object> features,
            boolean enabled
    ) {
        public static ModuleRoute notFound(String moduleCode) {
            return new ModuleRoute(moduleCode, "UNKNOWN", "Unknown", "ERROR", null, Map.of(), false);
        }

        public static ModuleRoute disabled(String moduleCode) {
            return new ModuleRoute(moduleCode, "DISABLED", "Disabled", "ERROR", null, Map.of(), false);
        }

        public static ModuleRoute disabledInSpace(String moduleCode) {
            return new ModuleRoute(moduleCode, "DISABLED_IN_SPACE", "Disabled in Space", "ERROR", null, Map.of(), false);
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
