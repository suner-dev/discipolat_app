package com.discipolat.modules.kpiNarrative.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KpiNarrativeRepository extends JpaRepository<KpiNarrative, UUID> {
    List<KpiNarrative> findByTenantIdAndTypeKPIOrderByGénéréLeDesc(UUID tenantId, KpiNarrative.TypeKPI type);
    List<KpiNarrative> findByTenantIdAndPériodeOrderByGénéréLeDesc(UUID tenantId, String période);
    List<KpiNarrative> findByTenantIdOrderByGénéréLeDesc(UUID tenantId);
}
