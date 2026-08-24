package com.discipolat.modules.visits.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PastoralVisitRepository extends JpaRepository<PastoralVisit, UUID> {
    List<PastoralVisit> findByTenantIdAndPrévuLeBetweenOrderByPrévuLeAsc(UUID tenantId, LocalDateTime start, LocalDateTime end);
    List<PastoralVisit> findByVisiteurIdAndStatut(UUID visiteurId, PastoralVisit.Statut statut);
    List<PastoralVisit> findByMembreIdOrderByPrévuLeDesc(UUID membreId);
}
