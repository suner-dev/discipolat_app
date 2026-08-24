package com.discipolat.modules.usageAnalytics.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P3 #109 — Analytics d'usage self-hosted.
 * Événement de tracking : page vue, action utilisateur, durée, appareil.
 */
@Entity
@Table(name = "usage_events")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    /** Page / route visitée (ex: /souls, /finance). */
    @Column(nullable = false)
    private String page;

    /** Type d'action : PAGE_VIEW, CLICK, EXPORT, SEARCH... */
    @Column(nullable = false)
    private String action = "PAGE_VIEW";

    private String referrer;

    /** Durée de la session sur la page en ms. */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** WEB, MOBILE_ANDROID, MOBILE_IOS. */
    private String device = "WEB";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
