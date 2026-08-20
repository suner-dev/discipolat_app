package com.discipolat.modules.gdpr.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GdprRequestRepository extends JpaRepository<GdprRequest, UUID> {
    List<GdprRequest> findByTenantIdOrderByRequestedAtDesc(UUID tenantId);
    List<GdprRequest> findByRequesterUserIdOrderByRequestedAtDesc(UUID requesterUserId);
    List<GdprRequest> findByTenantIdAndStatusOrderByRequestedAtDesc(UUID tenantId, GdprRequestStatus status);
}
