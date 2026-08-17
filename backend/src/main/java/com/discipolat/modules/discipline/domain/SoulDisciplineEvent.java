package com.discipolat.modules.discipline.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "soul_discipline_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SoulDisciplineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false)
    private CategorieDiscipline categorie;

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravite")
    private GraviteDiscipline gravite;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "date_evenement", nullable = false)
    private LocalDate dateEvenement;

    @Column(name = "resolu", nullable = false)
    @Builder.Default
    private boolean resolu = false;

    @Column(name = "date_resolution")
    private LocalDate dateResolution;

    @Column(name = "resolu_par")
    private UUID resoluPar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoulDisciplineEvent that = (SoulDisciplineEvent) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
