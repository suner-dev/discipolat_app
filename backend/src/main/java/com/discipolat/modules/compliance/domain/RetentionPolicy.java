package com.discipolat.modules.compliance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/** Politique de rétention des données configurable par type (RGPD Art. 5-1-e). */
@Entity
@Table(name = "retention_policies", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "data_type"}))
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class RetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Type de données : GdprRequest, ConsentLog, AuditLog, SoulNote, Prayer… */
    @Column(name = "data_type", nullable = false, length = 64)
    private String dataType;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    /** Purge physique (true) ou anonymisation (false). */
    @Column(name = "hard_delete", nullable = false)
    private boolean hardDelete = false;

    private LocalDateTime lastPurgeAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isHardDelete() { return hardDelete; }
    public void setHardDelete(boolean hardDelete) { this.hardDelete = hardDelete; }
    public LocalDateTime getLastPurgeAt() { return lastPurgeAt; }
    public void setLastPurgeAt(LocalDateTime lastPurgeAt) { this.lastPurgeAt = lastPurgeAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
