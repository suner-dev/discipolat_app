package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findByTenantIdAndTaskIdOrderByCreatedAtAsc(UUID tenantId, Long taskId);

    Optional<TaskComment> findByTenantIdAndId(UUID tenantId, Long id);
}
