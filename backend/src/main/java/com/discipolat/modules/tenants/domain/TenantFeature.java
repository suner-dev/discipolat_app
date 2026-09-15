package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * TenantFeature — Module activable par tenant (G1.3 - §29)
 * Chaque tenant peut activer/désactiver les modules et configurer leurs paramètres
 */
@Entity
@Table(name = "tenant_features", indexes = {
    @Index(name = "idx_tenant_features_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tenant_features_module", columnList = "module_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

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
