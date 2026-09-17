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
@Table(name = "space_templates", indexes = {
    @Index(name = "idx_space_template_code", columnList = "code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modules_json", columnDefinition = "jsonb", nullable = false)
    private List<String> modulesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_workflows_json", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> defaultWorkflowsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_statuses_json", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> defaultStatusesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_dashboards_json", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> defaultDashboardsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}