package com.discipolat.modules.core.repository;

import com.discipolat.modules.core.domain.PermissionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionVersionRepository extends JpaRepository<PermissionVersion, UUID> {

    Optional<PermissionVersion> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    List<PermissionVersion> findByTenantId(UUID tenantId);
}