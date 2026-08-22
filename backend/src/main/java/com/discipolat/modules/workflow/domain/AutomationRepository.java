package com.discipolat.modules.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutomationRepository extends JpaRepository<Automation, UUID> {
    List<Automation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<Automation> findByTenantIdAndStatut(UUID tenantId, Automation.Statut statut);
}
