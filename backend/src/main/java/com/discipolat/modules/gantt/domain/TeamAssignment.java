package com.discipolat.modules.gantt.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_assignments")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TeamAssignment {

    public enum Statut { PLANIFIÉE, EN_COURS, TERMINÉE, ANNULÉE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID équipeId;

    private UUID événementId;

    @Column(nullable = false)
    private String rôle;

    private UUID membreId;

    @Column(nullable = false)
    private LocalDateTime début;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.PLANIFIÉE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getÉquipeId() { return équipeId; }
    public void setÉquipeId(UUID équipeId) { this.équipeId = équipeId; }
    public UUID getÉvénementId() { return événementId; }
    public void setÉvénementId(UUID événementId) { this.événementId = événementId; }
    public String getRôle() { return rôle; }
    public void setRôle(String rôle) { this.rôle = rôle; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public LocalDateTime getDébut() { return début; }
    public void setDébut(LocalDateTime début) { this.début = début; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
