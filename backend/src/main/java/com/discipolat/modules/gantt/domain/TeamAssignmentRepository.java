package com.discipolat.modules.gantt.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeamAssignmentRepository extends JpaRepository<TeamAssignment, UUID> {
    List<TeamAssignment> findByTenantIdAndDébutBetweenOrderByDébutAsc(UUID tenantId, LocalDateTime start, LocalDateTime end);
    List<TeamAssignment> findByÉquipeId(UUID équipeId);
    List<TeamAssignment> findByMembreId(UUID membreId);
    long countByÉquipeIdAndStatut(UUID équipeId, TeamAssignment.Statut statut);
}
