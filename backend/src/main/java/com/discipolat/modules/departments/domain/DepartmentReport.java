package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Rapport de département sauvegardé (synthèse hebdomadaire, mensuelle,
 * trimestrielle, annuelle, d'événement, d'activité, d'effectif,
 * d'assiduité, de performance…). La synthèse est générée à partir des
 * données réelles du département puis stockée pour consultation, envoi
 * au pasteur ou export.
 */
@Entity
@Table(name = "department_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentReport {

    public enum ReportType {
        HEBDOMADAIRE, MENSUEL, TRIMESTRIEL, ANNUEL, EVENEMENT,
        INCIDENT, DISCIPLINE, ACTIVITE, EFFECTIF, ASSIDUITE, PERFORMANCE, SYNTHESE
    }

    public enum ReportStatus {
        BROUILLON, SOUMIS, ARCHIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    /** Auteur du rapport (compte utilisateur du responsable). */
    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ReportType type;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "periode_debut")
    private LocalDate periodeDebut;

    @Column(name = "periode_fin")
    private LocalDate periodeFin;

    /** Synthèse au format texte (paragraphes) générée sur données réelles. */
    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private ReportStatus statut = ReportStatus.BROUILLON;

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
