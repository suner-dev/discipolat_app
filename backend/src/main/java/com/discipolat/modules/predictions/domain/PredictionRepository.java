package com.discipolat.modules.predictions.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {
    List<Prediction> findByTenantIdAndPredictionTypeOrderByCreatedAtDesc(UUID tenantId, Prediction.Type type);
    List<Prediction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
