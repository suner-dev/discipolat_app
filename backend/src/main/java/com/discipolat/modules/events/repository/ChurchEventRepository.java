package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /** Filtre `status` de GET /church-events (V272) : portée tenant + non supprimé. */
    Page<Event> findByTenantIdAndStatutAndDeletedAtIsNull(UUID tenantId, String statut, Pageable pageable);

    List<Event> findByTenantIdAndStatutAndDeletedAtIsNull(UUID tenantId, String statut);

    /**
     * Lecture pour clôture d'archive (V272) : verrou pessimiste sur la ligne
     * événement. Deux clôtures simultanées d'un même événement sont sérialisées
     * ici, ce qui rend le calcul {@code max(version) + 1} sûr ; l'index unique
     * {@code uq_event_archive_event_version} (V272) demeure le dernier rempart
     * si un appelant contourne ce dépôt. Mêmes critères que
     * {@code findByIdAndTenantIdAndDeletedAtIsNull} — l'isolation tenant n'est
     * jamais affaiblie par le verrou.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.tenantId = :tenantId AND e.deletedAt IS NULL")
    Optional<Event> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

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
