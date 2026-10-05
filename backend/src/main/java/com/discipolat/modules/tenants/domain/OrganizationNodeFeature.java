package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §D — module activé pour un <b>nœud</b>
 * précis (campus/église), INDÉPENDANT par défaut (V3-D).
 *
 * <p>Complète {@link TenantFeature} (échelle tenant) et
 * {@link SpaceModule} (échelle space) en accordant un module à un
 * {@link OrganizationNode}. L'activation sur la racine <b>n'implique
 * pas</b> celle des enfants : la résolution remonte seulement si le
 * nœud est explicite {@code INHERITED}.
 */
@Entity
@Table(name = "organization_node_features",
        uniqueConstraints = @UniqueConstraint(name = "uk_node_feature",
                columnNames = {"node_id", "module_code"}),
        indexes = {
                @Index(name = "idx_node_feature_tenant", columnList = "tenant_id, module_code"),
                @Index(name = "idx_node_feature_node", columnList = "node_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationNodeFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    /** Référence {@code module_definition.code}. */
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
        if (this.configurationJson == null) this.configurationJson = Map.of();
        if (this.limitsJson == null) this.limitsJson = Map.of();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
