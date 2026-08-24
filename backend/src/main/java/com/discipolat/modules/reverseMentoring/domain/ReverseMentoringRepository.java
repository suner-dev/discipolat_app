package com.discipolat.modules.reverseMentoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReverseMentoringRepository extends JpaRepository<ReverseMentoringRequest, UUID> {
    List<ReverseMentoringRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, ReverseMentoringRequest.Status status);
    List<ReverseMentoringRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
