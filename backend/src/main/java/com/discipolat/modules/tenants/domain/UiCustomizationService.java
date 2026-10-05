package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * LOT 2 §LB — service de paramétrage fin de l'interface.
 *
 * <p>Couvre les deux demandes « rien ne doit être codé en dur » :
 * <ul>
 *   <li><b>chaque nom</b> → {@link UiLabelOverride} : surcharge d'une clé i18n
 *       ou d'un libellé source, par église et par langue ;</li>
 *   <li><b>chaque bouton / fonctionnalité</b> → {@link UiPageFeature} : visible,
 *       masqué, renommé, sur un écran donné.</li>
 * </ul>
 *
 * <p><b>Règles de résolution, identiques pour les deux tables</b> :
 * <ol>
 *   <li>nœud → église → global (le plus spécifique gagne) ;</li>
 *   <li>pour les libellés, langue exacte avant « {@code *} » ;</li>
 *   <li>une ligne {@code enabled = false} est ignorée (désactivée, pas effacée) ;</li>
 *   <li>une clé <b>absente</b> retombe sur le comportement par défaut de
 *       l'application — jamais de blanc, jamais d'erreur.</li>
 * </ol>
 */
@Service
@Transactional
public class UiCustomizationService {

    private final UiLabelOverrideRepository labelRepository;
    private final UiPageFeatureRepository featureRepository;

    public UiCustomizationService(UiLabelOverrideRepository labelRepository,
                                 UiPageFeatureRepository featureRepository) {
        this.labelRepository = labelRepository;
        this.featureRepository = featureRepository;
    }

    /* ================================================================== */
    /* Libellés                                                            */
    /* ================================================================== */

    /**
     * Surcharges effectives pour une église, à plat {@code clé -> valeur} pour
     * une langue donnée. Sert au payload envoyé au frontend, qui l'applique
     * <b>avant</b> de consulter le dictionnaire i18n.
     */
    @Transactional(readOnly = true)
    public Map<String, String> resolvedLabels(UUID tenantId, String locale, UUID nodeId) {
        // Tri par SPÉCIFICITÉ DÉCROISSANTE puis « le premier arrivé garde la
        // place ». Sans ce tri, le résultat dépendrait de l'ordre arbitraire
        // renvoyé par la base : un nom affiché changerait au hasard entre deux
        // rafraîchissements. On rend donc la résolution déterministe.
        List<UiLabelOverride> candidates = new ArrayList<>(
                labelRepository.findByTenantIdIsNullOrTenantId(tenantId));
        candidates.sort(java.util.Comparator
                .comparingInt((UiLabelOverride candidate) -> specificity(candidate, tenantId, nodeId))
                .reversed());

        Map<String, String> resolved = new LinkedHashMap<>();
        for (UiLabelOverride override : candidates) {
            if (!override.isEnabled()) {
                continue;
            }
            // Un réglage de nœud ne s'applique qu'à ce nœud.
            if (override.getNodeId() != null
                    && (nodeId == null || !override.getNodeId().equals(nodeId))) {
                continue;
            }
            if (!localeMatches(override.getLocale(), locale)) {
                continue;
            }
            resolved.putIfAbsent(override.getLabelKey(), override.getValue());
        }
        return resolved;
    }

    /**
     * Degrés de spécificité : global (0) &lt; église (1) &lt; nœud (2).
     * L'ordre croissant + {@code putIfAbsent} fait gagner le plus spécifique.
     */
    private static int specificity(UiLabelOverride candidate, UUID tenantId, UUID nodeId) {
        if (candidate.getNodeId() != null && candidate.getNodeId().equals(nodeId)) {
            return 2;
        }
        if (candidate.getTenantId() != null && candidate.getTenantId().equals(tenantId)) {
            return 1;
        }
        return 0;
    }

    /** `true` si la langue de la surcharge s'applique à la locale demandée. */
    private static boolean localeMatches(String overrideLocale, String locale) {
        if (overrideLocale == null || UiLabelOverride.ANY_LOCALE.equals(overrideLocale)) {
            return true;
        }
        if (locale == null) {
            return false;
        }
        return overrideLocale.equalsIgnoreCase(locale)
                // « fr » couvre « fr-FR ».
                || locale.toLowerCase(Locale.ROOT).startsWith(overrideLocale.toLowerCase(Locale.ROOT) + "-");
    }

    /* ================================================================== */
    /* Fonctionnalités / boutons                                            */
    /* ================================================================== */

    /**
     * Réglages de fonctionnalités pour une église, à plat
     * {@code "pageKey:featureKey" -> réglage résolu}.
     *
     * <p>Une fonctionnalité <b>absente</b> de la réponse est <b>visible</b> :
     * voir {@link UiPageFeature} pour pourquoi le défaut est « ouvert ».
     */
    @Transactional(readOnly = true)
    public Map<String, UiPageFeature> resolvedFeatures(UUID tenantId, UUID nodeId) {
        List<UiPageFeature> candidates = new ArrayList<>(
                featureRepository.findByTenantIdIsNullOrTenantId(tenantId));
        // Même exigence de déterminisme que pour les libellés : tri par
        // spécificité DÉCROISSANTE puis « le premier arrivé garde la place ».
        candidates.sort(java.util.Comparator.comparingInt((UiPageFeature candidate) -> {
            if (candidate.getNodeId() != null && candidate.getNodeId().equals(nodeId)) {
                return 2;
            }
            return candidate.getTenantId() != null && candidate.getTenantId().equals(tenantId) ? 1 : 0;
        }).reversed());

        Map<String, UiPageFeature> resolved = new LinkedHashMap<>();
        for (UiPageFeature feature : candidates) {
            if (feature.getNodeId() != null
                    && (nodeId == null || !feature.getNodeId().equals(nodeId))) {
                continue;
            }
            resolved.putIfAbsent(featureKey(feature.getPageKey(), feature.getFeatureKey()), feature);
        }
        return resolved;
    }

    /** Clé composite stable d'une fonctionnalité. */
    public static String featureKey(String pageKey, String featureKey) {
        return pageKey + ":" + featureKey;
    }

    /* ================================================================== */
    /* Payload complet pour le frontend                                     */
    /* ================================================================== */

    /** Charge utile consommée par {@code useUiCustomization()} côté web/mobile. */
    @Transactional(readOnly = true)
    public Map<String, Object> customizationFor(UUID tenantId, String locale, UUID nodeId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("labels", resolvedLabels(tenantId, locale, nodeId));
        Map<String, Object> features = new LinkedHashMap<>();
        for (Map.Entry<String, UiPageFeature> entry : resolvedFeatures(tenantId, nodeId).entrySet()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pageKey", entry.getValue().getPageKey());
            body.put("featureKey", entry.getValue().getFeatureKey());
            body.put("enabled", entry.getValue().isEnabled());
            body.put("label", entry.getValue().getLabelOverride());
            body.put("displayOrder", entry.getValue().getDisplayOrder());
            body.put("moduleKey", entry.getValue().getModuleKey());
            features.put(entry.getKey(), body);
        }
        payload.put("features", features);
        return payload;
    }

    /* ================================================================== */
    /* Écriture                                                            */
    /* ================================================================== */

    public UiLabelOverride upsertLabel(UUID tenantId, UUID nodeId, String labelKey, String locale,
                                       String value, String description, Boolean enabled) {
        if (isBlank(labelKey) || isBlank(value)) {
            throw new BusinessRuleException("Clé et valeur du libellé requises", "UI_LABEL_KEY_VALUE_REQUIRED");
        }
        String effectiveLocale = isBlank(locale) ? UiLabelOverride.ANY_LOCALE : locale.trim();
        String effectiveKey = labelKey.trim();
        // On ne reprend que la ligne de CETTE église (ou le global si l'on est
        // en console plateforme) et de CETTE langue : sinon un réglage de nœud
        // ou une autre langue se ferait écraser par erreur.
        UiLabelOverride override = labelRepository
                .findByTenantIdAndLabelKey(tenantId, effectiveKey).stream()
                .filter(candidate -> effectiveLocale.equalsIgnoreCase(candidate.getLocale()))
                .filter(candidate -> java.util.Objects.equals(candidate.getNodeId(), nodeId))
                .findFirst()
                .orElseGet(() -> UiLabelOverride.builder()
                        .tenantId(tenantId)
                        .nodeId(nodeId)
                        .labelKey(effectiveKey)
                        .locale(effectiveLocale)
                        .build());
        override.setNodeId(nodeId);
        override.setValue(value.trim());
        if (description != null) {
            override.setDescription(description);
        }
        if (enabled != null) {
            override.setEnabled(enabled);
        }
        return labelRepository.save(override);
    }

    public void deleteLabel(UUID tenantId, UUID id) {
        UiLabelOverride override = labelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UiLabelOverride", id));
        requireOwned(tenantId, override.getTenantId());
        labelRepository.delete(override);
    }

    public UiPageFeature upsertFeature(UUID tenantId, UUID nodeId, String pageKey, String featureKey,
                                        String labelOverride, Boolean enabled,
                                        Integer displayOrder, String moduleKey, String description) {
        if (isBlank(pageKey) || isBlank(featureKey)) {
            throw new BusinessRuleException("Écran et fonctionnalité requis", "UI_FEATURE_KEYS_REQUIRED");
        }
        UiPageFeature feature = featureRepository.findByTenantIdAndPageKey(tenantId, pageKey.trim()).stream()
                .filter(candidate -> featureKey.trim().equals(candidate.getFeatureKey()))
                .findFirst()
                .orElseGet(() -> UiPageFeature.builder()
                        .tenantId(tenantId)
                        .nodeId(nodeId)
                        .pageKey(pageKey.trim())
                        .featureKey(featureKey.trim())
                        .build());
        feature.setNodeId(nodeId);
        feature.setLabelOverride(isBlank(labelOverride) ? null : labelOverride.trim());
        if (enabled != null) {
            feature.setEnabled(enabled);
        }
        if (displayOrder != null) {
            feature.setDisplayOrder(displayOrder);
        }
        if (moduleKey != null) {
            feature.setModuleKey(isBlank(moduleKey) ? null : moduleKey.trim());
        }
        if (description != null) {
            feature.setDescription(description);
        }
        return featureRepository.save(feature);
    }

    public void deleteFeature(UUID tenantId, UUID id) {
        UiPageFeature feature = featureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UiPageFeature", id));
        requireOwned(tenantId, feature.getTenantId());
        featureRepository.delete(feature);
    }

    /** Réglages bruts de l'église, pour l'écran d'administration. */
    @Transactional(readOnly = true)
    public Map<String, Object> adminView(UUID tenantId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> labels = new ArrayList<>();
        for (UiLabelOverride label : labelRepository.findByTenantId(tenantId)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", label.getId());
            // `tenantId` permet à l'admin de distinguer ses propres réglages des
            // réglages globaux livrés par la plateforme.
            body.put("tenantId", label.getTenantId());
            body.put("labelKey", label.getLabelKey());
            body.put("locale", label.getLocale());
            body.put("value", label.getValue());
            body.put("description", label.getDescription());
            body.put("enabled", label.isEnabled());
            body.put("nodeId", label.getNodeId());
            labels.add(body);
        }
        List<Map<String, Object>> features = new ArrayList<>();
        for (UiPageFeature feature : featureRepository.findByTenantId(tenantId)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", feature.getId());
            body.put("tenantId", feature.getTenantId());
            body.put("pageKey", feature.getPageKey());
            body.put("featureKey", feature.getFeatureKey());
            body.put("labelOverride", feature.getLabelOverride());
            body.put("enabled", feature.isEnabled());
            body.put("displayOrder", feature.getDisplayOrder());
            body.put("moduleKey", feature.getModuleKey());
            body.put("description", feature.getDescription());
            body.put("nodeId", feature.getNodeId());
            features.add(body);
        }
        payload.put("labels", labels);
        payload.put("features", features);
        return payload;
    }

    /* ================================================================== */

    /** Un réglage global n'est pas modifiable depuis une église. */
    private static void requireOwned(UUID tenantId, UUID ownerTenantId) {
        if (ownerTenantId == null) {
            throw new BusinessRuleException(
                    "Un réglage global se modifie depuis la console plateforme", "UI_SETTING_GLOBAL_READONLY");
        }
        if (tenantId == null || !tenantId.equals(ownerTenantId)) {
            throw new EntityNotFoundException("UiSetting", ownerTenantId);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Lignes visibles par l'utilisateur, tous réglages confondus. */
    @Transactional(readOnly = true)
    public Collection<UiLabelOverride> allLabels(UUID tenantId) {
        return labelRepository.findByTenantIdIsNullOrTenantId(tenantId);
    }
}