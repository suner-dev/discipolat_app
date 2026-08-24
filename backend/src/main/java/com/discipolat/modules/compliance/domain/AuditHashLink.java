package com.discipolat.modules.compliance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chaîne de hachage immuable du journal d'audit (feature #4).
 * Chaque maillon référence une entrée d'AuditLog et contient
 * SHA-256(hash_précédent + contenu) — toute altération casse la chaîne.
 */
@Entity
@Table(name = "audit_hash_chain")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AuditHashLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "audit_log_id", nullable = false)
    private UUID auditLogId;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    /** SHA-256 hexadécimal (hash précédent + entrée). */
    @Column(name = "entry_hash", nullable = false, length = 64)
    private String entryHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getAuditLogId() { return auditLogId; }
    public void setAuditLogId(UUID auditLogId) { this.auditLogId = auditLogId; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
