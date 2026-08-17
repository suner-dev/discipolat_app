package com.discipolat.common.multitenancy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface that adds tenant-aware query methods.
 * All tenant-scoped repositories should extend this instead of plain JpaRepository.
 *
 * The tenant filter (Hibernate @Filter) handles automatic WHERE tenant_id filtering
 * for standard queries. These methods provide additional convenience for
 * explicit tenant filtering when needed.
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Find all entities for the current tenant.
     * The Hibernate tenant filter handles the actual filtering.
     */
    List<T> findAll();

    /**
     * Find entity by ID, ensuring it belongs to the current tenant.
     */
    Optional<T> findById(UUID id);
}
