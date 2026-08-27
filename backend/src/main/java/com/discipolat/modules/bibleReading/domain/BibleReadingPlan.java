package com.discipolat.modules.bibleReading.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #49/#60 — Plan de lecture biblique partagé
 */
@Entity
@Table(name = "bible_reading_plans")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BibleReadingPlan {

    public enum TypePlan { PARCOURS_365, PSAUMES_PROVERBES, EVANGILES, PERSONNALISE }
    public enum Statut { ACTIF, TERMINE, ABANDONNE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePlan typePlan = TypePlan.PERSONNALISE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIF;

    private UUID createurId;

    private boolean partageFamille = false;

    private int joursTotal = 365;
    private int joursCompletes = 0;

    private LocalDate dateDebut = LocalDate.now();

    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime modifieLe;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TypePlan getTypePlan() { return typePlan; }
    public void setTypePlan(TypePlan typePlan) { this.typePlan = typePlan; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public UUID getCreateurId() { return createurId; }
    public void setCreateurId(UUID createurId) { this.createurId = createurId; }
    public boolean isPartageFamille() { return partageFamille; }
    public void setPartageFamille(boolean partageFamille) { this.partageFamille = partageFamille; }
    public int getJoursTotal() { return joursTotal; }
    public void setJoursTotal(int joursTotal) { this.joursTotal = joursTotal; }
    public int getJoursCompletes() { return joursCompletes; }
    public void setJoursCompletes(int joursCompletes) { this.joursCompletes = joursCompletes; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime modifieLe) { this.modifieLe = modifieLe; }
}
