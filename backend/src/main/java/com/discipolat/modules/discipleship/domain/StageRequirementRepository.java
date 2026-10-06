package com.discipolat.modules.discipleship.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StageRequirementRepository extends JpaRepository<StageRequirement, Long> {

    List<StageRequirement> findByTenantIdAndStageIdOrderByOrderAsc(UUID tenantId, Long stageId);
}
