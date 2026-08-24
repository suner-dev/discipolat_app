package com.discipolat.modules.compliance.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/** Trace d'un export RGPD (portabilité Art. 20) effectué avant purge ou sur demande. */
@Entity
@Table(name = "data_export_records")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DataExportRecord {

    public enum Format { JSON, CSV }
    public enum Motif { DEMANDE_UTILISATEUR, AVANT_PURGE, AUDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Format format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Motif motif;

    @Column(name = "record_count")
    private int recordCount;

    @Column(length = 512)
    private String fichierPath;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Format getFormat() { return format; }
    public void setFormat(Format format) { this.format = format; }
    public Motif getMotif() { return motif; }
    public void setMotif(Motif motif) { this.motif = motif; }
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    public String getFichierPath() { return fichierPath; }
    public void setFichierPath(String fichierPath) { this.fichierPath = fichierPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
