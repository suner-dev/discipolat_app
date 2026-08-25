package com.discipolat.modules.admin.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AdminIntegrationConfigRepository extends JpaRepository<AdminIntegrationConfig, UUID> {
    Optional<AdminIntegrationConfig> findByTenantIdAndCategory(UUID tenantId, String category);
}
