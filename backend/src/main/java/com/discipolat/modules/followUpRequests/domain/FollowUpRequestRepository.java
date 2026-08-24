package com.discipolat.modules.followUpRequests.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowUpRequestRepository extends JpaRepository<FollowUpRequest, UUID> {
    List<FollowUpRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<FollowUpRequest> findByTenantIdAndStatusOrderByCreatedAtAsc(UUID tenantId, FollowUpRequest.Status status);
    List<FollowUpRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
    List<FollowUpRequest> findByAssignedToIdOrderByUpdatedAtDesc(UUID assignedToId);
    long countByTenantIdAndStatus(UUID tenantId, FollowUpRequest.Status status);
}
