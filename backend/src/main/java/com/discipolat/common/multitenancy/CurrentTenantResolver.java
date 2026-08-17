package com.discipolat.common.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Provides the current tenant identifier to Hibernate.
 * Used by Hibernate's multi-tenancy support (if configured).
 * Also serves as a fallback when TenantContext is not set.
 */
@Component
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        java.util.UUID tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId.toString() : "DEFAULT";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
