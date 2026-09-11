package com.discipolat.modules.tenants.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * FeatureAccessService — Vérifie l'accès aux fonctionnalités par tenant (Section 29 du prompt maître).
 * 
 * Chaque tenant peut activer/désactiver des modules. Le backend doit refuser
 * l'utilisation d'un module désactivé, pas seulement le cacher visuellement.
 */
@Service
public class FeatureAccessService {

    private final SaasPlanService saasPlanService;
    private final TenantRepository tenantRepository;

    public FeatureAccessService(SaasPlanService saasPlanService, TenantRepository tenantRepository) {
        this.saasPlanService = saasPlanService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Vérifie si le module Discipleship est accessible pour le tenant.
     */
    public boolean hasDiscipleship(UUID tenantId) {
        return isFeatureEnabled(tenantId, "discipleship");
    }

    /**
     * Vérifie si le module Academy est accessible pour le tenant.
     */
    public boolean hasAcademy(UUID tenantId) {
        return isFeatureEnabled(tenantId, "academy");
    }

    /**
     * Vérifie si le module Finance est accessible pour le tenant.
     */
    public boolean hasFinance(UUID tenantId) {
        return isFeatureEnabled(tenantId, "finance");
    }

    /**
     * Vérifie si le module AI est accessible pour le tenant.
     */
    public boolean hasAi(UUID tenantId) {
        return isFeatureEnabled(tenantId, "ai_copilot") && saasPlanService.hasFeature(tenantId, "ai_copilot");
    }

    /**
     * Vérifie si le module Marketplace est accessible pour le tenant.
     */
    public boolean hasMarketplace(UUID tenantId) {
        return isFeatureEnabled(tenantId, "marketplace");
    }

    /**
     * Vérifie si le module API est accessible pour le tenant.
     */
    public boolean hasApiAccess(UUID tenantId) {
        return isFeatureEnabled(tenantId, "api");
    }

    /**
     * Vérifie si le module WhatsApp est accessible pour le tenant.
     */
    public boolean hasWhatsapp(UUID tenantId) {
        return isFeatureEnabled(tenantId, "whatsapp");
    }

    /**
     * Vérifie si le module Community est accessible pour le tenant.
     */
    public boolean hasCommunity(UUID tenantId) {
        return isFeatureEnabled(tenantId, "community");
    }

    /**
     * Vérifie si une fonctionnalité est activée pour le tenant (plan + settings).
     */
    public boolean isFeatureEnabled(UUID tenantId, String featureKey) {
        // Vérifier d'abord le plan SaaS
        if (!saasPlanService.hasFeature(tenantId, featureKey)) {
            return false;
        }

        // Vérifier ensuite les feature flags du tenant
        return tenantRepository.findById(tenantId).map(tenant -> {
            if (tenant.getFeaturesJson() == null) return true;
            try {
                var features = new com.fasterxml.jackson.databind.ObjectMapper().readTree(tenant.getFeaturesJson());
                if (features.has(featureKey)) {
                    return features.get(featureKey).asBoolean();
                }
                return true; // Si pas explicitement désactivé dans les settings
            } catch (Exception e) {
                return true;
            }
        }).orElse(false);
    }

    /**
     * Vérifie si le tenant courant a accès à une fonctionnalité.
     */
    public boolean currentTenantHasFeature(String featureKey) {
        UUID tenantId = TenantContext.requireTenantId();
        return isFeatureEnabled(tenantId, featureKey);
    }

    /**
     * Vérifie l'accès et lève une exception si refusé.
     */
    public void requireFeature(UUID tenantId, String featureKey) {
        if (!isFeatureEnabled(tenantId, featureKey)) {
            throw new FeatureNotAvailableException(
                "Fonctionnalité non disponible pour votre plan: " + featureKey);
        }
    }

    /**
     * Vérifie l'accès pour le tenant courant et lève une exception si refusé.
     */
    public void requireCurrentTenantFeature(String featureKey) {
        UUID tenantId = TenantContext.requireTenantId();
        requireFeature(tenantId, featureKey);
    }

    /**
     * Exception levée lorsqu'une fonctionnalité n'est pas disponible.
     */
    public static class FeatureNotAvailableException extends RuntimeException {
        public FeatureNotAvailableException(String message) {
            super(message);
        }
    }
}
