package com.discipolat.modules.souls.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "souls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Soul {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "profession")
    private String profession;

    @Column(name = "niveau_etude")
    private String niveauEtude;

    @Column(name = "nb_enfants")
    private Integer nbEnfants;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_disciple", nullable = false)
    private TypeDisciple typeDisciple;

    @Column(name = "date_integration", nullable = false)
    private LocalDate dateIntegration;

    @Column(name = "date_conversion")
    private LocalDate dateConversion;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutAme statut = StatutAme.EN_INTEGRATION;

    @Column(name = "faiseur_id", nullable = false)
    private UUID faiseurId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "situation_familiale")
    private String situationFamiliale;

    @Column(name = "etat_spirituel", nullable = false)
    @Builder.Default
    private String etatSpirituel = "NOUVEAU_CONVERTI";

    @Column(name = "niveau_croissance", nullable = false)
    @Builder.Default
    private Integer niveauCroissance = 1;

    @Column(name = "notes_pasteur")
    private String notesPasteur;

    @Column(name = "date_dernier_contact")
    private LocalDateTime dateDernierContact;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "zone")
    private String zone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Transient
    public String getNomComplet() {
        return prenom != null ? prenom + " " + nom : nom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Soul soul = (Soul) o;
        return id != null && id.equals(soul.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Explicit getters/setters for Lombok compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }
    public String getNiveauEtude() { return niveauEtude; }
    public void setNiveauEtude(String niveauEtude) { this.niveauEtude = niveauEtude; }
    public Integer getNbEnfants() { return nbEnfants; }
    public void setNbEnfants(Integer nbEnfants) { this.nbEnfants = nbEnfants; }
    public TypeDisciple getTypeDisciple() { return typeDisciple; }
    public void setTypeDisciple(TypeDisciple typeDisciple) { this.typeDisciple = typeDisciple; }
    public LocalDate getDateIntegration() { return dateIntegration; }
    public void setDateIntegration(LocalDate dateIntegration) { this.dateIntegration = dateIntegration; }
    public LocalDate getDateConversion() { return dateConversion; }
    public void setDateConversion(LocalDate dateConversion) { this.dateConversion = dateConversion; }
    public StatutAme getStatut() { return statut; }
    public void setStatut(StatutAme statut) { this.statut = statut; }
    public UUID getFaiseurId() { return faiseurId; }
    public void setFaiseurId(UUID faiseurId) { this.faiseurId = faiseurId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getFamilleId() { return familleId; }
    public void setFamilleId(UUID familleId) { this.familleId = familleId; }
    public String getSituationFamiliale() { return situationFamiliale; }
    public void setSituationFamiliale(String situationFamiliale) { this.situationFamiliale = situationFamiliale; }
    public String getEtatSpirituel() { return etatSpirituel; }
    public void setEtatSpirituel(String etatSpirituel) { this.etatSpirituel = etatSpirituel; }
    public Integer getNiveauCroissance() { return niveauCroissance; }
    public void setNiveauCroissance(Integer niveauCroissance) { this.niveauCroissance = niveauCroissance; }
    public String getNotesPasteur() { return notesPasteur; }
    public void setNotesPasteur(String notesPasteur) { this.notesPasteur = notesPasteur; }
    public LocalDateTime getDateDernierContact() { return dateDernierContact; }
    public void setDateDernierContact(LocalDateTime dateDernierContact) { this.dateDernierContact = dateDernierContact; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEmail(String email) { this.email = email; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public void setProfession(String profession) { this.profession = profession; }
    public void setNiveauEtude(String niveauEtude) { this.niveauEtude = niveauEtude; }
    public void setNbEnfants(Integer nbEnfants) { this.nbEnfants = nbEnfants; }
    public void setTypeDisciple(TypeDisciple typeDisciple) { this.typeDisciple = typeDisciple; }
    public void setDateIntegration(LocalDate dateIntegration) { this.dateIntegration = dateIntegration; }
    public void setDateConversion(LocalDate dateConversion) { this.dateConversion = dateConversion; }
    public void setStatut(StatutAme statut) { this.statut = statut; }
    public void setFaiseurId(UUID faiseurId) { this.faiseurId = faiseurId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setFamilleId(UUID familleId) { this.familleId = familleId; }
    public void setSituationFamiliale(String situationFamiliale) { this.situationFamiliale = situationFamiliale; }
    public void setEtatSpirituel(String etatSpirituel) { this.etatSpirituel = etatSpirituel; }
    public void setNiveauCroissance(Integer niveauCroissance) { this.niveauCroissance = niveauCroissance; }
    public void setNotesPasteur(String notesPasteur) { this.notesPasteur = notesPasteur; }
    public void setDateDernierContact(LocalDateTime dateDernierContact) { this.dateDernierContact = dateDernierContact; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setZone(String zone) { this.zone = zone; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}