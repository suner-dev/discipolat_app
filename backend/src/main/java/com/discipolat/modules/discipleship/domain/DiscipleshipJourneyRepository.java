package com.discipolat.modules.discipleship.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscipleshipJourneyRepository extends JpaRepository<DiscipleshipJourney, Long> {

    List<DiscipleshipJourney> findByTenantIdOrderByNameAsc(UUID tenantId);

    List<DiscipleshipJourney> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    List<DiscipleshipJourney> findByTenantIdAndIsActiveFalseOrderByNameAsc(UUID tenantId);

    Page<DiscipleshipJourney> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    Optional<DiscipleshipJourney> findByTenantIdAndId(UUID tenantId, Long id);

    long countByTenantId(UUID tenantId);
}
