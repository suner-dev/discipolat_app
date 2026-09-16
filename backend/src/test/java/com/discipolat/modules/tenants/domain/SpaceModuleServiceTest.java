package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G2.2 — Tests du service des modules par espace (SpaceModuleService).
 * DoD : activation/désactivation, module inconnu refusé, validation de l'espace.
 */
@ExtendWith(MockitoExtension.class)
class SpaceModuleServiceTest {

    @Mock private SpaceModuleRepository spaceModuleRepository;
    @Mock private ModuleDefinitionRepository definitionRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    @Mock private ModuleCatalogService catalogService;

    @InjectMocks private SpaceModuleService spaceModuleService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID SPACE_ID = UUID.randomUUID();

    private ModuleDefinition def(String code, boolean enabled) {
        return ModuleDefinition.builder()
                .code(code)
                .name(code)
                .category("CONFIG")
                .source("ENGINE")
                .enabled(enabled)
                .displayOrder(0)
                .build();
    }

    @Test
    void enableModule_inconnu_shouldThrow() {
        when(definitionRepository.findByCode("inconnu")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> spaceModuleService.enableModule(TENANT_ID, SPACE_ID, "inconnu", null, null));
    }

    @Test
    void enableModule_desactiveDansCatalogue_shouldThrow() {
        when(definitionRepository.findByCode("events"))
                .thenReturn(Optional.of(def("events", false)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> spaceModuleService.enableModule(TENANT_ID, SPACE_ID, "events", null, null));
        assertTrue(ex.getMessage().contains("n'est pas disponible"));
    }

    @Test
    void enableModule_espaceInconnu_shouldThrow() {
        when(definitionRepository.findByCode("workflow"))
                .thenReturn(Optional.of(def("workflow", true)));
        when(orgNodeRepository.findById(SPACE_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> spaceModuleService.enableModule(TENANT_ID, SPACE_ID, "workflow", null, null));
    }

    @Test
    void enableModule_espaceHorsTenant_shouldThrow() {
        when(definitionRepository.findByCode("workflow"))
                .thenReturn(Optional.of(def("workflow", true)));

        OrganizationNode otherTenantNode = OrganizationNode.builder()
                .id(SPACE_ID)
                .tenantId(UUID.randomUUID())
                .name("Autre église")
                .build();
        when(orgNodeRepository.findById(SPACE_ID)).thenReturn(Optional.of(otherTenantNode));

        assertThrows(EntityNotFoundException.class,
                () -> spaceModuleService.enableModule(TENANT_ID, SPACE_ID, "workflow", null, null));
    }

    @Test
    void enableModule_valide_shouldCreateNeverCreatedModule() {
        when(definitionRepository.findByCode("workflow"))
                .thenReturn(Optional.of(def("workflow", true)));

        OrganizationNode node = OrganizationNode.builder()
                .id(SPACE_ID)
                .tenantId(TENANT_ID)
                .name("Audiovisuel")
                .build();
        when(orgNodeRepository.findById(SPACE_ID)).thenReturn(Optional.of(node));
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.empty());
        when(spaceModuleRepository.save(
                org.mockito.ArgumentMatchers.any(SpaceModule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SpaceModule module = spaceModuleService.enableModule(
                TENANT_ID, SPACE_ID, "workflow", Map.of("maxApprovals", 2), null);

        assertTrue(module.getEnabled());
        assertEquals(TENANT_ID, module.getTenantId());
        assertEquals(SPACE_ID, module.getSpaceId());
        assertEquals(Map.of("maxApprovals", 2), module.getConfigurationJson());
    }

    @Test
    void disableModule_inexistantPourEspace_shouldThrow() {
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> spaceModuleService.disableModule(TENANT_ID, SPACE_ID, "workflow"));
    }

    @Test
    void disableModule_existant_shouldDisable() {
        SpaceModule existing = SpaceModule.builder()
                .tenantId(TENANT_ID)
                .spaceId(SPACE_ID)
                .moduleCode("workflow")
                .enabled(true)
                .build();
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.of(existing));
        when(spaceModuleRepository.save(
                org.mockito.ArgumentMatchers.any(SpaceModule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SpaceModule disabled = spaceModuleService.disableModule(TENANT_ID, SPACE_ID, "workflow");

        assertFalse(disabled.getEnabled());
    }

    @Test
    void getCatalogForSpace_shouldMarkEnabledBySpaceModule() {
        when(catalogService.getEnabledDefinitions()).thenReturn(
                List.of(def("workflow", true), def("custom_fields", true)));
        when(spaceModuleRepository.findByTenantIdAndSpaceId(TENANT_ID, SPACE_ID))
                .thenReturn(List.of(SpaceModule.builder()
                        .tenantId(TENANT_ID)
                        .spaceId(SPACE_ID)
                        .moduleCode("workflow")
                        .enabled(true)
                        .displayOrder(1)
                        .build()));

        List<Map<String, Object>> catalog = spaceModuleService.getCatalogForSpace(TENANT_ID, SPACE_ID);

        assertEquals(2, catalog.size());
        Map<String, Object> workflowItem = catalog.stream()
                .filter(m -> "workflow".equals(m.get("code")))
                .findFirst().orElseThrow();
        assertTrue((Boolean) workflowItem.get("enabled"));

        Map<String, Object> customFieldsItem = catalog.stream()
                .filter(m -> "custom_fields".equals(m.get("code")))
                .findFirst().orElseThrow();
        assertFalse((Boolean) customFieldsItem.get("enabled"));
    }

    @Test
    void deletes_inexistant_shouldThrowEntityNotFound() {
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> spaceModuleService.deleteModule(TENANT_ID, SPACE_ID, "workflow"));

        verify(spaceModuleRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletes_existant_shouldDelete() {
        SpaceModule existing = SpaceModule.builder()
                .tenantId(TENANT_ID)
                .spaceId(SPACE_ID)
                .moduleCode("workflow")
                .enabled(true)
                .build();
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.of(existing));

        spaceModuleService.deleteModule(TENANT_ID, SPACE_ID, "workflow");

        verify(spaceModuleRepository).delete(existing);
    }
}