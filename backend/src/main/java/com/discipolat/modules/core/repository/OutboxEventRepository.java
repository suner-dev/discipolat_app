package com.discipolat.modules.core.repository;

import com.discipolat.modules.core.domain.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndAvailableAtBeforeOrderByAvailableAtAsc(String status, OffsetDateTime availableAt);

    Page<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    @Query("SELECT o FROM OutboxEvent o WHERE o.tenantId = :tenantId ORDER BY o.createdAt DESC")
    Page<OutboxEvent> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId, Pageable pageable);

    long countByStatus(String status);

    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
