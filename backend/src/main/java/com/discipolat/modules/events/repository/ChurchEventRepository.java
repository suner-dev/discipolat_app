package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt du modèle vivant Church OS. Depuis l'arbitrage D1 (V203), l'entité
 * unique de la table « event » est {@link Event} (propriétés françaises —
 * dateDebut remplace startAt, statut remplace status) : l'entité doublon
 * ChurchEvent est retirée. Les appelants anglais (ChurchEventService,
 * SpaceExportService) conservent leurs signatures OffsetDateTime, converties
 * à la frontière en la convention « naive = UTC » de V158.
 */
@Repository
public interface ChurchEventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByTenantIdAndDeletedAtIsNullOrderByDateDebutAsc(UUID tenantId);

    Page<Event> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    List<Event> findByTenantIdAndStatutAndDeletedAtIsNull(UUID tenantId, String statut);

    @Query("SELECT e FROM Event e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL "
            + "AND e.dateDebut BETWEEN :from AND :to ORDER BY e.dateDebut ASC")
    List<Event> findCalendarByTenantId(@Param("tenantId") UUID tenantId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    Optional<Event> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // Legacy compatibility - find by organizer
    List<Event> findByOrganisateurIdAndDeletedAtIsNull(UUID organisateurId);

    // Legacy compatibility - find by type
    List<Event> findByTypeEvenementAndDeletedAtIsNull(String typeEvenement);
}
