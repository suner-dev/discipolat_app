package com.discipolat.modules.health.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthDutyRepository extends JpaRepository<HealthDuty, UUID> {

    List<HealthDuty> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<HealthDuty> findByTenantIdAndId(UUID tenantId, UUID id);
}
