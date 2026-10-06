package com.discipolat.modules.health.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthKitRepository extends JpaRepository<HealthKit, UUID> {

    List<HealthKit> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<HealthKit> findByTenantIdAndId(UUID tenantId, UUID id);
}
