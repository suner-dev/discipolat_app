package com.discipolat.modules.compliance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GdprRepository extends JpaRepository<GdprRequest, UUID> {
    Page<GdprRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    long countByTenantIdAndStatut(UUID tenantId, GdprRequest.Statut statut);
}
