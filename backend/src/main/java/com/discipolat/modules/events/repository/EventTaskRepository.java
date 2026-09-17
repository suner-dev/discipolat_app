package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventTaskRepository extends JpaRepository<EventTask, UUID> {

    List<EventTask> findByChurchEventId(UUID churchEventId);

    List<EventTask> findByAssigneeIdAndStatus(UUID assigneeId, String status);

    List<EventTask> findByChurchEventIdAndStatus(UUID churchEventId, String status);

    @Query("SELECT t FROM EventTask t WHERE t.tenantId = :tenantId AND t.churchEventId = :churchEventId AND t.deletedAt IS NULL ORDER BY t.dueAt ASC")
    List<EventTask> findByTenantIdAndChurchEventIdOrderByDueAtAsc(@Param("tenantId") UUID tenantId, @Param("churchEventId") UUID churchEventId);

    Page<EventTask> findByChurchEventIdOrderByCreatedAtDesc(UUID churchEventId, Pageable pageable);
}