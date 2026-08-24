package com.discipolat.modules.members.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GeofencePingRepository extends JpaRepository<GeofencePing, UUID> {
    List<GeofencePing> findTop100ByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
    List<GeofencePing> findTop50ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
