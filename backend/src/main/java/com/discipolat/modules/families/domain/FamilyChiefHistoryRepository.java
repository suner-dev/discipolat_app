package com.discipolat.modules.families.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyChiefHistoryRepository extends JpaRepository<FamilyChiefHistory, UUID> {
    List<FamilyChiefHistory> findByFamilleIdOrderByCreatedAtDesc(UUID familleId);
    List<FamilyChiefHistory> findByNouveauChefIdOrderByCreatedAtDesc(UUID userId);
}
