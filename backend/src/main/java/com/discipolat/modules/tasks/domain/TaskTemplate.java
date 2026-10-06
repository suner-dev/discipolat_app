package com.discipolat.modules.tasks.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "task_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private Task.TaskType type = Task.TaskType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Task.TaskPriority priority = Task.TaskPriority.MEDIUM;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    /** Voir {@link Task#tags} : sérialisé en TEXT par le convertisseur. */
    @Column(name = "default_tags", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> defaultTags = List.of();

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = Instant.now(); }
}
