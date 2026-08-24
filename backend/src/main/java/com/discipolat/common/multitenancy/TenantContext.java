package com.discipolat.common.multitenancy;

import java.util.UUID;

/**
 * Tenant context holder using ThreadLocal.
 * The tenant ID is set at the beginning of each request (from JWT claim)
 * and cleared at the end.
 *
 * This is the single source of truth for the current tenant ID
 * throughout the request lifecycle.
 */
public final class TenantContext {

    /**
     * Tenant par défaut — créé par la migration V70 et backfillé sur toutes
     * les données existantes. Utilisé comme filet de sécurité pour les
     * écritures hors contexte de requête (jobs planifiés, initialiseurs,
     * tâches système) : sans lui, tout insert échouerait sur la contrainte
     * {@code tenant_id NOT NULL} de V70.
     */
    public static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    /** Alias demandé par les services métier (modules calendar, tickets, …). */
    public static UUID getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set for the current request");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Exécute un traitement dans le contexte d'un tenant donné puis restaure
     * l'état précédent. Utilisé par les webhooks publics et les jobs multi-tenants.
     */
    public static void runAsTenant(UUID tenantId, Runnable action) {
        UUID previous = CURRENT_TENANT.get();
        try {
            CURRENT_TENANT.set(tenantId);
            action.run();
        } finally {
            if (previous != null) {
                CURRENT_TENANT.set(previous);
            } else {
                CURRENT_TENANT.remove();
            }
        }
    }
}

