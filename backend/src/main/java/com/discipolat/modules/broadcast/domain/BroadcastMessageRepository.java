package com.discipolat.modules.broadcast.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, UUID> {
    Page<BroadcastMessage> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<BroadcastMessage> findByTenantIdAndStatut(UUID tenantId, BroadcastMessage.Statut statut, Pageable pageable);

    /** Lecture scoped : jamais de findById(id) nu sur une donnée multi-tenant. */
    Optional<BroadcastMessage> findByIdAndTenantId(UUID id, UUID tenantId);

    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStatut(UUID tenantId, BroadcastMessage.Statut statut);
}
