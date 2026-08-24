package com.discipolat.modules.eventChecklist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventChecklistItemRepository extends JpaRepository<EventChecklistItem, UUID> {
    List<EventChecklistItem> findByTenantIdAndEventIdOrderByOrderIndexAsc(UUID tenantId, UUID eventId);
    long countByTenantIdAndEventIdAndStatus(UUID tenantId, UUID eventId, EventChecklistItem.Status status);
}
