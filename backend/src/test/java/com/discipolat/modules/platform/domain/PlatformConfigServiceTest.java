package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.api.MenuGateInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformConfigServiceTest {

    @Mock private PlatformModuleRepository moduleRepository;
    @Mock private MenuEntryRepository menuRepository;
    @Mock private AuditService auditService;
    @Mock private ConfigRevisionService revisionService;

    private PlatformConfigService service;

    @BeforeEach
    void setUp() {
        service = new PlatformConfigService(moduleRepository, menuRepository, auditService, revisionService);
    }

    private MenuEntry menu(String key, String href, String moduleKey, List<String> roles, boolean enabled) {
        return MenuEntry.builder()
                .id(UUID.randomUUID()).key(key).label(key).href(href)
                .section("Section").ordre(1).roles(roles).moduleKey(moduleKey).enabled(enabled)
                .build();
    }

    private PlatformModule module(String key, boolean enabled) {
        return PlatformModule.builder().key(key).label(key).section("S").enabled(enabled).build();
    }

    @Test
    void menusForRoles_filtersByRoleVisible() {
        MenuEntry souls = menu("souls", "/souls", "SOULS", List.of("ADMIN", "PASTEUR", "RESPONSABLE", "CHEF_DE_FAMILLE", "FAISEUR"), true);
        MenuEntry audit = menu("audit", "/audit", "AUDIT", List.of("ADMIN", "PASTEUR"), true);
        when(moduleRepository.findAll()).thenReturn(List.of(module("SOULS", true), module("AUDIT", true)));
        when(menuRepository.findByEnabledTrueOrderBySectionAscOrdreAsc()).thenReturn(List.of(souls, audit));

        List<MenuEntry> result = service.menusForRoles(List.of("FAISEUR"));

        assertThat(result).extracting(MenuEntry::getKey).containsExactly("souls").doesNotContain("audit");
    }

    @Test
    void menusForRoles_hidesMenusOfDisabledModules() {
        MenuEntry souls = menu("souls", "/souls", "SOULS", List.of("ADMIN"), true);
        when(moduleRepository.findAll()).thenReturn(List.of(module("SOULS", false)));
        when(menuRepository.findByEnabledTrueOrderBySectionAscOrdreAsc()).thenReturn(List.of(souls));

        assertThat(service.menusForRoles(List.of("ADMIN"))).isEmpty();
    }

    @Test
    void menusForRoles_keepsAutonomousMenus() {
        MenuEntry profile = menu("profile", "/profile", null, List.of("ADMIN"), true);
        when(moduleRepository.findAll()).thenReturn(List.of());
        when(menuRepository.findByEnabledTrueOrderBySectionAscOrdreAsc()).thenReturn(List.of(profile));

        assertThat(service.menusForRoles(List.of("ADMIN"))).hasSize(1);
    }

    @Test
    void gateInfo_reportsDisabledModules() {
        MenuEntry souls = menu("souls", "/souls", "SOULS", List.of("ADMIN"), true);
        when(moduleRepository.findAll()).thenReturn(List.of(module("SOULS", false)));
        when(menuRepository.findByEnabledTrueOrderBySectionAscOrdreAsc()).thenReturn(List.of(souls));

        List<MenuGateInfo> gates = service.gateInfo(List.of("ADMIN"));

        assertThat(gates).singleElement().satisfies(g -> {
            assertThat(g.href()).isEqualTo("/souls");
            assertThat(g.moduleKey()).isEqualTo("SOULS");
            assertThat(g.moduleEnabled()).isFalse();
        });
    }

    @Test
    void toggleModule_persistsAndAudits() {
        PlatformModule module = module("SOULS", true);
        when(moduleRepository.findById("SOULS")).thenReturn(Optional.of(module));

        service.toggleModule("SOULS", false);

        assertThat(module.isEnabled()).isFalse();
        verify(moduleRepository).save(module);
        verify(auditService).logSimple("MODULE_DISABLED", "PLATFORM_MODULE", null);
    }

    @Test
    void toggleModule_recordsRevision() {
        PlatformModule module = module("SOULS", true);
        when(moduleRepository.findById("SOULS")).thenReturn(Optional.of(module));

        service.toggleModule("SOULS", false);

        verify(revisionService).record(eq("PLATFORM_MODULE"), eq("SOULS"), eq("MODULE_DISABLED"), anyMap());
    }

    @Test
    void createMenu_recordsRevision() {
        MenuEntry soulMenu = menu("souls", "/souls", "SOULS", List.of("ADMIN"), true);
        service.createMenu(soulMenu);

        verify(revisionService).record(eq("PLATFORM_MENU"), eq("souls"), eq("MENU_CREATED"), anyMap());
    }

    @Test
    void toggleModule_unknownKeyThrows() {
        when(moduleRepository.findById("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.toggleModule("NOPE", true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createModule_requiresKey() {
        assertThatThrownBy(() -> service.createModule(module("", true)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(moduleRepository, never()).save(any());
    }
}