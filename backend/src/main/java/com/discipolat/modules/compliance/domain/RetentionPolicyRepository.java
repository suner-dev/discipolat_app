package com.discipolat.modules.compliance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, UUID> {
    List<RetentionPolicy> findByTenantId(UUID tenantId);
    Optional<RetentionPolicy> findByTenantIdAndDataType(UUID tenantId, String dataType);
}
