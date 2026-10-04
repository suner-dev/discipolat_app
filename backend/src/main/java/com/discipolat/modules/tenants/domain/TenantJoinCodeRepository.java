package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantJoinCodeRepository extends JpaRepository<TenantJoinCode, UUID> {

    Optional<TenantJoinCode> findByCodeAndIsActiveTrue(String code);

    boolean existsByCodeAndIsActiveTrue(String code);

    List<TenantJoinCode> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<TenantJoinCode> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    Optional<TenantJoinCode> findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrue(UUID tenantId);

    Optional<TenantJoinCode> findByTenantIdAndOrgNodeIdAndIsActiveTrue(UUID tenantId, UUID orgNodeId);
}
