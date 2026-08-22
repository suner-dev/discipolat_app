package com.discipolat.modules.skillsMatrix.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillEvaluationRepository extends JpaRepository<SkillEvaluation, UUID> {
    List<SkillEvaluation> findByTenantId(UUID tenantId);
    List<SkillEvaluation> findByTenantIdAndMembreId(UUID tenantId, UUID membreId);
    List<SkillEvaluation> findByTenantIdAndCompetence(UUID tenantId, String competence);
}
