package com.discipolat.modules.leaveRequests.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    public enum Type { MALADIE, CONGE, MISSION, PERSONNEL, AUTRE }
    public enum Statut { EN_ATTENTE, APPROUVE, REFUSE, ANNULE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.MALADIE;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(nullable = false)
    private UUID demandeurId;

    private UUID valideParId;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public UUID getDemandeurId() { return demandeurId; }
    public void setDemandeurId(UUID demandeurId) { this.demandeurId = demandeurId; }
    public UUID getValideParId() { return valideParId; }
    public void setValideParId(UUID valideParId) { this.valideParId = valideParId; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
