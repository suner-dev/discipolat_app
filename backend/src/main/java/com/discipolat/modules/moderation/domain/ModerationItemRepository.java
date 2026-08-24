package com.discipolat.modules.moderation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationItemRepository extends JpaRepository<ModerationItem, UUID> {
    List<ModerationItem> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, ModerationItem.Status status);
    List<ModerationItem> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, ModerationItem.Status status);
    long countByTenantIdAndRiskLevel(UUID tenantId, ModerationItem.RiskLevel level);
}
