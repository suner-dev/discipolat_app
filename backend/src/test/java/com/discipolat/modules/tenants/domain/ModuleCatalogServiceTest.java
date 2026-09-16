package com.discipolat.modules.tenants.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * G2.2 — Tests du catalogue de modules (ModuleCatalogService).
 * DoD : catalogue complet (comptage == inventaire G0.1), 404 si module inconnu.
 */
@ExtendWith(MockitoExtension.class)
class ModuleCatalogServiceTest {

    @Mock private ModuleDefinitionRepository definitionRepository;

    @InjectMocks private ModuleCatalogService catalogService;

    private ModuleDefinition def(String code, String category, String source, boolean enabled) {
        return ModuleDefinition.builder()
                .code(code)
                .name(code)
                .category(category)
                .source(source)
                .enabled(enabled)
                .displayOrder(0)
                .build();
    }

    @Test
    void getAllDefinitions_shouldReturnAllFromRepository() {
        when(definitionRepository.findAll()).thenReturn(
                List.of(def("people", "PEOPLE", "CORE", true),
                        def("dashboard", "PILOTAGE", "EXISTING", true)));

        assertEquals(2, catalogService.getAllDefinitions().size());
    }

    @Test
    void getEnabledDefinitions_shouldOnlyReturnEnabledModules() {
        when(definitionRepository.findByEnabledTrueOrderByCategoryAscDisplayOrderAsc()).thenReturn(
                List.of(def("people", "PEOPLE", "CORE", true)));

        List<ModuleDefinition> enabled = catalogService.getEnabledDefinitions();

        assertEquals(1, enabled.size());
        assertTrue(enabled.stream().allMatch(ModuleDefinition::getEnabled));
    }

    @Test
    void getByCode_shouldReturnModule_whenExists() {
        when(definitionRepository.findByCode("people")).thenReturn(
                Optional.of(def("people", "PEOPLE", "CORE", true)));

        Optional<ModuleDefinition> found = catalogService.getByCode("people");

        assertTrue(found.isPresent());
        assertEquals("people", found.get().getCode());
    }

    @Test
    void getByCode_shouldBeEmpty_whenModuleUnknown() {
        when(definitionRepository.findByCode("inconnu")).thenReturn(Optional.empty());

        Optional<ModuleDefinition> notFound = catalogService.getByCode("inconnu");

        assertTrue(notFound.isEmpty());
    }

    @Test
    void countBySource_shouldCountModulesPerSource() {
        when(definitionRepository.findBySource("ENGINE")).thenReturn(
                List.of(def("workflow", "CONFIG", "ENGINE", true),
                        def("custom_fields", "CONFIG", "ENGINE", true)));

        assertEquals(2, catalogService.countBySource("ENGINE"));
    }

    @Test
    void getGroupedByCategory_shouldGroupEnabledModules() {
        when(definitionRepository.findByEnabledTrueOrderByCategoryAscDisplayOrderAsc()).thenReturn(
                List.of(def("people", "PEOPLE", "CORE", true),
                        def("org", "ORGANIZATION", "CORE", true),
                        def("events", "EVENTS", "CORE", true),
                        def("workflow", "CONFIG", "ENGINE", true),
                        def("config", "CONFIG", "ENGINE", true)));

        Map<String, List<ModuleDefinition>> grouped = catalogService.getGroupedByCategory();

        assertEquals(4, grouped.size());
        assertEquals(2, grouped.get("CONFIG").size());
        assertEquals(1, grouped.get("PEOPLE").size());
    }
}