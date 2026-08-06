package com.discipolat.modules.reports.domain;

import com.discipolat.common.enums.StatutValidation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "family_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "famille_id", nullable = false)
    private UUID familleId;

    @Column(name = "chef_famille_id", nullable = false)
    private UUID chefFamilleId;

    @Column(name = "semaine", nullable = false)
    private LocalDate semaine;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stats_agregees", columnDefinition = "jsonb")
    private Map<String, Object> statsAgregees;

    @Column(name = "presence_moyenne", precision = 5, scale = 2)
    private BigDecimal presenceMoyenne;

    @Column(name = "total_presents")
    private Integer totalPresents = 0;

    @Column(name = "total_absents")
    private Integer totalAbsents = 0;

    @Column(name = "total_sorties")
    private Integer totalSorties = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repartition_sorties", columnDefinition = "jsonb")
    private Map<String, Object> repartitionSorties;

    @Column(name = "total_maintenus")
    private Integer totalMaintenus = 0;

    @Column(name = "nb_suivis_paralleles")
    private Integer nbSuivisParalleles = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suivis_paralleles_details", columnDefinition = "jsonb")
    private Map<String, Object> suivisParallelesDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "faiseurs_sans_rapport", columnDefinition = "jsonb")
    private Map<String, Object> faiseursSansRapport;

    @Column(name = "commentaire_synthese")
    private String commentaireSynthese;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false)
    @Builder.Default
    private StatutValidation statutValidation = StatutValidation.BROUILLON;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_validation_responsable")
    private LocalDateTime dateValidationResponsable;

    @Column(name = "date_validation_pasteur")
    private LocalDateTime dateValidationPasteur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

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
