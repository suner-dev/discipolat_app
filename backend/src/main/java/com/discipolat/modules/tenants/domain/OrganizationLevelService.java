package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §A — référentiel de <b>niveaux configurables</b>
 * par dénomination.
 *
 * <p>Chaque racine ({@code rootTenantId}) définit librement ses étapes de
 * hiérarchie (Région → Zone → Campus…), leurs libellés, leur ordre et leur
 * parenté. L'enum {@link OrganizationNodeType} n'est plus une contrainte
 * d'affichage : elle reste la sémantique interne, la table
 * {@code organization_levels} porte le métier.
 *
 * <p>Les niveaux sont scopés par <b>racine</b> et non par tenant : un membre
 * d'une église fille voit les niveaux de sa dénomination, pas les siens propres.
 */
@Service
public class OrganizationLevelService {

    private final OrganizationLevelRepository levelRepository;
    private final OrganizationNodeRepository nodeRepository;
    private final TenantRepository tenantRepository;

    public OrganizationLevelService(OrganizationLevelRepository levelRepository,
                                    OrganizationNodeRepository nodeRepository,
                                    TenantRepository tenantRepository) {
        this.levelRepository = levelRepository;
        this.nodeRepository = nodeRepository;
        this.tenantRepository = tenantRepository;
    }

    /** Racine de l'organisation courante (détection du réseau de référence). */
    private UUID currentRootTenantId() {
        UUID tenantId = TenantContext.requireTenantId();
        return tenantRepository.findById(tenantId)
                .map(Tenant::effectiveRootTenantId)
                .orElseThrow(() -> new DomainException("Organisation introuvable",
                        HttpStatus.NOT_FOUND, "ORG_ROOT_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    public List<OrganizationLevel> listAll() {
        return levelRepository.findByRootTenantIdOrderByDepthOrderAsc(currentRootTenantId());
    }

    @Transactional(readOnly = true)
    public List<OrganizationLevel> listActive() {
        return levelRepository.findByRootTenantIdAndActiveTrueOrderByDepthOrderAsc(currentRootTenantId());
    }

    /**
     * Crée un niveau pour la dénomination courante. La profondeur est soit
     * fournie, soit placée en fin de chaîne.
     */
    @Transactional
    public OrganizationLevel create(String name, String pluralName, String semanticType,
                                   Integer depthOrder, UUID parentLevelId,
                                   String icon, String color, Boolean branching) {
        UUID root = currentRootTenantId();
        if (name == null || name.isBlank()) {
            throw new DomainException("Nom de niveau requis", HttpStatus.BAD_REQUEST, "LEVEL_NAME_REQUIRED");
        }
        int order = depthOrder != null ? depthOrder : nextDepthOrder(root);
        if (levelRepository.findByRootTenantIdAndDepthOrder(root, order).isPresent()) {
            throw new DomainException("Un niveau existe déjà à cette profondeur (" + order + ")",
                    HttpStatus.CONFLICT, "LEVEL_ORDER_TAKEN");
        }
        if (parentLevelId != null) {
            requireSameRoot(root, parentLevelId);
        }
        OrganizationLevel level = OrganizationLevel.builder()
                .rootTenantId(root)
                .name(name.trim())
                .pluralName(pluralName == null ? null : pluralName.trim())
                .semanticType(semanticType == null || semanticType.isBlank() ? "CUSTOM" : semanticType.trim().toUpperCase())
                .depthOrder(order)
                .parentLevelId(parentLevelId)
                .icon(icon)
                .color(color)
                .branching(branching == null ? Boolean.TRUE : branching)
                .active(true)
                .build();
        return levelRepository.save(level);
    }

    @Transactional
    public OrganizationLevel update(UUID levelId, String name, String pluralName, String description,
                                    String semanticType, Integer depthOrder, String icon, String color,
                                    Boolean branching, Boolean active) {
        OrganizationLevel level = requireOwnedLevel(levelId);
        if (name != null && !name.isBlank()) level.setName(name.trim());
        if (pluralName != null) level.setPluralName(pluralName.isBlank() ? null : pluralName.trim());
        if (description != null) level.setDescription(description);
        if (semanticType != null && !semanticType.isBlank()) level.setSemanticType(semanticType.trim().toUpperCase());
        if (icon != null) level.setIcon(icon);
        if (color != null) level.setColor(color);
        if (branching != null) level.setBranching(branching);
        if (active != null) level.setActive(active);
        if (depthOrder != null && !depthOrder.equals(level.getDepthOrder())) {
            UUID root = currentRootTenantId();
            if (levelRepository.findByRootTenantIdAndDepthOrder(root, depthOrder).isPresent()) {
                throw new DomainException("Profondeur déjà occupée (" + depthOrder + ")",
                        HttpStatus.CONFLICT, "LEVEL_ORDER_TAKEN");
            }
            level.setDepthOrder(depthOrder);
        }
        return levelRepository.save(level);
    }

    /**
     * Supprime un niveau. Refusé (409) si des nœuds y sont encore rattachés :
     * l'admin doit d'abord réaffecter ces nœuds à un autre niveau.
     */
    @Transactional
    public void delete(UUID levelId) {
        OrganizationLevel level = requireOwnedLevel(levelId);
        long used = nodeRepository.countByLevelId(level.getId());
        if (used > 0) {
            throw new DomainException("Niveau encore utilisé par " + used + " nœud(s) — réaffectez-les d'abord",
                    HttpStatus.CONFLICT, "LEVEL_IN_USE");
        }
        levelRepository.delete(level);
    }

    /**
     * Graine des niveaux par défaut moulés sur la sémantique V2, si la racine
     * n'a aucun niveau (première ouverture de l'éditeur). Idempotent.
     */
    @Transactional
    public List<OrganizationLevel> ensureDefaults() {
        UUID root = currentRootTenantId();
        List<OrganizationLevel> existing = levelRepository.findByRootTenantIdOrderByDepthOrderAsc(root);
        if (!existing.isEmpty()) {
            return existing;
        }
        OrganizationLevel region = saveLevel(root, "Région", "Régions", "REGION", 1, null);
        OrganizationLevel district = saveLevel(root, "District", "Districts", "DISTRICT", 2, region.getId());
        saveLevel(root, "Campus", "Campus", "CAMPUS", 3, district.getId());
        saveLevel(root, "Groupe", "Groupes", "GROUP", 4, district.getId());
        return levelRepository.findByRootTenantIdOrderByDepthOrderAsc(root);
    }

    private OrganizationLevel saveLevel(UUID root, String name, String plural, String semantic,
                                        int order, UUID parent) {
        return levelRepository.save(OrganizationLevel.builder()
                .rootTenantId(root).name(name).pluralName(plural).semanticType(semantic)
                .depthOrder(order).parentLevelId(parent).active(true).branching(true).build());
    }

    private int nextDepthOrder(UUID root) {
        return levelRepository.findByRootTenantIdOrderByDepthOrderAsc(root).stream()
                .map(OrganizationLevel::getDepthOrder)
                .max(Integer::compareTo)
                .map(m -> m + 1)
                .orElse(1);
    }

    private OrganizationLevel requireOwnedLevel(UUID levelId) {
        OrganizationLevel level = levelRepository.findById(levelId)
                .orElseThrow(() -> new DomainException("Niveau introuvable",
                        HttpStatus.NOT_FOUND, "LEVEL_NOT_FOUND"));
        if (!level.getRootTenantId().equals(currentRootTenantId())) {
            // Isolation stricte : on ne révèle même pas l'existence d'un niveau
            // d'une autre dénomination (même motif que le 404 inter-tenant V2).
            throw new DomainException("Niveau introuvable", HttpStatus.NOT_FOUND, "LEVEL_NOT_FOUND");
        }
        return level;
    }

    private void requireSameRoot(UUID root, UUID parentLevelId) {
        OrganizationLevel parent = levelRepository.findById(parentLevelId)
                .orElseThrow(() -> new DomainException("Niveau parent introuvable",
                        HttpStatus.BAD_REQUEST, "PARENT_LEVEL_NOT_FOUND"));
        if (!parent.getRootTenantId().equals(root)) {
            throw new DomainException("Le niveau parent n'appartient pas à cette dénomination",
                    HttpStatus.BAD_REQUEST, "PARENT_LEVEL_FOREIGN");
        }
    }
}
