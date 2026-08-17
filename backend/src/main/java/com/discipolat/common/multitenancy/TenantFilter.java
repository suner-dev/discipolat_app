package com.discipolat.common.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hibernate filter that automatically enforces tenant isolation on all queries.
 *
 * When enabled, every query that touches a tenant-scoped entity will include
 * a WHERE tenant_id = :tenantId clause, preventing cross-tenant data access
 * even if a developer forgets to add explicit filtering.
 *
 * The filter is enabled per-request by TenantFilterInterceptor.
 */
@Component
public class TenantFilter {

    private static final String FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAM = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Enable the tenant filter on the current Hibernate session.
     * Must be called at the beginning of each request.
     */
    public void enableFilter() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter(FILTER_NAME);
        filter.setParameter(TENANT_ID_PARAM, tenantId);
    }

    /**
     * Disable the filter (called at end of request or for system queries).
     */
    public void disableFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter(FILTER_NAME);
    }
}
