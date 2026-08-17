package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Objectif de progression fixé à un membre du département par le
 * responsable (suivi individuel de la progression spirituelle,
 * disciplinaire ou opérationnelle).
 */
@Entity
@Table(name = "department_member_objectives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentMemberObjective {

    public enum ObjectiveStatus {
        A_FAIRE, EN_COURS, ATTEINT, ANNULE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    /** Membre concerné (soul_id). */
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "echeance")
    private LocalDate echeance;

    /** Avancement 0..100. */
    @Column(name = "avancement", nullable = false)
    @Builder.Default
    private int avancement = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private ObjectiveStatus statut = ObjectiveStatus.A_FAIRE;

    @Column(name = "cree_par")
    private UUID creePar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
