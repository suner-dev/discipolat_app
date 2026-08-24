package com.discipolat.modules.members.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P20 — Ping GPS horodaté lors d'un check-in/check-out géofencing.
 * Sert à l'historique de présence géolocalisée et au mode basse consommation
 * (fréquence d'échantillonnage réduite côté client).
 */
@Entity
@Table(name = "geofence_pings", indexes = {
        @Index(name = "idx_gfp_user", columnList = "userId"),
        @Index(name = "idx_gfp_tenant_time", columnList = "tenantId, createdAt")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class GeofencePing {

    public enum Kind { CHECK_IN, CHECK_OUT, AUTO_CHECK_IN, HEARTBEAT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private double latitude;
    private double longitude;

    /** Précision GPS en mètres au moment du ping. */
    private double accuracy;

    /** Distance calculée jusqu'au centre de la zone (mètres). */
    private int distanceMeters;

    private boolean inZone;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    /** LOW_POWER si l'échantillonnage est réduit (batterie). */
    private String powerMode = "NORMAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID t) { this.tenantId = t; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID u) { this.userId = u; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double v) { this.latitude = v; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double v) { this.longitude = v; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double v) { this.accuracy = v; }
    public int getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(int v) { this.distanceMeters = v; }
    public boolean isInZone() { return inZone; }
    public void setInZone(boolean v) { this.inZone = v; }
    public Kind getKind() { return kind; }
    public void setKind(Kind k) { this.kind = k; }
    public String getPowerMode() { return powerMode; }
    public void setPowerMode(String p) { this.powerMode = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
