package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * LOT 2 §GR — service des groupes de navigation (onglets groupés).
 *
 * <p><b>Règle d'or du module : rien ne disparaît.</b> Un groupe est une
 * <i>regroupement</i>, jamais un filtre. La suppression d'un groupe ne retire
 * aucune entrée du menu : les entrées simplement retombent dans le groupe
 * « non regroupé » (rendu en dernier, comme aujourd'hui). C'est ce qui permet à
 * une église de réorganiser son menu sans jamais perdre une fonctionnalité.
 *
 * <p><b>Héritage.</b> {@link #resolveForTenant} fusionne les groupes globaux
 * ({@code tenant_id IS NULL}) et les groupes de l'église, l'église
 * <b>écrasant</b> le global de même {@code key}.
 */
@Service
@Transactional
public class NavigationGroupService {

    private final NavigationGroupRepository groupRepository;
    private final NavigationGroupItemRepository itemRepository;

    public NavigationGroupService(NavigationGroupRepository groupRepository,
                                  NavigationGroupItemRepository itemRepository) {
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
    }

    /* ------------------------------------------------------------------ */
    /* Lecture / résolution                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Groupes <b>effectifs</b> pour une église : globaux + propres à l'église,
     * l'église écrasant le global de même {@code key}, puis filtrés sur les
     * rôles de l'utilisateur et l'activation.
     *
     * @param tenantId église courante ({@code null} = contexte plateforme)
     * @param roles    rôles de l'utilisateur ({@code roles} vide = aucun filtre)
     */
    @Transactional(readOnly = true)
    public List<NavigationGroup> resolveForTenant(UUID tenantId, Collection<String> roles) {
        List<NavigationGroup> visible = new ArrayList<>();
        for (NavigationGroup group : groupRepository.findByTenantIdIsNullOrTenantId(tenantId)) {
            if (!group.isVisibleForRoles(roles)) {
                continue;
            }
            // Un groupe d'une église masque le global de même clé.
            if (group.getTenantId() == null && tenantId != null
                    && hasTenantOverride(tenantId, group.getKey())) {
                continue;
            }
            visible.add(group);
        }
        return visible.stream()
                .sorted(Comparator.comparing(NavigationGroup::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(NavigationGroup::getLabel, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private boolean hasTenantOverride(UUID tenantId, String key) {
        return groupRepository.findByTenantId(tenantId).stream()
                .anyMatch(g -> key != null && key.equals(g.getKey()));
    }

    /**
     * Affectations de toutes les entrées vers tous les groupes effectifs.
     *
     * @return {@code href -> Liste des groupes} (une entrée peut être
     *         référencée par plusieurs groupes si l'admin l'a fait ; le
     *         frontend la n'affiche qu'une fois)
     */
    @Transactional(readOnly = true)
    public Map<String, List<UUID>> assignmentsByHref(List<NavigationGroup> groups) {
        Map<String, List<UUID>> byHref = new LinkedHashMap<>();
        if (groups == null || groups.isEmpty()) {
            return byHref;
        }
        for (NavigationGroupItem item : itemRepository
                .findByGroupIdInOrderByDisplayOrderAsc(groups.stream().map(NavigationGroup::getId).toList())) {
            byHref.computeIfAbsent(item.getHref(), k -> new ArrayList<>()).add(item.getGroupId());
        }
        return byHref;
    }

    /** Vue « lecture » destinée au frontend/mobile : groupes + affectations. */
    @Transactional(readOnly = true)
    public Map<String, Object> navigationFor(UUID tenantId, Collection<String> roles) {
        List<NavigationGroup> groups = resolveForTenant(tenantId, roles);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groups", groups.stream().map(this::toMap).toList());
        payload.put("assignments", assignmentsByHref(groups));
        return payload;
    }

    /* ------------------------------------------------------------------ */
    /* Écriture (CRUD)                                                     */
    /* ------------------------------------------------------------------ */

    public NavigationGroup create(UUID tenantId, String key, String label, String description, String icon,
                                  UUID parentGroupId, Integer displayOrder, List<String> roles,
                                  String moduleKey, Boolean enabled, Boolean collapsedByDefault,
                                  Boolean showCount) {
        if (isBlank(key) || isBlank(label)) {
            throw new BusinessRuleException("Clé et libellé du groupe requis", "NAV_GROUP_KEY_LABEL_REQUIRED");
        }
        if (tenantId != null && groupRepository.findByTenantId(tenantId).stream()
                .anyMatch(g -> key.equalsIgnoreCase(g.getKey()))) {
            throw new BusinessRuleException("Un groupe porte déjà cette clé", "NAV_GROUP_KEY_DUPLICATE");
        }
        // Un parent d'une autre église serait une fuite : on refuse.
        if (parentGroupId != null) {
            NavigationGroup parent = requireGroup(parentGroupId);
            if (parent.getTenantId() == null && tenantId != null) {
                // Parent global : autorisé, il est visible de tous.
                parentGroupId = parent.getId();
            } else if (parent.getTenantId() != null && !parent.getTenantId().equals(tenantId)) {
                throw new BusinessRuleException("Parent appartenant à une autre église", "NAV_GROUP_PARENT_FOREIGN");
            }
        }
        NavigationGroup group = NavigationGroup.builder()
                .tenantId(tenantId)
                .key(key.trim())
                .label(label.trim())
                .description(description)
                .icon(icon)
                .parentGroupId(parentGroupId)
                .displayOrder(displayOrder == null ? 0 : displayOrder)
                .roles(roles == null ? new ArrayList<>() : new ArrayList<>(roles))
                .moduleKey(moduleKey)
                .enabled(enabled == null || enabled)
                .collapsedByDefault(collapsedByDefault == null || collapsedByDefault)
                .showCount(showCount != null && showCount)
                .build();
        return groupRepository.save(group);
    }

    public NavigationGroup update(UUID tenantId, UUID id, String label, String description, String icon,
                                  UUID parentGroupId, Integer displayOrder, List<String> roles,
                                  String moduleKey, Boolean enabled, Boolean collapsedByDefault,
                                  Boolean showCount) {
        NavigationGroup group = requireOwnedGroup(tenantId, id);
        if (!isBlank(label)) {
            group.setLabel(label.trim());
        }
        if (description != null) {
            group.setDescription(description);
        }
        if (icon != null) {
            group.setIcon(isBlank(icon) ? null : icon);
        }
        if (parentGroupId != null) {
            if (parentGroupId.equals(id)) {
                throw new BusinessRuleException("Un groupe ne peut pas être son propre parent", "NAV_GROUP_SELF_PARENT");
            }
            requireOwnedOrGlobalGroup(tenantId, parentGroupId);
            group.setParentGroupId(parentGroupId);
        }
        if (displayOrder != null) {
            group.setDisplayOrder(displayOrder);
        }
        if (roles != null) {
            group.setRoles(new ArrayList<>(roles));
        }
        if (moduleKey != null) {
            group.setModuleKey(isBlank(moduleKey) ? null : moduleKey);
        }
        if (enabled != null) {
            group.setEnabled(enabled);
        }
        if (collapsedByDefault != null) {
            group.setCollapsedByDefault(collapsedByDefault);
        }
        if (showCount != null) {
            group.setShowCount(showCount);
        }
        return groupRepository.save(group);
    }

    /**
     * Suppression d'un groupe. <b>Les entrées de menu ne sont PAS supprimées</b> :
     * elles retombent simplement non regroupées. Les sous-groupes et les
     * affectations suivent en cascade (FK {@code ON DELETE CASCADE}).
     */
    public void delete(UUID tenantId, UUID id) {
        NavigationGroup group = requireOwnedGroup(tenantId, id);
        // Un groupe encore parenté d'un autre groupe ne doit pas disparaître
        // sans laisse : on le désactive plutôt que de casser la hiérarchie.
        List<NavigationGroup> children = groupRepository.findByParentGroupId(id);
        if (!children.isEmpty()) {
            group.setEnabled(false);
            groupRepository.save(group);
            return;
        }
        itemRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }

    /** Remplace l'ensemble des affectations d'un groupe (écriture idempotente). */
    @Transactional
    public List<NavigationGroupItem> setItems(UUID tenantId, UUID groupId, List<ItemRef> refs) {
        NavigationGroup group = requireOwnedGroup(tenantId, groupId);
        itemRepository.deleteByGroupId(groupId);
        List<NavigationGroupItem> saved = new ArrayList<>();
        if (refs != null) {
            int order = 0;
            for (ItemRef ref : refs) {
                if (ref == null || isBlank(ref.href())) {
                    continue;
                }
                saved.add(itemRepository.save(NavigationGroupItem.builder()
                        .groupId(group.getId())
                        .tenantId(group.getTenantId())
                        .itemKey(ref.itemKey())
                        .href(ref.href().trim())
                        .displayOrder(ref.displayOrder() == null ? order++ : ref.displayOrder())
                        .build()));
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NavigationGroupItem> itemsOf(UUID tenantId, UUID groupId) {
        requireOwnedOrGlobalGroup(tenantId, groupId);
        return itemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers                                                             */
    /* ------------------------------------------------------------------ */

    /** Référence d'une entrée de menu à affecter à un groupe. */
    public record ItemRef(String itemKey, String href, Integer displayOrder) {
    }

    private NavigationGroup requireGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NavigationGroup", id));
    }

    /** Résout un groupe et vérifie qu'il appartient bien à l'église courante. */
    private NavigationGroup requireOwnedGroup(UUID tenantId, UUID id) {
        NavigationGroup group = requireGroup(id);
        if (group.getTenantId() == null) {
            throw new BusinessRuleException(
                    "Un groupe global se modifie depuis la console plateforme", "NAV_GROUP_GLOBAL_READONLY");
        }
        if (tenantId == null || !tenantId.equals(group.getTenantId())) {
            // 404 et non 403 : ne pas confirmer l'existence d'un groupe d'une autre église.
            throw new EntityNotFoundException("NavigationGroup", id);
        }
        return group;
    }

    private void requireOwnedOrGlobalGroup(UUID tenantId, UUID id) {
        NavigationGroup group = requireGroup(id);
        if (group.getTenantId() != null && (tenantId == null || !tenantId.equals(group.getTenantId()))) {
            throw new EntityNotFoundException("NavigationGroup", id);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public Map<String, Object> toMap(NavigationGroup group) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", group.getId());
        body.put("tenantId", group.getTenantId());
        body.put("key", group.getKey());
        body.put("label", group.getLabel());
        body.put("description", group.getDescription());
        body.put("icon", group.getIcon());
        body.put("parentGroupId", group.getParentGroupId());
        body.put("displayOrder", group.getDisplayOrder());
        body.put("roles", Optional.ofNullable(group.getRoles()).orElse(List.of()));
        body.put("moduleKey", group.getModuleKey());
        body.put("enabled", group.isEnabled());
        body.put("collapsedByDefault", group.isCollapsedByDefault());
        body.put("showCount", group.isShowCount());
        return body;
    }

    public Map<String, Object> toItemMap(NavigationGroupItem item) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", item.getId());
        body.put("groupId", item.getGroupId());
        body.put("itemKey", item.getItemKey());
        body.put("href", item.getHref());
        body.put("displayOrder", item.getDisplayOrder());
        return body;
    }

    /** Normalise une étiquette pour comparer sans casse ni accents. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}