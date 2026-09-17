package com.discipolat.modules.workflow.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends TenantAwareRepository<WorkflowInstance, UUID> {

    Optional<WorkflowInstance> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WorkflowInstance> findByTenantIdAndStatus(UUID tenantId, WorkflowInstanceStatus status);

    List<WorkflowInstance> findByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    List<WorkflowInstance> findByTenantIdAndSpaceId(UUID tenantId, UUID spaceId);
}
