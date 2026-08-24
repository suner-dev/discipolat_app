package com.discipolat.modules.skillsMatrix.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillEvaluationRepository extends JpaRepository<SkillEvaluation, UUID> {
    List<SkillEvaluation> findByMembreId(UUID membreId);
    List<SkillEvaluation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<SkillEvaluation> findByTenantIdAndCompétence(UUID tenantId, String compétence);
}
