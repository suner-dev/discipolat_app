package com.discipolat.modules.workflow.domain;

import com.discipolat.common.multitenancy.TenantAwareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends TenantAwareRepository<WorkflowDefinition, UUID> {

    Optional<WorkflowDefinition> findByIdAndTenantId(UUID id, UUID tenantId);

    List<WorkflowDefinition> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    /** Workflow propre à un espace, sinon repli sur celui du tenant. */
    Optional<WorkflowDefinition> findFirstByTenantIdAndSpaceIdAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
            UUID tenantId, UUID spaceId, String code);

    Optional<WorkflowDefinition> findFirstByTenantIdAndSpaceIdIsNullAndCodeAndEnabledTrueAndDeletedAtIsNullOrderByVersionDesc(
            UUID tenantId, String code);
}
