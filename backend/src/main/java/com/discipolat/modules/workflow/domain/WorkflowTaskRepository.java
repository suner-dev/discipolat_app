package com.discipolat.modules.workflow.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTaskRepository extends TenantAwareRepository<WorkflowTask, UUID> {

    Optional<WorkflowTask> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WorkflowTask> findByInstanceIdOrderByCreatedAtAsc(UUID instanceId);

    List<WorkflowTask> findByTenantIdAndAssigneeIdAndStatus(UUID tenantId, UUID assigneeId, WorkflowTaskStatus status);

    List<WorkflowTask> findByTenantIdAndStatusAndDueAtBefore(UUID tenantId, WorkflowTaskStatus status, Instant threshold);
}
