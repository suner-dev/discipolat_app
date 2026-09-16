package com.discipolat.modules.tenants.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * G1.7 §53 — Moteur de résolution d'héritage de configuration.
 *
 * Chaîne : tenant → organisation → campus → ministère → département → famille → sous-équipe
 * Chaque nœud peut avoir : DEFAULT (valeurs par défaut) | INHERITED (hérite du parent) | OVERRIDDEN (redéfinit et stoppe la remontée).
 *
 * La résolution remonte la chaîne parente ; un OVERRIDDEN stoppe la remontée.
 * Le résultat est mis en cache dans `resolved_config_json` et invalidé par événement `config-changed`.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationResolver {

    private final OrganizationNodeRepository orgNodeRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final TenantFeatureRepository tenantFeatureRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    // Clés de configuration supportées pour la résolution
    private static final Set<String> RESOLVABLE_KEYS = Set.of(
            "branding", "colors", "fonts", "locale", "timezone", "currency",
            "modules", "features", "limits", "workflows", "statuses", "customFields",
            "notifications", "integrations", "ui", "access"
    );

    /**
     * Résout la configuration effective pour un nœud organisationnel.
     * Parcourt la chaîne d'héritage (DEFAULT → INHERITED → OVERRIDDEN stoppe).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolveConfig(UUID tenantId, UUID nodeId, Set<String> keys) {
        OrganizationNode node = orgNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Nœud non trouvé: " + nodeId));

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Accès cross-tenant interdit");
        }

        Set<String> keysToResolve = keys != null && !keys.isEmpty() ? keys : RESOLVABLE_KEYS;
        Map<String, Object> result = new LinkedHashMap<>();

        // Collecter la chaîne d'héritage : du nœud vers la racine
        List<OrganizationNode> chain = collectInheritanceChain(node);

        // Parcourir la chaîne du bas vers le haut (enfant → parent)
        for (OrganizationNode current : chain) {
            Map<String, Object> nodeConfig = getNodeConfig(current);
            
            for (String key : keysToResolve) {
                if (result.containsKey(key)) {
                    continue; // Déjà résolu par un descendant (OVERRIDDEN ou valeur explicite)
                }
                
                OrganizationNode.ConfigSource source = current.getConfigSource();
                Object value = nodeConfig.get(key);
                
                if (source == OrganizationNode.ConfigSource.OVERRIDDEN && value != null) {
                    // OVERRIDDEN : prend la valeur et arrête la remontée pour cette clé
                    result.put(key, value);
                } else if (source == OrganizationNode.ConfigSource.INHERITED && value != null) {
                    // INHERITED : prend la valeur si pas déjà définie, continue la remontée
                    result.put(key, value);
                } else if (source == OrganizationNode.ConfigSource.DEFAULT && value != null) {
                    // DEFAULT : valeur par défaut du nœud (ex: settings du tenant racine)
                    result.put(key, value);
                }
            }

            // Si toutes les clés sont résolues, on peut s'arrêter
            if (result.size() >= keysToResolve.size()) {
                break;
            }
        }

        return result;
    }

    /**
     * Collecte la chaîne d'héritage du nœud vers la racine.
     */
    private List<OrganizationNode> collectInheritanceChain(OrganizationNode node) {
        List<OrganizationNode> chain = new ArrayList<>();
        OrganizationNode current = node;
        
        while (current != null) {
            chain.add(current);
            if (current.getParentId() != null) {
                current = orgNodeRepository.findById(current.getParentId()).orElse(null);
            } else {
                break; // Racine atteinte
            }
        }
        
        return chain;
    }

    /**
     * Extrait la configuration brute d'un nœud selon son type.
     */
    private Map<String, Object> getNodeConfig(OrganizationNode node) {
        Map<String, Object> config = new HashMap<>();

        try {
            // 1. Métadonnées locales du nœud (metadata_json)
            if (node.getMetadataJson() != null) {
                config.putAll(objectMapper.readValue(node.getMetadataJson(), Map.class));
            }

            // 2. Si nœud racine du tenant, ajouter TenantSettings
            if (node.getParentId() == null && node.getType() == OrganizationNodeType.ROOT_CHURCH) {
                tenantSettingsRepository.findByTenantId(node.getTenantId())
                        .ifPresent(settings -> {
                            config.put("branding", mapBranding(settings));
                            config.put("colors", mapColors(settings));
                            config.put("fonts", mapFonts(settings));
                            config.put("locale", settings.getLocale());
                            config.put("timezone", settings.getTimezone());
                            config.put("currency", settings.getCurrency());
                        });
            }

            // 3. Modules/features du tenant
            List<TenantFeature> features = tenantFeatureRepository.findByTenantId(node.getTenantId());
            if (!features.isEmpty()) {
                Map<String, Object> modulesConfig = new HashMap<>();
                for (TenantFeature f : features) {
                    Map<String, Object> modConfig = new HashMap<>();
                    modConfig.put("enabled", f.getEnabled());
                    if (f.getConfigurationJson() != null) {
                        modConfig.put("config", f.getConfigurationJson());
                    }
                    if (f.getLimitsJson() != null) {
                        modConfig.put("limits", f.getLimitsJson());
                    }
                    modulesConfig.put(f.getModuleCode(), modConfig);
                }
                config.put("modules", modulesConfig);
            }

        } catch (Exception e) {
            log.warn("Erreur lecture config nœud {}: {}", node.getId(), e.getMessage());
        }

        return config;
    }

    // Helpers pour mapper TenantSettings
    private Map<String, Object> mapBranding(TenantSettings s) {
        Map<String, Object> m = new HashMap<>();
        m.put("businessName", s.getBusinessName());
        m.put("slogan", s.getSlogan());
        m.put("logoUrl", s.getLogoUrl());
        m.put("logoDarkUrl", s.getLogoDarkUrl());
        m.put("coverUrl", s.getCoverUrl());
        m.put("faviconUrl", s.getFaviconUrl());
        return m;
    }

    private Map<String, Object> mapColors(TenantSettings s) {
        Map<String, Object> m = new HashMap<>();
        m.put("primary", s.getPrimaryColor());
        m.put("secondary", s.getSecondaryColor());
        m.put("accent", s.getAccentColor());
        m.put("surface", s.getSurfaceColor());
        m.put("background", s.getBackgroundColor());
        m.put("textPrimary", s.getTextPrimaryColor());
        m.put("textSecondary", s.getTextSecondaryColor());
        m.put("success", s.getSuccessColor());
        m.put("warning", s.getWarningColor());
        m.put("error", s.getErrorColor());
        m.put("info", s.getInfoColor());
        return m;
    }

    private Map<String, Object> mapFonts(TenantSettings s) {
        Map<String, Object> m = new HashMap<>();
        m.put("primary", s.getPrimaryFont());
        m.put("secondary", s.getSecondaryFont());
        m.put("heading", s.getHeadingFont());
        m.put("mono", s.getMonoFont());
        return m;
    }

    /**
     * Invalide le cache résolu pour un nœud et tous ses descendants.
     * À appeler après toute mutation de config (settings, features, metadata, config_source).
     */
    @Transactional
    public void invalidateResolvedConfig(UUID tenantId, UUID nodeId) {
        // Invalider le nœud
        orgNodeRepository.findById(nodeId).ifPresent(n -> {
            n.setResolvedConfigJson(null);
            orgNodeRepository.save(n);
        });

        // Invalider récursivement les descendants
        List<OrganizationNode> descendants = orgNodeRepository.findDescendantsByNodeId(tenantId, nodeId);
        for (OrganizationNode desc : descendants) {
            desc.setResolvedConfigJson(null);
        }
        orgNodeRepository.saveAll(descendants);

        // Publier événement pour WebSocket (les clients rechargeront)
        // TODO: injecter SimpMessagingTemplate ou ApplicationEventPublisher
    }

    /**
     * Met à jour le config_source d'un nœud et invalide le cache.
     */
    @Transactional
    public void setConfigSource(UUID tenantId, UUID nodeId, OrganizationNode.ConfigSource source, UUID userId) {
        OrganizationNode node = orgNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Nœud non trouvé: " + nodeId));

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Accès cross-tenant interdit");
        }

        node.setConfigSource(source);
        node.setResolvedConfigJson(null); // invalider cache
        orgNodeRepository.save(node);

        // Invalider descendants
        invalidateResolvedConfig(tenantId, nodeId);

        // Audit
        // auditService.log(userId, tenantId, "CONFIG_SOURCE_CHANGED", "ORG_NODE", nodeId, ...);
    }

    /**
     * Met à jour la configuration locale (metadata_json) d'un nœud.
     * Si config_source = OVERRIDDEN, les valeurs prennent le pas sur l'héritage.
     */
    @Transactional
    public void updateLocalConfig(UUID tenantId, UUID nodeId, Map<String, Object> config, UUID userId) {
        OrganizationNode node = orgNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Nœud non trouvé: " + nodeId));

        if (!node.getTenantId().equals(tenantId)) {
            throw new SecurityException("Accès cross-tenant interdit");
        }

        try {
            String json = objectMapper.writeValueAsString(config);
            node.setMetadataJson(json);
            node.setResolvedConfigJson(null);
            orgNodeRepository.save(node);

            // Invalider descendants si ce nœud est OVERRIDDEN
            if (node.getConfigSource() == OrganizationNode.ConfigSource.OVERRIDDEN) {
                invalidateResolvedConfig(tenantId, nodeId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation config: " + e.getMessage(), e);
        }
    }
}