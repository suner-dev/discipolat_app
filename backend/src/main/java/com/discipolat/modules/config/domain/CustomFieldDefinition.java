package com.discipolat.modules.config.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "custom_field_definitions", indexes = {
    @Index(name = "idx_cfd_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cfd_entity", columnList = "tenant_id, entity_type"),
    @Index(name = "idx_cfd_space", columnList = "space_id"),
    @Index(name = "idx_cfd_deleted", columnList = "deleted_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "field_code", nullable = false, length = 60)
    private String fieldCode;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "field_type", nullable = false, length = 30)
    private String fieldType;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> optionsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> validationJson;

    @Column(name = "visibility_scope", nullable = false, length = 30)
    private String visibilityScope = "PUBLIC";

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.optionsJson == null) this.optionsJson = List.of();
        if (this.validationJson == null) this.validationJson = Map.of();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}