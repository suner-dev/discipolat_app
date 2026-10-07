package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventTeamRepository extends JpaRepository<EventTeam, UUID> {

    List<EventTeam> findByChurchEventId(UUID churchEventId);

    List<EventTeam> findBySpaceId(UUID spaceId);

    /** Lookup scopé tenant (anti-IDOR) : une équipe n'est accessible que dans son tenant ET son événement. */
    Optional<EventTeam> findByTenantIdAndChurchEventIdAndId(UUID tenantId, UUID churchEventId, UUID id);
}