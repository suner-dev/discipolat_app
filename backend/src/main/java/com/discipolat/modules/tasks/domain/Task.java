package com.discipolat.modules.tasks.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tasks_status", columnList = "tenant_id, status"),
        @Index(name = "idx_tasks_assigned", columnList = "tenant_id, assigned_to_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Task {

    public enum TaskType { TASK, SUBTASK, EPIC, STORY, BUG, FEATURE, CHORES, MEETING, CALL, REVIEW }
    public enum TaskPriority { LOW, MEDIUM, HIGH, URGENT, CRITICAL }
    public enum TaskStatus { BACKLOG, TODO, IN_PROGRESS, IN_REVIEW, BLOCKED, DONE, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private TaskType type = TaskType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "assigned_by_id")
    private UUID assignedById;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "completed_date")
    private Instant completedDate;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "actual_hours")
    private Integer actualHours;

    @Column(name = "tags", columnDefinition = "TEXT[]")
    @Convert(converter = StringListConverter.class)
    @Builder.Default
    private List<String> tags = List.of();

    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Column(name = "recurrence_rule_id")
    private Long recurrenceRuleId;

    @Column(name = "recurrence_pattern", length = 50)
    private String recurrencePattern;

    @Column(name = "recurrence_end_date")
    private Instant recurrenceEndDate;

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
