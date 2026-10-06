package com.discipolat.modules.discipleship.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StageRewardRepository extends JpaRepository<StageReward, Long> {

    List<StageReward> findByTenantIdAndStageId(UUID tenantId, Long stageId);
}
