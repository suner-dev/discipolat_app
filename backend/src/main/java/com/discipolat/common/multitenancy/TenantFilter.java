package com.discipolat.common.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Hibernate filter that automatically enforces tenant isolation on all queries.
 *
 * When enabled, every query that touches a tenant-scoped entity will include
 * a WHERE tenant_id = :tenantId clause, preventing cross-tenant data access
 * even if a developer forgets to add explicit filtering.
 *
 * <p>The filter is enabled per-request by {@link TenantFilterInterceptor}.
 *
 * <p><b>Why this resolves the EntityManager directly instead of using
 * {@code @PersistenceContext}:</b> MVC interceptors registered by user
 * {@code WebMvcConfigurer}s run BEFORE Spring Boot's OpenEntityManagerInView
 * interceptor (auto-configuration registers it later, and Spring does not sort
 * the interceptor chain). At that point no EntityManager is bound to the thread
 * yet, so a {@code @PersistenceContext} proxy would silently enable the filter
 * on a throwaway EntityManager — and repositories would keep querying the
 * un-filtered OpenEntityManagerInView session. To be immune to interceptor
 * ordering, we resolve the thread-bound EntityManager ourselves and, if none
 * exists yet, create and bind one (exactly like OpenEntityManagerInView would),
 * guaranteeing the filter is active on the exact session the repositories use.
 */
@Component
public class TenantFilter {

    private static final String FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAM = "tenantId";
    private static final String BOUND_EM_ATTRIBUTE = TenantFilter.class.getName() + ".BOUND_EM";

    private final EntityManagerFactory entityManagerFactory;

    public TenantFilter(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Enables the tenant filter on the request-scoped EntityManager (the same
     * one the repositories use). If no EntityManager is bound to the current
     * thread yet, one is created and bound so the filter is guaranteed to be
     * active on the session used by the repositories.
     *
     * @return true if this call created and bound the EntityManager (the caller
     *         must release it in {@link #afterCompletion}).
     */
    public boolean enableFilter(HttpServletRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return false;
        }
        EntityManagerHolder holder = getBoundHolder();
        boolean created = false;
        if (holder == null) {
            EntityManager em = entityManagerFactory.createEntityManager();
            holder = new EntityManagerHolder(em);
            TransactionSynchronizationManager.bindResource(entityManagerFactory, holder);
            request.setAttribute(BOUND_EM_ATTRIBUTE, Boolean.TRUE);
            created = true;
        }
        Session session = holder.getEntityManager().unwrap(Session.class);
        Filter filter = session.enableFilter(FILTER_NAME);
        filter.setParameter(TENANT_ID_PARAM, tenantId);
        return created;
    }

    /**
     * Releases the EntityManager when this interceptor created it, and disables
     * the filter as a safety net. When the EntityManager was bound by
     * OpenEntityManagerInView instead, it is left untouched (the OpenEntityManagerInView
     * interceptor is responsible for closing it).
     */
    public void afterCompletion(HttpServletRequest request) {
        if (request.getAttribute(BOUND_EM_ATTRIBUTE) == null) {
            return;
        }
        request.removeAttribute(BOUND_EM_ATTRIBUTE);
        EntityManagerHolder holder = getBoundHolder();
        if (holder == null) {
            return;
        }
        EntityManager em = holder.getEntityManager();
        try {
            em.unwrap(Session.class).disableFilter(FILTER_NAME);
        } catch (Exception ignored) {
            // Session may already be closed; the filter dies with it.
        }
        if (TransactionSynchronizationManager.hasResource(entityManagerFactory)) {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        }
        EntityManagerFactoryUtils.closeEntityManager(em);
    }

    private EntityManagerHolder getBoundHolder() {
        Object resource = TransactionSynchronizationManager.getResource(entityManagerFactory);
        return resource instanceof EntityManagerHolder holder ? holder : null;
    }
}
