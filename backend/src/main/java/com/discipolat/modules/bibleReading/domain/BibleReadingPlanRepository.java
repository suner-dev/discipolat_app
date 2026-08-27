package com.discipolat.modules.bibleReading.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BibleReadingPlanRepository extends JpaRepository<BibleReadingPlan, UUID> {
    List<BibleReadingPlan> findByCreateurIdOrderByCreeLeDesc(UUID createurId);
    List<BibleReadingPlan> findByTenantIdOrderByCreeLeDesc(UUID tenantId);
    List<BibleReadingPlan> findByCreateurIdAndStatutOrderByCreeLeDesc(UUID createurId, BibleReadingPlan.Statut statut);
    List<BibleReadingPlan> findByTenantIdAndPartageFamilleTrueOrderByCreeLeDesc(UUID tenantId);
}
