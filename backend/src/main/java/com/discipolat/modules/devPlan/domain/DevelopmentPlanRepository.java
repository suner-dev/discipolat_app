package com.discipolat.modules.devPlan.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DevelopmentPlanRepository extends JpaRepository<DevelopmentPlan, UUID> {
    List<DevelopmentPlan> findByMembreIdOrderByCreeLeDesc(UUID membreId);
    List<DevelopmentPlan> findByDepartementIdOrderByCreeLeDesc(UUID departementId);
    List<DevelopmentPlan> findByMembreIdAndStatut(UUID membreId, DevelopmentPlan.Statut statut);
    long countByTenantIdAndStatut(UUID tenantId, DevelopmentPlan.Statut statut);
}
