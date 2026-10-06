package com.discipolat.modules.tasks.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    Page<TaskTemplate> findByTenantIdAndIsActiveTrue(UUID tenantId, Pageable pageable);

    List<TaskTemplate> findByTenantIdAndIsActiveTrue(UUID tenantId);

    Optional<TaskTemplate> findByTenantIdAndId(UUID tenantId, Long id);
}
