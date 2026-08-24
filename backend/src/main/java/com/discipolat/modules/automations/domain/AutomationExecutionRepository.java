package com.discipolat.modules.automations.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationExecutionRepository extends JpaRepository<AutomationExecution, UUID> {
    List<AutomationExecution> findByRuleIdOrderByExécutéLeDesc(UUID ruleId);
    long countByRuleIdAndStatut(UUID ruleId, AutomationExecution.Statut statut);
}
