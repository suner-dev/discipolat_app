package com.discipolat.modules.health.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthMedicationRepository extends JpaRepository<HealthMedication, UUID> {

    List<HealthMedication> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<HealthMedication> findByTenantIdAndId(UUID tenantId, UUID id);
}
