package com.discipolat.modules.intelligence.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntelligenceKpiRepository extends JpaRepository<IntelligenceKpi, UUID> {
    List<IntelligenceKpi> findByTenantIdOrderByCategoryAscDisplayOrderAsc(UUID tenantId);
    List<IntelligenceKpi> findByTenantIdAndCategoryOrderByDisplayOrderAsc(UUID tenantId, IntelligenceKpi.Category category);
    List<IntelligenceKpi> findByTenantIdAndIsAlertTrueOrderByDisplayOrderAsc(UUID tenantId);
}
