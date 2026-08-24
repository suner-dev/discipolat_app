package com.discipolat.modules.personalObjectives.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonalObjectiveRepository extends JpaRepository<PersonalObjective, UUID> {
    List<PersonalObjective> findByMembreIdOrderByCreatedAtDesc(UUID membreId);
    long countByTenantIdAndMembreId(UUID tenantId, UUID membreId);
    long countByTenantIdAndMembreIdAndStatut(UUID tenantId, UUID membreId, PersonalObjective.Statut statut);
}
