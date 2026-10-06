package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskTimeEntryRepository extends JpaRepository<TaskTimeEntry, Long> {

    List<TaskTimeEntry> findByTenantIdAndTaskIdOrderByStartTimeAsc(UUID tenantId, Long taskId);

    Optional<TaskTimeEntry> findByTenantIdAndId(UUID tenantId, Long id);

    Integer sumDurationMinutesByTenantIdAndTaskId(UUID tenantId, Long taskId);
}
