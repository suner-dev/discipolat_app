package com.discipolat.modules.aiPredictions.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
    List<AiPrediction> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<AiPrediction> findByTenantIdAndPredictionType(Long tenantId, AiPrediction.PredictionType type);
    List<AiPrediction> findByTenantIdAndRiskLevelIn(Long tenantId, List<AiPrediction.RiskLevel> riskLevels);
}
