package com.discipolat.modules.prayerJournal.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prayer_journal_entries")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PrayerJournalEntry {

    public enum Statut { EN_COURS, EXAUCÉE, MÉMORISÉE }
    public enum Visibilité { PRIVÉE, FAISEUR, FAMILLE, PUBLIQUE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_COURS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibilité visibilité = Visibilité.PRIVÉE;

    @Column(columnDefinition = "TEXT")
    private String réponse;

    private String category;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private LocalDateTime exaucéeAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public Visibilité getVisibilité() { return visibilité; }
    public void setVisibilité(Visibilité visibilité) { this.visibilité = visibilité; }
    public String getRéponse() { return réponse; }
    public void setRéponse(String réponse) { this.réponse = réponse; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getExaucéeAt() { return exaucéeAt; }
    public void setExaucéeAt(LocalDateTime exaucéeAt) { this.exaucéeAt = exaucéeAt; }
}
