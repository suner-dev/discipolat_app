package com.discipolat.modules.aiPredictions.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
    List<AiPrediction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<AiPrediction> findByTenantIdAndPredictionType(UUID tenantId, AiPrediction.PredictionType type);
    List<AiPrediction> findByTenantIdAndRiskLevelIn(UUID tenantId, List<AiPrediction.RiskLevel> riskLevels);
}
