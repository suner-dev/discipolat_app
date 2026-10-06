package com.discipolat.modules.tasks.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Task> findByTenantIdAndStatus(UUID tenantId, Task.TaskStatus status, Pageable pageable);

    Page<Task> findByTenantIdAndAssignedToId(UUID tenantId, UUID assignedToId, Pageable pageable);

    Page<Task> findByTenantIdAndParentTaskId(UUID tenantId, Long parentTaskId, Pageable pageable);

    List<Task> findByTenantIdAndStatusAndDueDateBefore(UUID tenantId, Task.TaskStatus status, java.time.Instant now);

    List<Task> findByTenantIdAndDueDateBeforeAndStatusNot(UUID tenantId, java.time.Instant now, Task.TaskStatus status);

    Optional<Task> findByTenantIdAndId(UUID tenantId, Long id);

    long countByTenantIdAndStatus(UUID tenantId, Task.TaskStatus status);

    long countByTenantId(UUID tenantId);
}
