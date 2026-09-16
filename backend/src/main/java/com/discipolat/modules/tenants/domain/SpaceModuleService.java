package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * G2.2 — Service pour les modules par espace (SpaceModule)
 * Gestion CRUD + validation contre le catalogue ModuleDefinition
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SpaceModuleService {

    private final SpaceModuleRepository spaceModuleRepository;
    private final ModuleDefinitionRepository definitionRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final ModuleCatalogService catalogService;

    public List<SpaceModule> getModulesBySpace(UUID tenantId, UUID spaceId) {
        return spaceModuleRepository.findByTenantIdAndSpaceId(tenantId, spaceId);
    }

    public List<SpaceModule> getEnabledModulesBySpace(UUID tenantId, UUID spaceId) {
        return spaceModuleRepository.findEnabledBySpaceIdOrderByDisplayOrder(tenantId, spaceId);
    }

    public Optional<SpaceModule> getModule(UUID tenantId, UUID spaceId, String moduleCode) {
        return spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode);
    }

    public SpaceModule enableModule(UUID tenantId, UUID spaceId, String moduleCode, Map<String, Object> configuration, Map<String, Object> limits) {
        // Valider que le module existe dans le catalogue
        ModuleDefinition definition = definitionRepository.findByCode(moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("ModuleDefinition", "code", moduleCode));

        if (!definition.getEnabled()) {
            throw new IllegalStateException("Module " + moduleCode + " n'est pas disponible dans le catalogue");
        }

        // Valider que l'espace existe et appartient au tenant
        orgNodeRepository.findById(spaceId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", "id", spaceId.toString()));

        SpaceModule module = spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .orElseGet(() -> SpaceModule.builder()
                        .tenantId(tenantId)
                        .spaceId(spaceId)
                        .moduleCode(moduleCode)
                        .build());

        module.setEnabled(true);
        if (configuration != null) module.setConfigurationJson(configuration);
        if (limits != null) module.setLimitsJson(limits);

        return spaceModuleRepository.save(module);
    }

    public SpaceModule disableModule(UUID tenantId, UUID spaceId, String moduleCode) {
        SpaceModule module = spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("SpaceModule", "moduleCode", moduleCode));

        module.setEnabled(false);
        return spaceModuleRepository.save(module);
    }

    public SpaceModule updateConfiguration(UUID tenantId, UUID spaceId, String moduleCode, Map<String, Object> configuration) {
        SpaceModule module = spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("SpaceModule", "moduleCode", moduleCode));

        module.setConfigurationJson(configuration != null ? configuration : Map.of());
        return spaceModuleRepository.save(module);
    }

    public SpaceModule updateLimits(UUID tenantId, UUID spaceId, String moduleCode, Map<String, Object> limits) {
        SpaceModule module = spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("SpaceModule", "moduleCode", moduleCode));

        module.setLimitsJson(limits != null ? limits : Map.of());
        return spaceModuleRepository.save(module);
    }

    public SpaceModule updateDisplayOrder(UUID tenantId, UUID spaceId, String moduleCode, int displayOrder) {
        SpaceModule module = spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("SpaceModule", "moduleCode", moduleCode));

        module.setDisplayOrder(displayOrder);
        return spaceModuleRepository.save(module);
    }

    public void deleteModule(UUID tenantId, UUID spaceId, String moduleCode) {
        SpaceModule module = spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(tenantId, spaceId, moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("SpaceModule", "moduleCode", moduleCode));

        spaceModuleRepository.delete(module);
    }

    public Map<String, List<SpaceModule>> getModulesGroupedByCategory(UUID tenantId, UUID spaceId) {
        List<SpaceModule> modules = getEnabledModulesBySpace(tenantId, spaceId);

        Map<String, String> codeToCategory = definitionRepository.findAll().stream()
                .collect(Collectors.toMap(ModuleDefinition::getCode, ModuleDefinition::getCategory));

        return modules.stream()
                .collect(Collectors.groupingBy(
                        sm -> codeToCategory.getOrDefault(sm.getModuleCode(), "GENERAL")
                ));
    }

    public Map<String, Object> getModuleDefinitionWithSpaceConfig(UUID tenantId, UUID spaceId, String moduleCode) {
        ModuleDefinition def = definitionRepository.findByCode(moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("ModuleDefinition", "code", moduleCode));

        SpaceModule spaceModule = getModule(tenantId, spaceId, moduleCode).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("definition", def);
        result.put("spaceConfig", spaceModule != null ? spaceModule : Map.of());
        result.put("enabled", spaceModule != null && spaceModule.getEnabled());
        result.put("configuration", spaceModule != null ? spaceModule.getConfigurationJson() : Map.of());
        result.put("limits", spaceModule != null ? spaceModule.getLimitsJson() : Map.of());
        result.put("displayOrder", spaceModule != null ? spaceModule.getDisplayOrder() : 0);

        return result;
    }

    public List<Map<String, Object>> getCatalogForSpace(UUID tenantId, UUID spaceId) {
        List<ModuleDefinition> definitions = catalogService.getEnabledDefinitions();
        List<SpaceModule> spaceModules = getModulesBySpace(tenantId, spaceId);

        Map<String, SpaceModule> spaceModuleMap = spaceModules.stream()
                .collect(Collectors.toMap(SpaceModule::getModuleCode, sm -> sm));

        return definitions.stream().map(def -> {
            SpaceModule sm = spaceModuleMap.get(def.getCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", def.getCode());
            item.put("name", def.getName());
            item.put("description", def.getDescription());
            item.put("category", def.getCategory());
            item.put("icon", def.getIcon());
            item.put("source", def.getSource());
            item.put("enabled", sm != null && sm.getEnabled());
            item.put("configuration", sm != null ? sm.getConfigurationJson() : Map.of());
            item.put("limits", sm != null ? sm.getLimitsJson() : Map.of());
            item.put("displayOrder", sm != null ? sm.getDisplayOrder() : 0);
            return item;
        }).collect(Collectors.toList());
    }
}