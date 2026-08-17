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

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
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
}
