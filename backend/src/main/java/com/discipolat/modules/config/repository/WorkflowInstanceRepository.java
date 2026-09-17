package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    List<WorkflowInstance> findByTenantIdAndStatus(UUID tenantId, String status);

    List<WorkflowInstance> findByEntityId(UUID entityId);

    Optional<WorkflowInstance> findByEntityIdAndStatusNot(UUID entityId, String status);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.tenantId = :tenantId AND w.startedAt BETWEEN :from AND :to ORDER BY w.startedAt DESC")
    List<WorkflowInstance> findByTenantIdAndStartedAtBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    Page<WorkflowInstance> findByTenantIdOrderByStartedAtDesc(UUID tenantId, Pageable pageable);
}