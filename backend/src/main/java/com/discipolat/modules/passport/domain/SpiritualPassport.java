package com.discipolat.modules.passport.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Passeport spirituel portable : dossier de discipolat vérifiable d'un membre.
 *
 * Identité vérifiable : le contenu (code + membre + statut + empreinte des
 * entrées) est signé avec la clé RSA de la plateforme (voir
 * {@link PassportSignatureService}). Toute modification du contenu invalide
 * la signature — détection de falsification.
 *
 * NOTE multi-tenant : filtrage Hibernate `tenantFilter` (isolation défense en
 * profondeur) + vérifications service.
 */
@Entity
@Table(name = "spiritual_passports",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_passport_tenant_member", columnNames = {"tenant_id", "member_id"})
        })
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SpiritualPassport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Identifiant du membre titulaire (user_id). */
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    /** Code public unique imprimé dans le QR — ne révèle pas l'identité. */
    @Column(name = "passport_code", nullable = false, unique = true, length = 40)
    private String passportCode;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, REVOKED, EXPIRED

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    /** Empreinte SHA-256 du contenu (entrées) — liée à la signature. */
    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    /** Signature RSA-SHA256 (base64) du payload canonique. */
    @Column(columnDefinition = "TEXT")
    private String signature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
    public String getPassportCode() { return passportCode; }
    public void setPassportCode(String passportCode) { this.passportCode = passportCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
