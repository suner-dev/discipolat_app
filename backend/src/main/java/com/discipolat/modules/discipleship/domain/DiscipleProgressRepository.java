package com.discipolat.modules.discipleship.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscipleProgressRepository extends JpaRepository<DiscipleProgress, Long> {

    Page<DiscipleProgress> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DiscipleProgress> findByTenantIdAndJourneyId(UUID tenantId, Long journeyId, Pageable pageable);

    Page<DiscipleProgress> findByTenantIdAndDiscipleId(UUID tenantId, UUID discipleId, Pageable pageable);

    Page<DiscipleProgress> findByTenantIdAndStatus(UUID tenantId, DiscipleProgress.ProgressStatus status, Pageable pageable);

    Optional<DiscipleProgress> findByTenantIdAndId(UUID tenantId, Long id);

    Optional<DiscipleProgress> findByTenantIdAndDiscipleIdAndJourneyId(UUID tenantId, UUID discipleId, Long journeyId);

    long countByTenantIdAndJourneyId(UUID tenantId, Long journeyId);

    long countByTenantIdAndJourneyIdAndStatus(UUID tenantId, Long journeyId, DiscipleProgress.ProgressStatus status);
}
