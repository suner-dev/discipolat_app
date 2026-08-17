package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Rapport du responsable sur un membre du département. Complète les
 * rapports hebdomadaires des faiseurs par un bilan rédigé par le
 * responsable : comportement, assiduité, capacité, progression,
 * incident, discipline, recommandation.
 */
@Entity
@Table(name = "department_member_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentMemberReport {

    public enum ReportType {
        COMPORTEMENT, ASSIDUITE, CAPACITE, PROGRESSION, INCIDENT, DISCIPLINE, RECOMMANDATION
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

    /** Auteur du rapport (compte utilisateur du responsable). */
    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReportType type;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

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
