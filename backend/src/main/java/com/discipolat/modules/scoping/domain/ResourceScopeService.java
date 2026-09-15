package com.discipolat.modules.scoping.domain;

import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * G1.8 §54 — Regles de cloisonnement GLOBAL / LOCAL.
 *
 * - GLOBAL  : visible et editable selon les permissions heritees du tenant.
 * - LOCAL   : visible uniquement dans son unite et ses descendants.
 *
 * Le filtrage se fait TOUJOURS sur `resource_scope + organization_unit_id`,
 * jamais sur le scope seul (regle contractuelle du master prompt).
 */
@Service
@Transactional(readOnly = true)
public class ResourceScopeService {

    private final OrganizationNodeRepository nodeRepository;

    public ResourceScopeService(OrganizationNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    /**
     * Identifiants d'unites visibles pour un observateur ayant le scope `viewerUnitId`.
     * Inclut l'unite elle-meme et tous ses descendants. Null = observateur tenant-wide
     * (voit tout, GLOBAL comme LOCAL).
     */
    public Set<UUID> visibleUnitIds(UUID tenantId, UUID viewerUnitId) {
        if (viewerUnitId == null) {
            return null; // tenant-wide : aucun filtre d'unite
        }
        Set<UUID> visible = new LinkedHashSet<>();
        visible.add(viewerUnitId);
        nodeRepository.findById(viewerUnitId).ifPresent(node -> {
            if (node.getPath() != null) {
                for (OrganizationNode descendant : nodeRepository.findDescendants(tenantId, node.getPath() + ".*")) {
                    visible.add(descendant.getId());
                }
            }
        });
        return visible;
    }

    /** La ressource est-elle visible pour l'observateur ? */
    public boolean canSee(UUID tenantId, UUID viewerUnitId, ResourceScope scope, UUID resourceUnitId) {
        if (scope == null || scope.isGlobal()) {
            return true; // GLOBAL : visible a tout le tenant
        }
        Set<UUID> visible = visibleUnitIds(tenantId, viewerUnitId);
        if (visible == null) {
            return true; // observateur tenant-wide
        }
        return resourceUnitId != null && visible.contains(resourceUnitId);
    }

    /**
     * Filtre une collection de ressources selon les regles GLOBAL/LOCAL.
     * `unitOf` extrait l'unite d'une ressource (peut retourner null pour GLOBAL).
     */
    public <T> List<T> filterVisible(UUID tenantId, UUID viewerUnitId,
                                     Collection<T> resources,
                                     java.util.function.Function<T, ResourceScope> scopeOf,
                                     java.util.function.Function<T, UUID> unitOf) {
        Set<UUID> visible = visibleUnitIds(tenantId, viewerUnitId);
        if (visible == null) {
            return new ArrayList<>(resources);
        }
        return resources.stream()
                .filter(r -> {
                    ResourceScope scope = scopeOf.apply(r);
                    if (scope == null || scope.isGlobal()) return true;
                    UUID unit = unitOf.apply(r);
                    return unit != null && visible.contains(unit);
                })
                .toList();
    }
}
