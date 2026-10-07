package com.discipolat.modules.streaming.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {
    List<LiveStream> findByTenantIdOrderByScheduledAtDesc(UUID tenantId);
    List<LiveStream> findByTenantIdAndStatus(UUID tenantId, LiveStream.StreamStatus status);

    /** Anti-IDOR : tout accès par id passe par cette requête scopée tenant. */
    Optional<LiveStream> findByIdAndTenantId(Long id, UUID tenantId);
}
