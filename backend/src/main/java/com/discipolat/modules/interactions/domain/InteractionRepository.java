package com.discipolat.modules.interactions.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    List<Interaction> findBySoulIdOrderByDateInteractionDesc(UUID soulId);
    Page<Interaction> findBySoulId(UUID soulId, Pageable pageable);
    long countBySoulId(UUID soulId);
    long countBySoulIdAndType(UUID soulId, InteractionType type);
    long countByTypeAndDateInteractionBetween(InteractionType type, LocalDateTime from, LocalDateTime to);
    long countByTypeAndSoulIdInAndDateInteractionBetween(InteractionType type, List<UUID> soulIds, LocalDateTime from, LocalDateTime to);
    long countByAuteurId(UUID auteurId);

    /** Actions assignées à un utilisateur : à faire (rappel passé ou sans rappel) puis à venir. */
    @Query("""
            SELECT i FROM Interaction i
            WHERE i.aFairePar = :userId AND (i.rappelLe IS NULL OR i.rappelLe >= CURRENT_TIMESTAMP)
            ORDER BY i.rappelLe ASC NULLS LAST
            """)
    List<Interaction> findReminders(@Param("userId") UUID userId);
}
