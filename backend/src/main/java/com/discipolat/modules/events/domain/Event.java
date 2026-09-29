package com.discipolat.modules.events.domain;

import com.discipolat.modules.scoping.domain.ResourceScope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// Dérive de schéma corrigée (famille H, attrapée par le replay de recette
// §5.5 sur PostgreSQL réel le 2026-09-29) : V158 avait renommé la table
// physique « events » en « legacy_events » sans publier de mappage pour
// l'entité — toute la surface /api/v1/events (dont FIRST_EVENT du wizard)
// répondait 500 « relation events does not exist » sur les bases migrées.
// Le profil de test H2 (ddl-auto create-drop) masquait la dérive. V194
// remplace le nom réel en « events » pour rejoindre le contrat du code.
// La table Church OS « event » (V158) reste propriété exclusive de ChurchEvent.
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "organisateur_id", nullable = false)
    private UUID organisateurId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "department_id")
    private UUID departmentId;

    // G1.8 §54 — Resource scoping
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_scope", nullable = false, length = 20)
    @Builder.Default
    private ResourceScope resourceScope = ResourceScope.TENANT_GLOBAL;

    @Column(name = "organization_unit_id")
    private UUID organizationUnitId;

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "limite_places")
    private Integer limitePlaces;

    @Builder.Default
    @Column(name = "nb_inscrits", nullable = false)
    private Integer nbInscrits = 0;

    @Column(name = "statut", nullable = false)
    private String statut = "PLANIFIE";

    @Column(name = "compte_rendu")
    private String compteRendu;

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
        Event event = (Event) o;
        return id != null && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
