package com.discipolat.modules.leaveRequests.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    Page<LeaveRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<LeaveRequest> findByTenantIdAndStatut(UUID tenantId, LeaveRequest.Statut statut, Pageable pageable);
}
