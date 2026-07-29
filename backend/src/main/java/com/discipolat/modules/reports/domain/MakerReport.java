package com.discipolat.modules.reports.domain;

import com.discipolat.common.enums.MotifSortie;
import com.discipolat.common.enums.RaisonAbsence;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "maker_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakerReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "faiseur_id", nullable = false)
    private UUID faiseurId;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "semaine", nullable = false)
    private LocalDate semaine;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "presences_par_culte", columnDefinition = "jsonb")
    private Map<String, Boolean> presencesParCulte;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_raison")
    private RaisonAbsence absenceRaison;

    @Column(name = "absence_commentaire")
    private String absenceCommentaire;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "absences_multi", columnDefinition = "jsonb")
    private List<String> absencesMulti;

    @Column(name = "difficultes_categorie")
    private String difficultesCategorie;

    @Column(name = "difficultes")
    private String difficultes;

    @Column(name = "nb_sorties")
    private Integer nbSorties = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_sortie")
    private MotifSortie motifSortie;

    @Column(name = "nb_maintenus")
    private Integer nbMaintenus = 0;

    @Column(name = "nb_invites_culte")
    private Integer nbInvitesCulte = 0;

    @Column(name = "vie_faiseur_challenges")
    private String vieFaiseurChallenges;

    @Column(name = "vie_faiseur_demandes_aide")
    private String vieFaiseurDemandesAide;

    @Column(name = "vie_faiseur_suggestions")
    private String vieFaiseurSuggestions;

    @Column(name = "notes_complementaires")
    private String notesComplementaires;

    @Column(name = "soumis", nullable = false)
    private boolean soumis = false;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_derniere_modification")
    private LocalDateTime dateDerniereModification;

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
        this.dateDerniereModification = LocalDateTime.now();
    }
}
