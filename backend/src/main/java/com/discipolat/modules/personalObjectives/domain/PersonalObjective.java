package com.discipolat.modules.personalObjectives.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "personal_objectives")
public class PersonalObjective {

    public enum Statut { EN_COURS, ATTEINT, ABANDONNÉ }
    public enum Catégorie { PRIÈRE, LECTURE, SERVICE, ÉVANGÉLISATION, FORMATION, AUTRE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Catégorie catégorie = Catégorie.AUTRE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_COURS;

    private int objectifCible;
    private int progressionActuelle = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime deadline;
    private LocalDateTime completedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Catégorie getCatégorie() { return catégorie; }
    public void setCatégorie(Catégorie catégorie) { this.catégorie = catégorie; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public int getObjectifCible() { return objectifCible; }
    public void setObjectifCible(int objectifCible) { this.objectifCible = objectifCible; }
    public int getProgressionActuelle() { return progressionActuelle; }
    public void setProgressionActuelle(int progressionActuelle) { this.progressionActuelle = progressionActuelle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
