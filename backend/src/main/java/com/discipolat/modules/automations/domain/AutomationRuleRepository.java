package com.discipolat.modules.automations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {
    List<AutomationRule> findByTenantIdAndStatutOrderByCreatedAtDesc(UUID tenantId, AutomationRule.Statut statut);
    Page<AutomationRule> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<AutomationRule> findByTenantIdAndTriggerEvent(UUID tenantId, AutomationRule.TriggerEvent event);
    long countByTenantIdAndStatut(UUID tenantId, AutomationRule.Statut statut);
}
