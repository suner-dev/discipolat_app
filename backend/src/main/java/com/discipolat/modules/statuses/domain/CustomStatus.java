package com.discipolat.modules.statuses.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * G2.7 — Custom Status : un statut métier est une CONFIGURATION, jamais un enum.
 *
 * Chaque espace peut définir ses statuts (ex. Audiovisuel :
 * Préproduction → Production → Postproduction → Validé → Publié) et les
 * transitions autorisées entre eux.
 */
@Entity
@Table(name = "custom_status_set", indexes = {
        @Index(name = "idx_custom_status_tenant", columnList = "tenant_id"),
        @Index(name = "idx_custom_status_entity", columnList = "tenant_id, entity_type"),
        @Index(name = "idx_custom_status_space", columnList = "space_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CustomStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Domaine métier concerné (ex. ASSET, EVENT, TASK, DEPARTMENT_TASK…). */
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    /** Espace propriétaire ; null = statut défini au niveau du tenant (héritage G1.7). */
    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "initial_status", nullable = false)
    @Builder.Default
    private Boolean initial = false;

    @Column(name = "final_status", nullable = false)
    @Builder.Default
    private Boolean finalStatus = false;

    /** Codes des statuts cibles autorisés depuis celui-ci. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_transitions_json", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> allowedTransitions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.allowedTransitions == null) this.allowedTransitions = new ArrayList<>();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean allowsTransitionTo(String targetCode) {
        return allowedTransitions != null && allowedTransitions.contains(targetCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomStatus that = (CustomStatus) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
