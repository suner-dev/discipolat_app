package com.discipolat.modules.discipleship.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscipleProgressRequirementRepository extends JpaRepository<DiscipleProgressRequirement, Long> {

    List<DiscipleProgressRequirement> findByTenantIdAndProgressId(UUID tenantId, Long progressId);

    Optional<DiscipleProgressRequirement> findByTenantIdAndProgressIdAndRequirementId(UUID tenantId, Long progressId, Long requirementId);
}
