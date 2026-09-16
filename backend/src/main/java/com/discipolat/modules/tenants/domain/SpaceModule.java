package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * G2.2 — SpaceModule : Module activé pour un espace (organization_unit)
 * Relation polymorphe : space_id peut être department, family, campus, ministry, team, cell, group...
 * Remplace l'ancien department_module
 */
@Entity
@Table(name = "space_module", indexes = {
    @Index(name = "idx_space_module_tenant", columnList = "tenant_id"),
    @Index(name = "idx_space_module_space", columnList = "space_id"),
    @Index(name = "idx_space_module_module", columnList = "module_code"),
    @Index(name = "idx_space_module_enabled", columnList = "enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "module_code", nullable = false, length = 50)
    private String moduleCode;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> configurationJson = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limits_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> limitsJson = Map.of();

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}