package com.discipolat.common.multitenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository base class that enforces tenant isolation on
 * {@code EntityManager.find()}-based operations.
 *
 * <p>Hibernate's {@code @Filter} does NOT apply to {@code find()}/{@code load()} by
 * primary key — only to JPQL/criteria queries. As a result, {@code findById} and
 * {@code getReferenceById} would otherwise allow cross-tenant access by primary key
 * (read AND update/delete of another church's rows). This base class re-implements
 * those methods as criteria queries with an explicit {@code tenantId} condition
 * whenever a tenant context is active.
 *
 * <p>Entities without a {@code tenantId} property (global, platform-level data such
 * as the {@code Tenant} itself) transparently fall back to the standard behaviour.
 */
public class TenantAwareSimpleJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private static final ConcurrentHashMap<Class<?>, Boolean> TENANT_SCOPED_CACHE = new ConcurrentHashMap<>();

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public TenantAwareSimpleJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<T> findById(ID id) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null || !isTenantScoped()) {
            return super.findById(id);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(getDomainClass());
        Root<T> root = query.from(getDomainClass());
        query.select(root).where(cb.and(
                cb.equal(root.get(entityInformation.getIdAttribute()), id),
                cb.equal(root.get("tenantId"), tenantId)));
        List<T> results = entityManager.createQuery(query).setMaxResults(1).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public T getReferenceById(ID id) {
        // Charge réellement l'entité (au lieu d'un proxy) afin de vérifier
        // qu'elle appartient au tenant courant — fail-closed pour les références.
        return findById(id).orElseThrow(() -> new EntityNotFoundException(
                getDomainClass().getSimpleName() + " #" + id + " introuvable dans le tenant courant"));
    }

    private boolean isTenantScoped() {
        return TENANT_SCOPED_CACHE.computeIfAbsent(getDomainClass(), this::hasTenantIdProperty);
    }

    private boolean hasTenantIdProperty(Class<?> domainClass) {
        try {
            entityManager.getMetamodel().entity(domainClass).getAttribute("tenantId");
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
