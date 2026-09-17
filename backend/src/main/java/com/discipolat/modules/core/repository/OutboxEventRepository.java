package com.discipolat.modules.core.repository;

import com.discipolat.modules.core.domain.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndAvailableAtBeforeOrderByAvailableAtAsc(String status, OffsetDateTime availableAt);

    Page<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    @Query("SELECT o FROM OutboxEvent o WHERE o.tenantId = :tenantId ORDER BY o.createdAt DESC")
    Page<OutboxEvent> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") java.util.UUID tenantId, org.springframework.data.domain.Pageable pageable);

    long countByStatus(String status);
}