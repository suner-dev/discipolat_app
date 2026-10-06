package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskTemplateSubtaskRepository extends JpaRepository<TaskTemplateSubtask, Long> {

    List<TaskTemplateSubtask> findByTenantIdAndTemplateIdOrderByOrderAsc(UUID tenantId, Long templateId);
}
