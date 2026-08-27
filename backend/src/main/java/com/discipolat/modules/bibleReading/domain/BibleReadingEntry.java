package com.discipolat.modules.bibleReading.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entrée de lecture biblique quotidienne (verset lu + note).
 */
@Entity
@Table(name = "bible_reading_entries")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BibleReadingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private UUID utilisateurId;

    @Column(nullable = false)
    private String referenceVerset; // e.g. "Jean 3:16-21"

    private String categorie; // Évangile, Psaume, Proverbe, etc.

    private String theme;

    private boolean lu = false;

    private LocalDate dateLecture = LocalDate.now();

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime creeLe = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(UUID utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getReferenceVerset() { return referenceVerset; }
    public void setReferenceVerset(String referenceVerset) { this.referenceVerset = referenceVerset; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
    public LocalDate getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDate dateLecture) { this.dateLecture = dateLecture; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
}
