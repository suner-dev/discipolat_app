package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Élément d'une checklist de département. Un item peut être cochée
 * (« fait ») et réordonnée.
 */
@Entity
@Table(name = "department_checklist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "checklist_id", nullable = false)
    private UUID checklistId;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "fait", nullable = false)
    private boolean fait = false;

    @Column(name = "ordre", nullable = false)
    private int ordre = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
