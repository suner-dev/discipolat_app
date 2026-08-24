package com.discipolat.modules.integrations.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Configuration persistée d'un connecteur tiers (feature #3).
 * Remplace la config in-memory perdue au restart.
 */
@Entity
@Table(name = "integration_configs")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class IntegrationConfig {

    public enum Connector {
        ZAPIER, MAKE, GOOGLE_CALENDAR, OUTLOOK_CALENDAR, QUICKBOOKS, XERO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector", nullable = false)
    private Connector connector;

    @Column(nullable = false)
    private boolean enabled = false;

    /** URL du webhook sortant (Zapier/Make) ou endpoint API (QuickBooks). */
    @Column(name = "endpoint_url", length = 512)
    private String endpointUrl;

    /** Secret/clé API chiffré AES. */
    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    /** URL iCal à synchroniser (Google/Outlook). */
    @Column(name = "ical_url", length = 512)
    private String icalUrl;

    /** Dernière synchronisation réussie. */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_sync_status", length = 32)
    private String lastSyncStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Connector getConnector() { return connector; }
    public void setConnector(Connector connector) { this.connector = connector; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getApiKeyEncrypted() { return apiKeyEncrypted; }
    public void setApiKeyEncrypted(String apiKeyEncrypted) { this.apiKeyEncrypted = apiKeyEncrypted; }
    public String getIcalUrl() { return icalUrl; }
    public void setIcalUrl(String icalUrl) { this.icalUrl = icalUrl; }
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
