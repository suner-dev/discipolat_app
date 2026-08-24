package com.discipolat.modules.mentoring.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MentorSuggestionRepository extends JpaRepository<MentorSuggestion, UUID> {
    Page<MentorSuggestion> findByChefDeFamilleIdAndStatutOrderByPrioritéAscCreatedAtDesc(
            UUID chefDeFamilleId, MentorSuggestion.Statut statut, Pageable pageable);
    List<MentorSuggestion> findByChefDeFamilleIdOrderByPrioritéAscCreatedAtDesc(UUID chefDeFamilleId);
    long countByChefDeFamilleIdAndStatut(UUID chefDeFamilleId, MentorSuggestion.Statut statut);
    List<MentorSuggestion> findByFaiseurIdOrderByCreatedAtDesc(UUID faiseurId);
}
