package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantFeatureRepository extends JpaRepository<TenantFeature, UUID> {

    List<TenantFeature> findByTenantId(UUID tenantId);

    Optional<TenantFeature> findByTenantIdAndModuleCode(UUID tenantId, String moduleCode);

    @Query("SELECT tf FROM TenantFeature tf WHERE tf.tenant.id = :tenantId AND tf.enabled = true")
    List<TenantFeature> findEnabledByTenantId(UUID tenantId);

    boolean existsByTenantIdAndModuleCode(UUID tenantId, String moduleCode);

    @Query("SELECT COUNT(tf) FROM TenantFeature tf WHERE tf.tenant.id = :tenantId AND tf.enabled = true")
    long countEnabledByTenantId(UUID tenantId);
}
