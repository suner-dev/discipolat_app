package com.discipolat.modules.compliance.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_logs")
public class ConsentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID utilisateurId;

    @Column(nullable = false)
    private String typeConsentement;

    @Column(nullable = false)
    private boolean accorde;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(UUID utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getTypeConsentement() { return typeConsentement; }
    public void setTypeConsentement(String typeConsentement) { this.typeConsentement = typeConsentement; }
    public boolean isAccorde() { return accorde; }
    public void setAccorde(boolean accorde) { this.accorde = accorde; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
