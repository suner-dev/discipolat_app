package com.discipolat.modules.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfig, UUID> {
    List<WorkflowConfig> findByTenantId(UUID tenantId);
    Optional<WorkflowConfig> findByTenantIdAndWorkflowKey(UUID tenantId, String workflowKey);
    void deleteByTenantIdAndWorkflowKey(UUID tenantId, String workflowKey);
}
