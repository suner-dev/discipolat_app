package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findByTenantIdAndTaskId(UUID tenantId, Long taskId);

    void deleteByTenantIdAndId(UUID tenantId, Long id);

    Optional<TaskAttachment> findByTenantIdAndId(UUID tenantId, Long id);
}
