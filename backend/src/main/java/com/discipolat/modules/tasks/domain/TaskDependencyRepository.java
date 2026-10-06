package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findByTenantIdAndTaskId(UUID tenantId, Long taskId);

    Optional<TaskDependency> findByTenantIdAndId(UUID tenantId, Long id);
}
