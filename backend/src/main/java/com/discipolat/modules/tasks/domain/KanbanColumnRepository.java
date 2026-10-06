package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {

    List<KanbanColumn> findByTenantIdAndIsActiveTrueOrderByOrderAsc(UUID tenantId);

    Optional<KanbanColumn> findByTenantIdAndId(UUID tenantId, Long id);
}
