package com.discipolat.modules.skillMatching.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SkillMatchRepository extends JpaRepository<SkillMatch, UUID> {
    List<SkillMatch> findByTenantIdOrderByScoreMatchDesc(UUID tenantId);
    List<SkillMatch> findByMembreIdOrderByScoreMatchDesc(UUID membreId);
    List<SkillMatch> findByDepartementIdAndStatut(UUID departementId, SkillMatch.Statut statut);
    long countByTenantIdAndStatut(UUID tenantId, SkillMatch.Statut statut);
}
