package com.discipolat.modules.families.domain;

import com.discipolat.common.enums.NiveauRisque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyRiskHistoryRepository extends JpaRepository<FamilyRiskHistory, UUID> {
    List<FamilyRiskHistory> findByFamilyIdOrderByCreatedAtDesc(UUID familyId);
}
