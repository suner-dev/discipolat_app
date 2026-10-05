package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §D / T-B13 — modules par <b>nœud</b>.
 *
 * <p>V3-D : chaque église/campus <b>choisit</b> ses modules, INDÉPENDANT
 * par défaut. Activer un module sur la racine <b>n'active pas</b> les
 * enfants. La résolution ne remonte la chaîne que si le nœud est
 * explicitement {@code config_source = INHERITED} (confort optionnel,
 * jamais une contrainte). L'invalidation du cache de config résolue suit
 * toute mutation.
 */
@Service
@Transactional
public class OrganizationNodeFeatureService {

    private final OrganizationNodeFeatureRepository featureRepository;
    private final OrganizationNodeRepository nodeRepository;
    private final ConfigurationResolver configurationResolver;

    public OrganizationNodeFeatureService(OrganizationNodeFeatureRepository featureRepository,
                                          OrganizationNodeRepository nodeRepository,
                                          ConfigurationResolver configurationResolver) {
        this.featureRepository = featureRepository;
        this.nodeRepository = nodeRepository;
        this.configurationResolver = configurationResolver;
    }

    @Transactional(readOnly = true)
    public List<OrganizationNodeFeature> listForNode(UUID tenantId, UUID nodeId) {
        requireNodeInTenant(tenantId, nodeId);
        return featureRepository.findByTenantIdAndNodeId(tenantId, nodeId);
    }

    /**
     * Remplace la sélection de modules d'un nœud (défaut indépendant). Une
     * entrée {@code {code, enabled, configurationJson?}} upserte la ligne ;
     * les codes absents de la liste sont désactivés (suppression de la ligne
     * active) pour refléter fidèlement la sélection.
     */
    @Transactional
    public List<OrganizationNodeFeature> setModules(UUID tenantId, UUID nodeId, List<ModuleSelection> selections) {
        requireNodeInTenant(tenantId, nodeId);
        List<OrganizationNodeFeature> current = featureRepository.findByTenantIdAndNodeId(tenantId, nodeId);
        for (OrganizationNodeFeature f : current) {
            featureRepository.delete(f);
        }
        featureRepository.flush();
        for (ModuleSelection sel : selections) {
            if (sel == null || sel.code() == null || sel.code().isBlank()) continue;
            OrganizationNodeFeature f = OrganizationNodeFeature.builder()
                    .tenantId(tenantId)
                    .nodeId(nodeId)
                    .moduleCode(sel.code().trim())
                    .enabled(sel.enabled() == null ? Boolean.TRUE : sel.enabled())
                    .configurationJson(sel.configurationJson() == null ? Map.of() : sel.configurationJson())
                    .build();
            featureRepository.save(f);
        }
        configurationResolver.invalidateResolvedConfig(tenantId, nodeId);
        return featureRepository.findByTenantIdAndNodeId(tenantId, nodeId);
    }

    /**
     * Le module {@code moduleCode} est-il actif pour ce nœud ? indépendant
     * par défaut : on ne lit que la ligne du nœud, sauf {@code INHERITED}
     * explicite (on remonte alors la chaîne parente).
     */
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(UUID tenantId, UUID nodeId, String moduleCode) {
        OrganizationNode node = requireNodeInTenant(tenantId, nodeId);
        Optional<OrganizationNodeFeature> own = featureRepository.findByNodeIdAndModuleCode(nodeId, moduleCode);
        if (own.isPresent()) {
            return Boolean.TRUE.equals(own.get().getEnabled());
        }
        // Héritage OPTIONNEL (jamais implicite) : uniquement si le nœud déclare INHERITED.
        if (node.getConfigSource() == OrganizationNode.ConfigSource.INHERITED) {
            UUID parentId = node.getParentId();
            while (parentId != null) {
                OrganizationNode parent = nodeRepository.findById(parentId).orElse(null);
                if (parent == null || !parent.getTenantId().equals(tenantId)) break;
                Optional<OrganizationNodeFeature> pf = featureRepository.findByNodeIdAndModuleCode(parent.getId(), moduleCode);
                if (pf.isPresent()) {
                    return Boolean.TRUE.equals(pf.get().getEnabled());
                }
                if (parent.getConfigSource() != OrganizationNode.ConfigSource.INHERITED) break;
                parentId = parent.getParentId();
            }
        }
        return false; // indépendant par défaut : rien d'activé = désactivé
    }

    /** Agrégat « quels nœuds ont ce module ? » (comptage descendant §5.4/D). */
    @Transactional(readOnly = true)
    public long countNodesWithModule(UUID tenantId, String moduleCode) {
        return featureRepository.findByTenantIdAndModuleCodeAndEnabledTrue(tenantId, moduleCode).size();
    }

    private OrganizationNode requireNodeInTenant(UUID tenantId, UUID nodeId) {
        OrganizationNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("OrganizationNode", nodeId));
        if (!node.getTenantId().equals(tenantId)) {
            // Isolation stricte : un nœud étranger est un 404, pas un 403.
            throw new EntityNotFoundException("OrganizationNode", nodeId);
        }
        return node;
    }

    public record ModuleSelection(String code, Boolean enabled, Map<String, Object> configurationJson) {
    }
}
