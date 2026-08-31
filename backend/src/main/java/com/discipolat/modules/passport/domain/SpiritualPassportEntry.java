package com.discipolat.modules.passport.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entrée d'historique d'un passeport spirituel (baptême, formation,
 * certification, service, recommandation, étape de discipolat…).
 */
@Entity
@Table(name = "spiritual_passport_entries")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SpiritualPassportEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "passport_id", nullable = false)
    private UUID passportId;

    /** BAPTISM, FORMATION, CERTIFICATION, SERVICE, RECOMMENDATION, DISCIPLESHIP_STEP, OTHER. */
    @Column(name = "entry_type", nullable = false, length = 40)
    private String entryType = "DISCIPLESHIP_STEP";

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "occurred_at")
    private LocalDate occurredAt;

    @Column(name = "issuing_organization", length = 300)
    private String issuingOrganization;

    /** true = validé par un responsable de l'église émettrice. */
    @Column(nullable = false)
    private Boolean verified = false;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getPassportId() { return passportId; }
    public void setPassportId(UUID passportId) { this.passportId = passportId; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDate occurredAt) { this.occurredAt = occurredAt; }
    public String getIssuingOrganization() { return issuingOrganization; }
    public void setIssuingOrganization(String issuingOrganization) { this.issuingOrganization = issuingOrganization; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
