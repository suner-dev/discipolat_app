package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantDisputeRepository extends JpaRepository<TenantDispute, UUID> {

    List<TenantDispute> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<TenantDispute> findByStatusOrderByCreatedAtDesc(TenantDispute.DisputeStatus status);
}
