package com.discipolat.modules.tenants.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * G2.2 — Tests du routeur feature-flag (ModuleRouter).
 * DoD : un module EXISTING répond par l'ancien chemin, un ENGINE par le nouveau ;
 * module désactivé / inconnu / désactivé dans l'espace → refusé.
 */
@ExtendWith(MockitoExtension.class)
class ModuleRouterTest {

    @Mock private ModuleCatalogService catalogService;
    @Mock private SpaceModuleRepository spaceModuleRepository;

    @InjectMocks private ModuleRouter moduleRouter;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID SPACE_ID = UUID.randomUUID();

    private ModuleDefinition def(String code, String category, String source, boolean enabled) {
        return ModuleDefinition.builder()
                .code(code)
                .category(category)
                .source(source)
                .name(code)
                .enabled(enabled)
                .displayOrder(0)
                .build();
    }

    @Test
    void resolveRoute_inconnu_shouldReturnNotFoundDisabled() {
        when(catalogService.getByCode("inconnu")).thenReturn(Optional.empty());

        ModuleRouter.ModuleRoute route = moduleRouter.resolveRoute(TENANT_ID, SPACE_ID, "inconnu");

        assertFalse(route.isEnabled());
        assertEquals("UNKNOWN", route.source());
    }

    @Test
    void resolveRoute_moduleDesactiveGlobalement_shouldBeDisabled() {
        when(catalogService.getByCode("events")).thenReturn(
                Optional.of(def("events", "EVENTS", "CORE", false)));

        ModuleRouter.ModuleRoute route = moduleRouter.resolveRoute(TENANT_ID, SPACE_ID, "events");

        assertFalse(route.isEnabled());
        assertEquals("DISABLED", route.source());
    }

    @Test
    void resolveRoute_moduleDesactiveDansLEspace_shouldBeDisabledInSpace() {
        when(catalogService.getByCode("workflow")).thenReturn(
                Optional.of(def("workflow", "CONFIG", "ENGINE", true)));
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.of(SpaceModule.builder()
                        .enabled(false)
                        .build()));

        ModuleRouter.ModuleRoute route = moduleRouter.resolveRoute(TENANT_ID, SPACE_ID, "workflow");

        assertFalse(route.isEnabled());
        assertEquals("DISABLED_IN_SPACE", route.source());
    }

    @Test
    void resolveRoute_moduleEngine_shouldRouteToNewPath() {
        when(catalogService.getByCode("workflow")).thenReturn(
                Optional.of(def("workflow", "CONFIG", "ENGINE", true)));
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "workflow"))
                .thenReturn(Optional.of(SpaceModule.builder()
                        .enabled(true)
                        .build()));

        ModuleRouter.ModuleRoute route = moduleRouter.resolveRoute(TENANT_ID, SPACE_ID, "workflow");

        assertTrue(route.isEnabled());
        assertEquals("ENGINE", route.source());
        assertEquals("/api/v1/config/workflow", route.routePrefix());
    }

    @Test
    void resolveRoute_moduleExisting_shouldRouteToLegacyPath() {
        when(catalogService.getByCode("SOULS")).thenReturn(
                Optional.of(def("SOULS", "PEOPLE", "EXISTING", true)));
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "SOULS"))
                .thenReturn(Optional.of(SpaceModule.builder()
                        .enabled(true)
                        .build()));

        ModuleRouter.ModuleRoute route = moduleRouter.resolveRoute(TENANT_ID, SPACE_ID, "SOULS");

        assertTrue(route.isEnabled());
        assertEquals("EXISTING", route.source());
        assertEquals("/api/v1/people/souls", route.routePrefix());
    }

    @Test
    void isModuleEnabledForSpace_unknownModule_shouldBeFalse() {
        when(catalogService.getByCode("inconnu")).thenReturn(Optional.empty());

        assertFalse(moduleRouter.isModuleEnabledForSpace(TENANT_ID, SPACE_ID, "inconnu"));
    }

    @Test
    void isModuleEnabledForSpace_sansConfigSpecifique_shouldDefaultEnabled() {
        when(catalogService.getByCode("people")).thenReturn(
                Optional.of(def("people", "PEOPLE", "CORE", true)));
        when(spaceModuleRepository.findByTenantIdAndSpaceIdAndModuleCode(TENANT_ID, SPACE_ID, "people"))
                .thenReturn(Optional.empty());

        assertTrue(moduleRouter.isModuleEnabledForSpace(TENANT_ID, SPACE_ID, "people"));
    }
}