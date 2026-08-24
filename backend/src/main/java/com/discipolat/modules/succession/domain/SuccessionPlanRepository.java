package com.discipolat.modules.succession.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SuccessionPlanRepository extends JpaRepository<SuccessionPlan, UUID> {
    List<SuccessionPlan> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<SuccessionPlan> findByRôleCible(String rôleCible);
    List<SuccessionPlan> findByStatut(SuccessionPlan.Statut statut);
}
