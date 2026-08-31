package com.discipolat.modules.network.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkEventParticipantRepository extends JpaRepository<NetworkEventParticipant, UUID> {

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    List<NetworkEventParticipant> findByEventIdInAndUserId(List<UUID> eventIds, UUID userId);

    void deleteByEventIdAndUserId(UUID eventId, UUID userId);

    long countByEventId(UUID eventId);

    long countByUserId(UUID userId);
}
