package com.discipolat.modules.broadcast.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, UUID> {
    Page<BroadcastMessage> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<BroadcastMessage> findByTenantIdAndStatut(UUID tenantId, BroadcastMessage.Statut statut, Pageable pageable);
}
