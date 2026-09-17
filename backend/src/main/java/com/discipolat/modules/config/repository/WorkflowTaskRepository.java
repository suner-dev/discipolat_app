package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.WorkflowTask;
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
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, UUID> {

    List<WorkflowTask> findByInstanceId(UUID instanceId);

    List<WorkflowTask> findByAssigneeIdAndStatus(UUID assigneeId, String status);

    List<WorkflowTask> findByStatusAndDueAtBefore(String status, OffsetDateTime dueAt);

    Optional<WorkflowTask> findByInstanceIdAndStepId(UUID instanceId, UUID stepId);

    @Query("SELECT w FROM WorkflowTask w WHERE w.tenantId = :tenantId AND w.dueAt BETWEEN :from AND :to ORDER BY w.dueAt ASC")
    List<WorkflowTask> findByTenantIdAndDueAtBetween(@Param("tenantId") UUID tenantId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    Page<WorkflowTask> findByAssigneeIdOrderByDueAtAsc(UUID assigneeId, Pageable pageable);

    long countByAssigneeIdAndStatus(UUID assigneeId, String status);
}