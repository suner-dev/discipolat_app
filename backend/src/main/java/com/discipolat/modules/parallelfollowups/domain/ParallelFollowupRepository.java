package com.discipolat.modules.parallelfollowups.domain;

import com.discipolat.common.enums.StatutSuiviParallele;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParallelFollowupRepository extends JpaRepository<ParallelFollowup, UUID> {
    Page<ParallelFollowup> findByInitiateurId(UUID initiateurId, Pageable pageable);
    Page<ParallelFollowup> findByStatut(StatutSuiviParallele statut, Pageable pageable);
    Page<ParallelFollowup> findByInitiateurIdAndStatut(UUID initiateurId, StatutSuiviParallele statut, Pageable pageable);
    List<ParallelFollowup> findByAmeId(UUID ameId);
    long countByFamilleIdAndStatut(UUID familleId, StatutSuiviParallele statut);
    long countByStatut(StatutSuiviParallele statut);
}
