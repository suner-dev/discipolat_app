package com.discipolat.modules.makerTracking.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #39 - Suivi de développement faiseur
 */
@Entity
@Table(name = "maker_trackings")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MakerTracking {

    public enum TypeEvenement { FORMATION, COMPETENCE_ACQUISE, AIME_ACCOMPAGNEE, REUNION_FREQUENTEE, DEFI_REUSSI, CERTIFICAT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID faiseurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEvenement typeEvenement;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private UUID refId;
    private String refType;

    private int pointsGagnes = 0;
    private LocalDate dateEvenement = LocalDate.now();
    private LocalDateTime creeLe = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getFaiseurId() { return faiseurId; }
    public void setFaiseurId(UUID faiseurId) { this.faiseurId = faiseurId; }
    public TypeEvenement getTypeEvenement() { return typeEvenement; }
    public void setTypeEvenement(TypeEvenement t) { this.typeEvenement = t; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public UUID getRefId() { return refId; }
    public void setRefId(UUID refId) { this.refId = refId; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public int getPointsGagnes() { return pointsGagnes; }
    public void setPointsGagnes(int p) { this.pointsGagnes = p; }
    public LocalDate getDateEvenement() { return dateEvenement; }
    public void setDateEvenement(LocalDate d) { this.dateEvenement = d; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime l) { this.creeLe = l; }
}
