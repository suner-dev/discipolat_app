package com.discipolat.modules.configuration.domain;

import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * G1.7 §53 — Moteur de résolution de configuration par héritage.
 *
 * Chaine : tenant -> organisation -> campus -> ministere -> departement -> famille -> sous-equipe.
 * Chaque noeud declare sa source :
 *   - DEFAULT    : utilise les valeurs par defaut du tenant
 *   - INHERITED  : herite du parent le plus proche
 *   - OVERRIDDEN : redefinit et STOPPE la remontee vers le parent
 *
 * La configuration resolue est mise en cache dans organization_nodes.resolved_config_json
 * et invalidee par l'evenement de domaine config-changed (outbox G2.8).
 */
@Service
@Transactional
public class ConfigurationResolver {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationResolver.class);

    private final OrganizationNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;

    public ConfigurationResolver(OrganizationNodeRepository nodeRepository, ObjectMapper objectMapper) {
        this.nodeRepository = nodeRepository;
        this.objectMapper = objectMapper;
    }

    /** Resolution complete d'un noeud (toutes les cles). */
    @Transactional(readOnly = true)
    public Map<String, Object> resolve(UUID tenantId, UUID unitId) {
        return resolve(tenantId, unitId, null);
    }

    /**
     * Resolution d'un noeud pour une liste de cles donnee.
     * Si keys est null ou vide, toutes les cles resolues sont retournees.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolve(UUID tenantId, UUID unitId, Collection<String> keys) {
        if (unitId == null) {
            return Map.of();
        }
        OrganizationNode node = nodeRepository.findById(unitId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .orElse(null);
        if (node == null) {
            return Map.of();
        }

        // Chaine du plus lointain ancetre (racine) vers le noeud courant.
        LinkedList<OrganizationNode> chain = new LinkedList<>();
        OrganizationNode current = node;
        int guard = 0;
        while (current != null && guard++ < 64) {
            chain.addFirst(current);
            if (current.getParentId() == null) {
                break;
            }
            current = nodeRepository.findById(current.getParentId()).orElse(null);
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (OrganizationNode n : chain) {
            // Un noeud OVERRIDDEN redefinit tout : on repart de sa propre base.
            if (n.getConfigSource() == OrganizationNode.ConfigSource.OVERRIDDEN) {
                resolved = new LinkedHashMap<>();
            }
            if (n.getMetadataJson() != null && !n.getMetadataJson().isBlank()) {
                resolved.putAll(readJson(n.getMetadataJson()));
            }
        }

        if (keys == null || keys.isEmpty()) {
            return resolved;
        }
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String key : keys) {
            if (resolved.containsKey(key)) {
                filtered.put(key, resolved.get(key));
            }
        }
        return filtered;
    }

    /**
     * Retourne l'origine (heritage) d'une cle : "DEFAULT" si non definie,
     * sinon le noeud qui a fourni la valeur effective.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolveWithOrigin(UUID tenantId, UUID unitId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (unitId == null) return result;
        OrganizationNode node = nodeRepository.findById(unitId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .orElse(null);
        if (node == null) return result;

        List<OrganizationNode> chain = new ArrayList<>();
        OrganizationNode current = node;
        int guard = 0;
        while (current != null && guard++ < 64) {
            chain.add(current);
            if (current.getParentId() == null) break;
            current = nodeRepository.findById(current.getParentId()).orElse(null);
        }
        Collections.reverse(chain); // racine -> noeud

        Map<String, Map<String, Object>> valueToOrigin = new LinkedHashMap<>();
        for (OrganizationNode n : chain) {
            if (n.getConfigSource() == OrganizationNode.ConfigSource.OVERRIDDEN) {
                valueToOrigin.clear();
            }
            if (n.getMetadataJson() != null && !n.getMetadataJson().isBlank()) {
                for (Map.Entry<String, Object> e : readJson(n.getMetadataJson()).entrySet()) {
                    Map<String, Object> origin = new LinkedHashMap<>();
                    origin.put("value", e.getValue());
                    origin.put("originNodeId", n.getId());
                    origin.put("originNodeName", n.getName());
                    origin.put("configSource", n.getConfigSource().name());
                    origin.put("inherited", n.getId() != unitId);
                    valueToOrigin.put(e.getKey(), origin);
                }
            }
        }
        valueToOrigin.forEach(result::put);
        return result;
    }

    /**
     * Recalcule et memorise la configuration resolue (cache) d'un noeud.
     * Appele apres un changement de configuration ; l'evenement config-changed
     * (outbox G2.8) declenche cette invalidation.
     */
    public Map<String, Object> recalculateAndCache(UUID tenantId, UUID unitId) {
        Map<String, Object> resolved = resolve(tenantId, unitId);
        nodeRepository.findById(unitId).ifPresent(n -> {
            n.setResolvedConfigJson(writeJson(resolved));
            nodeRepository.save(n);
        });
        return resolved;
    }

    /** Invalidation du cache (evenement config-changed). */
    public void invalidate(UUID tenantId, UUID unitId) {
        nodeRepository.findById(unitId)
                .filter(n -> n.getTenantId().equals(tenantId))
                .ifPresent(n -> {
                    n.setResolvedConfigJson(null);
                    nodeRepository.save(n);
                });
    }

    // ==================== helpers JSON ====================

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Configuration JSON illisible : {}", e.getMessage());
            return Map.of();
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Configuration JSON non serialisable : {}", e.getMessage());
            return null;
        }
    }
}
