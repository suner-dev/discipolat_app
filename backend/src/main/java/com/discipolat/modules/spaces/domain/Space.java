package com.discipolat.modules.spaces.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * G2.6 — Space : objet unifié « département = famille = sous-équipe ».
 *
 * Exigence non négociable §0.3 : aucun département n'est codé en dur.
 * Un espace est une configuration vivant dans l'unité organisationnelle
 * qui l'accueille ({@code organization_unit_id}) ; il n'y a pas de fusion
 * des deux objets (annexe A §A.1).
 */
@Entity
@Table(name = "spaces", indexes = {
        @Index(name = "idx_space_tenant", columnList = "tenant_id"),
        @Index(name = "idx_space_org_unit", columnList = "organization_unit_id"),
        @Index(name = "idx_space_type", columnList = "space_type"),
        @Index(name = "idx_space_tenant_type", columnList = "tenant_id, space_type"),
        @Index(name = "idx_space_deleted", columnList = "deleted_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_space_tenant_code", columnNames = {"tenant_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "organization_unit_id", nullable = false)
    private UUID organizationUnitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false, length = 30)
    private SpaceType spaceType;

    @Column(name = "template_code", length = 100)
    private String templateCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 60)
    private String code;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SpaceStatus status = SpaceStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "visible_people_scope", length = 20)
    @Builder.Default
    private VisiblePeopleScope visiblePeopleScope = VisiblePeopleScope.CHURCH;

    /** Configuration de l'espace (pages, boutons, widgets, dashboard…). Jamais du code. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configurationJson = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Soft delete (§0.3 n°5) : aucune donnée critique n'est supprimée physiquement. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = SpaceStatus.ACTIVE;
        if (this.configurationJson == null) this.configurationJson = new LinkedHashMap<>();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Space space = (Space) o;
        return id != null && id.equals(space.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
