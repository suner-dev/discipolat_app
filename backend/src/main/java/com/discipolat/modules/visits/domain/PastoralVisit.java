package com.discipolat.modules.visits.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pastoral_visits")
public class PastoralVisit {

    public enum Statut { PLANIFIEE, REALISEE, ANNULEE, REPORTEE }
    public enum Motif { ALERTE, DEMANDE_MEMBRE, ROUTINE, NOUVEAU_MEMBRE, SUIVI }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID visiteurId;

    @Column(nullable = false)
    private UUID membreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Motif motif = Motif.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.PLANIFIEE;

    @Column(nullable = false)
    private LocalDateTime prévuLe;

    private LocalDateTime réaliséLe;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private boolean autoGénéré = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getVisiteurId() { return visiteurId; }
    public void setVisiteurId(UUID visiteurId) { this.visiteurId = visiteurId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public Motif getMotif() { return motif; }
    public void setMotif(Motif motif) { this.motif = motif; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDateTime getPrévuLe() { return prévuLe; }
    public void setPrévuLe(LocalDateTime prévuLe) { this.prévuLe = prévuLe; }
    public LocalDateTime getRéaliséLe() { return réaliséLe; }
    public void setRéaliséLe(LocalDateTime réaliséLe) { this.réaliséLe = réaliséLe; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isAutoGénéré() { return autoGénéré; }
    public void setAutoGénéré(boolean autoGénéré) { this.autoGénéré = autoGénéré; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
