package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.api.ChurchEventArchiveSummary;
import com.discipolat.modules.events.domain.ChurchEventArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChurchEventArchiveRepository extends JpaRepository<ChurchEventArchive, UUID> {

    List<ChurchEventArchive> findByTenantIdOrderByEventStartAtDesc(UUID tenantId);

    List<ChurchEventArchive> findByTenantIdAndEventYearOrderByEventStartAtDesc(UUID tenantId, Integer eventYear);

    List<ChurchEventArchive> findByTenantIdAndEventYearAndEventMonthOrderByEventStartAtDesc(
            UUID tenantId, Integer eventYear, Integer eventMonth);

    List<ChurchEventArchive> findByEventIdOrderByVersionDesc(UUID eventId);

    /**
     * Versions d'UN événement, portée au tenant (V272) — la consultation des
     * versions depuis l'écran reste isolée comme la liste générale ; la
     * variante sans tenant est réservée au calcul de version interne au service
     * de clôture, où l'événement a déjà été vérifié porté au tenant.
     */
    List<ChurchEventArchive> findByTenantIdAndEventIdOrderByVersionDesc(UUID tenantId, UUID eventId);

    Optional<ChurchEventArchive> findFirstByEventIdOrderByVersionDesc(UUID eventId);

    long countByTenantId(UUID tenantId);

    List<ChurchEventArchive> findByTenantId(UUID tenantId);

    /**
     * Recherche paginée de l'écran « Archives » (V272) : projection SANS
     * {@code snapshot_json}. Le résumé ne charge que les colonnes scalaires —
     * c'est la garantie bornée qu'exigeait l'audit : ni mémoire ni temps de
     * rendu ne croissent avec la taille des instantanés.
     *
     * <p>Filtres : année, mois, espace principal (colonne {@code space_id}
     * indexée ; le repli sur les espaces secondaires du snapshot reste porté
     * par l'endpoint legacy non paginé, qui lit le snapshot), événement.
     * Les {@code CAST(:x AS …) IS NULL} suivent la convention du dépôt
     * (PrayerRepository) pour les critères optionnels.
     *
     * <p>Tri stable {@code event_start_at DESC, version DESC} : les versions
     * d'un même événement restent adjacentes.
     */
    @Query(value = """
            SELECT new com.discipolat.modules.events.api.ChurchEventArchiveSummary(
                a.id, a.eventId, a.eventTitle, a.eventStartAt, a.eventYear, a.eventMonth,
                a.spaceId, a.version, a.attendanceCount, a.archivedAt, a.archivedBy)
            FROM ChurchEventArchive a
            WHERE a.tenantId = :tenantId
              AND (CAST(:year AS integer) IS NULL OR a.eventYear = :year)
              AND (CAST(:month AS integer) IS NULL OR a.eventMonth = :month)
              AND (CAST(:spaceId AS uuid) IS NULL OR a.spaceId = :spaceId)
              AND (CAST(:eventId AS uuid) IS NULL OR a.eventId = :eventId)
            ORDER BY a.eventStartAt DESC, a.version DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM ChurchEventArchive a
            WHERE a.tenantId = :tenantId
              AND (CAST(:year AS integer) IS NULL OR a.eventYear = :year)
              AND (CAST(:month AS integer) IS NULL OR a.eventMonth = :month)
              AND (CAST(:spaceId AS uuid) IS NULL OR a.spaceId = :spaceId)
              AND (CAST(:eventId AS uuid) IS NULL OR a.eventId = :eventId)
            """)
    Page<ChurchEventArchiveSummary> searchSummaries(
            @Param("tenantId") UUID tenantId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("spaceId") UUID spaceId,
            @Param("eventId") UUID eventId,
            Pageable pageable);

    /**
     * Facettes (années / mois réellement archivés) — alimente les filtres de
     * l'écran sans listage en dur ni téléchargement du jeu complet (V272).
     * Agrégat sur colonnes scalaires : aucune désérialisation de snapshot.
     */
    @Query("""
            SELECT a.eventYear, a.eventMonth, COUNT(a)
            FROM ChurchEventArchive a
            WHERE a.tenantId = :tenantId
            GROUP BY a.eventYear, a.eventMonth
            ORDER BY a.eventYear DESC, a.eventMonth ASC
            """)
    List<Object[]> findYearMonthFacets(@Param("tenantId") UUID tenantId);
}
