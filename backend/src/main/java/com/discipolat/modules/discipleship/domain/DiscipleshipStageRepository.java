package com.discipolat.modules.discipleship.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscipleshipStageRepository extends JpaRepository<DiscipleshipStage, Long> {

    List<DiscipleshipStage> findByTenantIdAndJourneyIdOrderByOrderAsc(UUID tenantId, Long journeyId);

    Optional<DiscipleshipStage> findByTenantIdAndId(UUID tenantId, Long id);

    long countByTenantIdAndJourneyId(UUID tenantId, Long journeyId);
}
