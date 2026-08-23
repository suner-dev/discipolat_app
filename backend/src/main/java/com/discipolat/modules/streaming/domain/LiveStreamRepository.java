package com.discipolat.modules.streaming.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {
    List<LiveStream> findByTenantIdOrderByScheduledAtDesc(Long tenantId);
    List<LiveStream> findByTenantIdAndStatus(Long tenantId, LiveStream.StreamStatus status);
}
