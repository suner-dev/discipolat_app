package com.discipolat.modules.souls.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "souls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Soul {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "profession")
    private String profession;

    @Column(name = "niveau_etude")
    private String niveauEtude;

    @Column(name = "nb_enfants")
    private Integer nbEnfants;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_disciple", nullable = false)
    private TypeDisciple typeDisciple;

    @Column(name = "date_integration", nullable = false)
    private LocalDate dateIntegration;

    @Column(name = "date_conversion")
    private LocalDate dateConversion;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutAme statut = StatutAme.EN_INTEGRATION;

    @Column(name = "faiseur_id", nullable = false)
    private UUID faiseurId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "situation_familiale")
    private String situationFamiliale;

    @Column(name = "etat_spirituel", nullable = false)
    private String etatSpirituel = "NOUVEAU_CONVERTI";

    @Column(name = "niveau_croissance", nullable = false)
    private Integer niveauCroissance = 1;

    @Column(name = "notes_pasteur")
    private String notesPasteur;

    @Column(name = "date_dernier_contact")
    private LocalDateTime dateDernierContact;

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

    @Transient
    public String getNomComplet() {
        return prenom != null ? prenom + " " + nom : nom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Soul soul = (Soul) o;
        return id != null && id.equals(soul.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
