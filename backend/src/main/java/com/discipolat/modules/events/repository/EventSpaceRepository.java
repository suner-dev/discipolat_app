package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSpaceRepository extends JpaRepository<EventSpace, UUID> {

    List<EventSpace> findByChurchEventId(UUID churchEventId);

    List<EventSpace> findBySpaceId(UUID spaceId);

    Optional<EventSpace> findByChurchEventIdAndSpaceId(UUID churchEventId, UUID spaceId);

    void deleteByChurchEventId(UUID churchEventId);
}