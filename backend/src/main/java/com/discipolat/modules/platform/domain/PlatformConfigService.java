package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.api.MenuGateInfo;
import com.discipolat.modules.platform.api.MenuOrderItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration de la plateforme : modules activables/désactivables et
 * menus configurables. Toute modification est tracée dans le journal d'audit.
 */
@Service
@Transactional
public class PlatformConfigService {

    private final PlatformModuleRepository moduleRepository;
    private final MenuEntryRepository menuRepository;
    private final AuditService auditService;
    private final ConfigRevisionService revisionService;

    public PlatformConfigService(PlatformModuleRepository moduleRepository,
                                 MenuEntryRepository menuRepository,
                                 AuditService auditService,
                                 ConfigRevisionService revisionService) {
        this.moduleRepository = moduleRepository;
        this.menuRepository = menuRepository;
        this.auditService = auditService;
        this.revisionService = revisionService;
    }

    /* ============================== Modules ============================== */

    public List<PlatformModule> listModules() {
        return moduleRepository.findAllByOrderByOrdreAsc();
    }

    public Set<String> enabledModuleKeys() {
        return moduleRepository.findAll().stream()
                .filter(PlatformModule::isEnabled)
                .map(PlatformModule::getKey)
                .collect(Collectors.toSet());
    }

    public PlatformModule getModule(String key) {
        return moduleRepository.findById(key)
                .orElseThrow(() -> new EntityNotFoundException("PlatformModule", "key", key));
    }

    public PlatformModule toggleModule(String key, boolean enabled) {
        PlatformModule module = getModule(key);
        if (module.isEnabled() != enabled) {
            module.setEnabled(enabled);
            moduleRepository.save(module);
            auditService.logSimple(enabled ? "MODULE_ENABLED" : "MODULE_DISABLED", "PLATFORM_MODULE", null);
            revisionService.record("PLATFORM_MODULE", key,
                    enabled ? "MODULE_ENABLED" : "MODULE_DISABLED",
                    Map.of("enabled", enabled));
        }
        return module;
    }

    public PlatformModule createModule(PlatformModule module) {
        if (module.getKey() == null || module.getKey().isBlank()) {
            throw new IllegalArgumentException("La clé du module est obligatoire");
        }
        module.setKey(module.getKey().trim().toUpperCase());
        moduleRepository.save(module);
        auditService.logSimple("MODULE_CREATED", "PLATFORM_MODULE", null);
        revisionService.record("PLATFORM_MODULE", module.getKey(), "MODULE_CREATED",
                modulePayload(module));
        return module;
    }

    public PlatformModule updateModule(String key, PlatformModule request) {
        PlatformModule module = getModule(key);
        Map<String, Object> before = modulePayload(module);
        if (request.getLabel() != null && !request.getLabel().isBlank()) module.setLabel(request.getLabel());
        if (request.getDescription() != null) module.setDescription(request.getDescription());
        if (request.getIcon() != null) module.setIcon(request.getIcon());
        if (request.getSection() != null && !request.getSection().isBlank()) module.setSection(request.getSection());
        module.setOrdre(request.getOrdre());
        moduleRepository.save(module);
        auditService.logSimple("MODULE_UPDATED", "PLATFORM_MODULE", null);
        revisionService.record("PLATFORM_MODULE", key, "MODULE_UPDATED",
                Map.of("before", before, "after", modulePayload(module)));
        return module;
    }

    public void deleteModule(String key) {
        PlatformModule module = getModule(key);
        long linkedMenus = menuRepository.findAll().stream()
                .filter(m -> key.equals(m.getModuleKey())).count();
        if (linkedMenus > 0) {
            throw new IllegalStateException(
                    "Impossible de supprimer le module : " + linkedMenus + " menu(s) y sont rattachés.");
        }
        moduleRepository.delete(module);
        auditService.logSimple("MODULE_DELETED", "PLATFORM_MODULE", null);
        revisionService.record("PLATFORM_MODULE", key, "MODULE_DELETED", modulePayload(module));
    }

    /* ============================== Menus ============================== */

    /**
     * Menus visibles pour un utilisateur donné :
     * - menu actif
     * - module rattaché actif (ou menu autonome)
     * - au moins un des rôles de l'utilisateur autorisé (ou liste vide = tous)
     */
    @Transactional(readOnly = true)
    public List<MenuEntry> menusForRoles(Collection<String> userRoles) {
        Set<String> enabledModules = enabledModuleKeys();
        Set<String> roles = userRoles == null ? Set.of() : userRoles.stream()
                .map(String::toUpperCase).collect(Collectors.toSet());
        return menuRepository.findByEnabledTrueOrderBySectionAscOrdreAsc().stream()
                .filter(m -> m.getModuleKey() == null || enabledModules.contains(m.getModuleKey()))
                .filter(m -> roles.isEmpty() || m.getRoles() == null || m.getRoles().isEmpty()
                        || m.getRoles().stream().map(String::toUpperCase).anyMatch(roles::contains))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuEntry> listAllMenus() {
        return menuRepository.findAllByOrderBySectionAscOrdreAsc();
    }

    /**
     * Informations de gating : pour chaque menu visible (rôle) rattaché à un
     * module, l'état d'activation du module — même si celui-ci est désactivé.
     */
    @Transactional(readOnly = true)
    public List<MenuGateInfo> gateInfo(Collection<String> userRoles) {
        Set<String> enabledModules = enabledModuleKeys();
        Set<String> roles = userRoles == null ? Set.of() : userRoles.stream()
                .map(String::toUpperCase).collect(Collectors.toSet());
        return menuRepository.findByEnabledTrueOrderBySectionAscOrdreAsc().stream()
                .filter(m -> m.getModuleKey() != null)
                .filter(m -> roles.isEmpty() || m.getRoles() == null || m.getRoles().isEmpty()
                        || m.getRoles().stream().map(String::toUpperCase).anyMatch(roles::contains))
                .map(m -> new MenuGateInfo(m.getHref(), m.getModuleKey(), enabledModules.contains(m.getModuleKey())))
                .toList();
    }

    public MenuEntry createMenu(MenuEntry menu) {
        if (menu.getKey() == null || menu.getKey().isBlank()) {
            throw new IllegalArgumentException("La clé du menu est obligatoire");
        }
        menuRepository.save(menu);
        auditService.logSimple("MENU_CREATED", "PLATFORM_MENU", menu.getId());
        revisionService.record("PLATFORM_MENU", menu.getKey(), "MENU_CREATED", menuPayload(menu));
        return menu;
    }

    public MenuEntry updateMenu(UUID id, MenuEntry request) {
        MenuEntry menu = menuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuEntry", id));
        Map<String, Object> before = menuPayload(menu);
        if (request.getLabel() != null && !request.getLabel().isBlank()) menu.setLabel(request.getLabel());
        if (request.getHref() != null && !request.getHref().isBlank()) menu.setHref(request.getHref());
        if (request.getIcon() != null) menu.setIcon(request.getIcon());
        if (request.getSection() != null && !request.getSection().isBlank()) menu.setSection(request.getSection());
        if (request.getRoles() != null) menu.setRoles(new ArrayList<>(request.getRoles()));
        if (request.getModuleKey() != null) menu.setModuleKey(request.getModuleKey());
        menu.setEnabled(request.isEnabled());
        menu.setOrdre(request.getOrdre());
        menuRepository.save(menu);
        auditService.logSimple("MENU_UPDATED", "PLATFORM_MENU", menu.getId());
        revisionService.record("PLATFORM_MENU", menu.getKey(), "MENU_UPDATED",
                Map.of("before", before, "after", menuPayload(menu)));
        return menu;
    }

    public void deleteMenu(UUID id) {
        MenuEntry menu = menuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MenuEntry", id));
        menuRepository.delete(menu);
        auditService.logSimple("MENU_DELETED", "PLATFORM_MENU", id);
        revisionService.record("PLATFORM_MENU", menu.getKey(), "MENU_DELETED", menuPayload(menu));
    }

    /** Réordonne les menus de la section donnée. */
    public List<MenuEntry> reorderMenus(List<MenuOrderItem> items) {
        List<MenuEntry> updated = new ArrayList<>();
        for (MenuOrderItem item : items) {
            menuRepository.findById(item.id()).ifPresent(menu -> {
                if (item.ordre() != null) menu.setOrdre(item.ordre());
                if (item.section() != null && !item.section().isBlank()) menu.setSection(item.section());
                updated.add(menuRepository.save(menu));
            });
        }
        auditService.logSimple("MENUS_REORDERED", "PLATFORM_MENU", null);
        revisionService.record("PLATFORM_MENU", "reorder", "MENUS_REORDERED",
                Map.of("ordre", items.stream()
                        .map(MenuOrderItem::id)
                        .collect(Collectors.toList())));
        return updated;
    }

    /* ====================== Helpers de versionnage ====================== */

    private Map<String, Object> modulePayload(PlatformModule m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", m.getLabel());
        map.put("description", m.getDescription());
        map.put("icon", m.getIcon());
        map.put("section", m.getSection());
        map.put("enabled", m.isEnabled());
        map.put("ordre", m.getOrdre());
        return map;
    }

    private Map<String, Object> menuPayload(MenuEntry m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", m.getLabel());
        map.put("href", m.getHref());
        map.put("icon", m.getIcon());
        map.put("section", m.getSection());
        map.put("ordre", m.getOrdre());
        map.put("roles", m.getRoles());
        map.put("moduleKey", m.getModuleKey());
        map.put("enabled", m.isEnabled());
        return map;
    }
}
