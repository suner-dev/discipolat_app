package com.discipolat.modules.growthProjection.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GrowthProjectionRepository extends JpaRepository<GrowthProjection, UUID> {
    List<GrowthProjection> findByTenantIdOrderByCalculeLeDesc(UUID tenantId);
    List<GrowthProjection> findByTypeProjectionAndCibleId(GrowthProjection.TypeProjection type, UUID cibleId);
}
