package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {

    Optional<TenantSettings> findByTenantId(UUID tenantId);

    @Query("SELECT ts FROM TenantSettings ts WHERE ts.tenant.id = :tenantId")
    Optional<TenantSettings> findByTenant_Id(UUID tenantId);

    boolean existsByTenantId(UUID tenantId);
}