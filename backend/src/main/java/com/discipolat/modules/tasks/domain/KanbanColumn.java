package com.discipolat.modules.tasks.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "kanban_columns", indexes = {
        @Index(name = "idx_kanban_cols_tenant", columnList = "tenant_id, \"order\"")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class KanbanColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Task.TaskStatus status;

    @Column(name = "\"order\"", nullable = false)
    @Builder.Default
    private int order = 0;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
