package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * §G3.4 / §G6.4 — archive intégrale d'un événement, versionnée au clôturage :
 * snapshot JSON (dress codes, équipes, tâches, programme, matériel, présences)
 * consultable par années / mois / espace (« 4 mars 2023 — Séminaire… »).
 */
@Entity
@Table(name = "event_archive", uniqueConstraints = {
    @UniqueConstraint(name = "uq_event_archive_event_version", columnNames = {"event_id", "version"})
}, indexes = {
    @Index(name = "idx_event_archive_tenant", columnList = "tenant_id"),
    @Index(name = "idx_event_archive_event", columnList = "event_id"),
    @Index(name = "idx_event_archive_year_month", columnList = "tenant_id, event_year, event_month"),
    @Index(name = "idx_event_archive_attendance", columnList = "tenant_id, attendance_count")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurchEventArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_title", nullable = false, length = 255)
    private String eventTitle;

    /** Date de début de l'événement archivé (derived keys for browsing). */
    @Column(name = "event_start_at", nullable = false)
    private OffsetDateTime eventStartAt;

    @Column(name = "event_year", nullable = false)
    private Integer eventYear;

    @Column(name = "event_month", nullable = false)
    private Integer eventMonth;

    /** Espace (unité) principal de l'événement, si rattaché — filtre « par espace ». */
    @Column(name = "space_id")
    private UUID spaceId;

    /** Version d'archive : incrémentée à chaque re-clôture (jamais d'écrasement). */
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /** Snapshot complet et immuable de l'événement au moment de la clôture. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> snapshotJson = new LinkedHashMap<>();

    /**
     * Nombre de présences figé au clôturage, dénormalisé depuis
     * {@code snapshot_json.attendanceCount} (migration V272 §3).
     *
     * <p>Pourquoi une colonne : la liste paginée doit afficher le compteur sans
     * déserialiser le JSONB de chaque ligne — l'audit pertinence (oct. 2026)
     * relevait que la page « Archives » chargeait l'intégralité des instantanés
     * juste pour rendre une carte. La colonne est la valeur lue par la liste ;
     * le snapshot reste la vérité d'instantané et n'est lu que pour le détail.
     *
     * <p>Backfillée pour les lignes préexistantes par V272 ; renseignée à
     * l'écriture par {@code archiveEvent}.
     */
    @Column(name = "attendance_count", nullable = false)
    @Builder.Default
    private Integer attendanceCount = 0;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "archived_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime archivedAt = OffsetDateTime.now();
}
