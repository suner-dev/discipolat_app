package com.discipolat.common.multitenancy;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import java.util.UUID;

/**
 * Tenant context holder using ThreadLocal.
 * The tenant ID is set at the beginning of each request (from JWT claim)
 * and cleared at the end.
 *
 * This is the single source of truth for the current tenant ID
 * throughout the request lifecycle.
 *
 * <p><b>SECURITY:</b> No DEFAULT_TENANT_ID fallback. Every request MUST have
 * an explicit tenant context. Jobs/webhooks must use {@link #runAsTenant(UUID, Runnable)}.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }
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
            throw new IllegalStateException("No tenant context set for the current request. " +
                    "Ensure JwtAuthenticationFilter runs before this code and user has valid tenant claim.");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Exécute un traitement dans le contexte d'un tenant donné puis restaure
     * l'état précédent. Utilisé par les webhooks publics, jobs planifiés, et tests.
     *
     * @param tenantId the tenant to run as (must not be null)
     * @param action the action to execute
     * @throws IllegalArgumentException if tenantId is null
     */
    public static void runAsTenant(UUID tenantId, Runnable action) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null in runAsTenant");
        }
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

    /**
     * Check if a tenant context is currently set.
     */
    public static boolean hasTenantContext() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Get the current authenticated user ID from SecurityContext.
     * Delegates to SecurityUtils for proper extraction.
     */
    public static UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}

