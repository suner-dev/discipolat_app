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
@Table(name = "event_archive", indexes = {
    @Index(name = "idx_event_archive_tenant", columnList = "tenant_id"),
    @Index(name = "idx_event_archive_event", columnList = "event_id"),
    @Index(name = "idx_event_archive_year_month", columnList = "tenant_id, event_year, event_month")
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

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "archived_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime archivedAt = OffsetDateTime.now();
}
