package com.discipolat.modules.calendar.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    public enum Source { INTERNE, GOOGLE, OUTLOOK, ICAL }
    public enum Statut { CONFIRMÉ, EN_ATTENTE, ANNULÉ }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime début;

    @Column(nullable = false)
    private LocalDateTime fin;

    private String lieu;

    private UUID événementId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source = Source.INTERNE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.CONFIRMÉ;

    private String externalId;

    private boolean rappelActivé = true;

    private int rappelMinutesAvant = 60;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDébut() { return début; }
    public void setDébut(LocalDateTime début) { this.début = début; }
    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public UUID getÉvénementId() { return événementId; }
    public void setÉvénementId(UUID événementId) { this.événementId = événementId; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public boolean isRappelActivé() { return rappelActivé; }
    public void setRappelActivé(boolean rappelActivé) { this.rappelActivé = rappelActivé; }
    public int getRappelMinutesAvant() { return rappelMinutesAvant; }
    public void setRappelMinutesAvant(int rappelMinutesAvant) { this.rappelMinutesAvant = rappelMinutesAvant; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
