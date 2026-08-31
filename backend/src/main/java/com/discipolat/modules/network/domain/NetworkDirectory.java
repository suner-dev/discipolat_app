package com.discipolat.modules.network.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Répertoire volontaire des églises membres du réseau.
 * Une église n'apparaît dans l'annuaire QUE si elle a explicitement opt-in (is_listed = true).
 * Les données de localisation précises ne sont jamais exposées publiquement.
 *
 * NOTE multi-tenant : PAS de @Filter(tenantFilter) Hibernate ici — l'annuaire liste
 * des églises de tous les tenants ayant opt-in (voir NetworkResource). La mise à jour
 * de la propre entrée est scopée au tenant courant dans NetworkService.
 */
@Entity
@Table(name = "network_directory")
public class NetworkDirectory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    /** Nullable : l'entrée est créée implicitement au premier opt-in ; le nom
     *  est obligatoire pour publier le listing (vérifié dans le service). */
    @Column(name = "church_name", length = 300)
    private String churchName;

    @Column(length = 200)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(length = 200)
    private String denomination;

    @Column(name = "pastor_name", length = 300)
    private String pastorName;

    @Column(name = "contact_email", length = 300)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(length = 500)
    private String website;

    @Column(name = "member_count")
    private Integer memberCount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_listed", nullable = false)
    private Boolean isListed = false;

    @Column(name = "listed_at")
    private LocalDateTime listedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getChurchName() { return churchName; }
    public void setChurchName(String churchName) { this.churchName = churchName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getDenomination() { return denomination; }
    public void setDenomination(String denomination) { this.denomination = denomination; }
    public String getPastorName() { return pastorName; }
    public void setPastorName(String pastorName) { this.pastorName = pastorName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Boolean getIsListed() { return isListed; }
    public void setIsListed(Boolean isListed) { this.isListed = isListed; }
    public LocalDateTime getListedAt() { return listedAt; }
    public void setListedAt(LocalDateTime listedAt) { this.listedAt = listedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
