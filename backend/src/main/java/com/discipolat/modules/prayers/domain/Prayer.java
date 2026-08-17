package com.discipolat.modules.prayers.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "prayers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Prayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "ame_id")
    private UUID ameId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "categorie", nullable = false)
    private String categorie;

    @Column(name = "priorite", nullable = false)
    private String priorite = "MOYENNE";

    @Column(name = "statut", nullable = false)
    private String statut = "EN_COURS";

    @Column(name = "temoignage")
    private String temoignage;

    @Column(name = "date_exaucee")
    private LocalDateTime dateExaucee;

    @Column(name = "visibilite", nullable = false)
    private String visibilite = "PARTAGEE";

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prayer prayer = (Prayer) o;
        return id != null && id.equals(prayer.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
