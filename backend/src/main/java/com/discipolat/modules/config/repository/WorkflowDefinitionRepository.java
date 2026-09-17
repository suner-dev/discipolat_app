package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    List<WorkflowDefinition> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);

    List<WorkflowDefinition> findByTenantIdAndSpaceIdAndDeletedAtIsNull(UUID tenantId, UUID spaceId);

    Optional<WorkflowDefinition> findByTenantIdAndSpaceIdAndCodeAndVersionAndDeletedAtIsNull(UUID tenantId, UUID spaceId, String code, Integer version);

    @Query("SELECT w FROM WorkflowDefinition w WHERE w.tenantId = :tenantId AND (w.spaceId = :spaceId OR w.spaceId IS NULL) AND w.deletedAt IS NULL ORDER BY w.name")
    List<WorkflowDefinition> findResolvedDefinitions(@Param("tenantId") UUID tenantId, @Param("spaceId") UUID spaceId);
}