package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §E — snapshot des <b>agrégats</b>
 * recalculés d'un nœud (compteurs REMONTÉS de son sous-arbre).
 *
 * <p>Les compteurs sont <b>recalculés</b> (événement de mutation + job
 * planifié) et <b>non dérivés nominatifs</b> : côté super-admin on
 * n'expose que des NOMBRES, jamais de PII (D7). La série de snapshots
 * constitue la <b>progression</b> (drill-down temporel).
 */
@Entity
@Table(name = "node_aggregate_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_node_agg",
                columnNames = {"node_id", "snapshot_at"}),
        indexes = {
                @Index(name = "idx_node_agg_lookup", columnList = "node_id, snapshot_at"),
                @Index(name = "idx_node_agg_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeAggregateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    /** Fidèles rattachés au sous-arbre. */
    @Column(name = "member_count", nullable = false)
    @Builder.Default
    private Long memberCount = 0L;

    /** Nœuds « église/campus » descendants. */
    @Column(name = "church_count", nullable = false)
    @Builder.Default
    private Long churchCount = 0L;

    /** Porteurs d'un rôle-capacité de direction. */
    @Column(name = "leader_count", nullable = false)
    @Builder.Default
    private Long leaderCount = 0L;

    @Column(name = "sermon_count", nullable = false)
    @Builder.Default
    private Long sermonCount = 0L;

    @Column(name = "prayer_topic_count", nullable = false)
    @Builder.Default
    private Long prayerTopicCount = 0L;

    /** Extension (progression, attendance…) — toujours non-null. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metricsJson = Map.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.snapshotAt == null) this.snapshotAt = Instant.now();
        if (this.metricsJson == null) this.metricsJson = Map.of();
    }
}
