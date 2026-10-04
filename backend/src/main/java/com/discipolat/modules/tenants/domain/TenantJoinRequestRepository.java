package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantJoinRequestRepository extends JpaRepository<TenantJoinRequest, UUID> {

    List<TenantJoinRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, TenantJoinRequest.Status status);

    List<TenantJoinRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<TenantJoinRequest> findByTenantIdAndUserIdAndCodeAndStatus(
            UUID tenantId, UUID userId, String code, TenantJoinRequest.Status status);

    Optional<TenantJoinRequest> findByTenantIdAndEmailAndCodeAndStatus(
            UUID tenantId, String email, String code, TenantJoinRequest.Status status);
}
