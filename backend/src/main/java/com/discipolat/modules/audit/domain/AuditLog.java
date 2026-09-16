package com.discipolat.modules.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "audit_logs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entite_type", nullable = false)
    private String entiteType;

    @Column(name = "entite_id")
    private UUID entiteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancien_valeur", columnDefinition = "jsonb")
    private Map<String, Object> ancienValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_valeur", columnDefinition = "jsonb")
    private Map<String, Object> nouvelleValeur;

    @Column(name = "adresse_ip")
    private String adresseIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Explicit getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(UUID utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntiteType() { return entiteType; }
    public void setEntiteType(String entiteType) { this.entiteType = entiteType; }
    public UUID getEntiteId() { return entiteId; }
    public void setEntiteId(UUID entiteId) { this.entiteId = entiteId; }
    public Map<String, Object> getAncienValeur() { return ancienValeur; }
    public void setAncienValeur(Map<String, Object> ancienValeur) { this.ancienValeur = ancienValeur; }
    public Map<String, Object> getNouvelleValeur() { return nouvelleValeur; }
    public void setNouvelleValeur(Map<String, Object> nouvelleValeur) { this.nouvelleValeur = nouvelleValeur; }
    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}