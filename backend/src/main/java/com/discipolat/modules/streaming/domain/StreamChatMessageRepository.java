package com.discipolat.modules.streaming.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StreamChatMessageRepository extends JpaRepository<StreamChatMessage, UUID> {
    List<StreamChatMessage> findByStreamIdAndTenantIdOrderByCreatedAtAsc(Long streamId, UUID tenantId);
    long countByStreamIdAndTenantId(Long streamId, UUID tenantId);
}
