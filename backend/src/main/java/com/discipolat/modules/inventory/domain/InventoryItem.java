package com.discipolat.modules.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inventaire — matériel, stock, affectations, maintenance.
 * Permet à chaque église de gérer son propre inventaire.
 */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "categorie", nullable = false)
    @Builder.Default
    private String categorie = "MATERIEL"; // MATERIEL, MOBILIER, TECHNIQUE, VESTIMENTAIRE, AUTRE

    @Column(name = "statut", nullable = false)
    @Builder.Default
    private String statut = "DISPONIBLE"; // DISPONIBLE, AFFECTE, EN_MAINTENANCE, PERDU, RETIRE

    @Column(name = "quantite")
    @Builder.Default
    private Integer quantite = 1;

    @Column(name = "quantite_disponible")
    @Builder.Default
    private Integer quantiteDisponible = 1;

    @Column(name = "valeur_unitaire")
    private Double valeurUnitaire;

    @Column(name = "lieu_stockage")
    private String lieuStockage;

    @Column(name = "numero_serie")
    private String numeroSerie;

    @Column(name = "date_acquisition")
    private LocalDateTime dateAcquisition;

    @Column(name = "derniere_maintenance")
    private LocalDateTime derniereMaintenance;

    @Column(name = "prochaine_maintenance")
    private LocalDateTime prochaineMaintenance;

    @Column(name = "departement_id")
    private UUID departementId;

    @Column(name = "affecte_a_id")
    private UUID affecteAId; // memberId

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
