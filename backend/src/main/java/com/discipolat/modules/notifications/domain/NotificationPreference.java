package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * P21 — Préférences de notification par utilisateur.
 * L'utilisateur choisit les canaux qu'il accepte ; le service de notification
 * dégrade automatiquement vers IN_APP quand un canal est refusé.
 */
@Entity
@Table(name = "notification_preferences")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    private boolean emailEnabled = true;
    private boolean pushEnabled = true;
    private boolean smsEnabled = false; // opt-in (coût)
    private boolean whatsappEnabled = false; // opt-in
    private boolean inAppEnabled = true;

    /** Heure locale (0-23) avant laquelle ne pas déranger (optionnel). */
    private Integer quietHoursStart;
    private Integer quietHoursEnd;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean allows(CanalNotification canal) {
        return switch (canal) {
            case EMAIL -> emailEnabled;
            case PUSH -> pushEnabled;
            case SMS -> smsEnabled;
            case WHATSAPP -> whatsappEnabled;
            case IN_APP -> inAppEnabled;
        };
    }

    /** Canaux explicitement refusés. */
    public Set<CanalNotification> refused() {
        EnumSet<CanalNotification> out = EnumSet.noneOf(CanalNotification.class);
        for (CanalNotification c : CanalNotification.values()) {
            if (!allows(c)) out.add(c);
        }
        return out;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID t) { this.tenantId = t; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID u) { this.userId = u; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean v) { this.emailEnabled = v; }
    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean v) { this.pushEnabled = v; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean v) { this.smsEnabled = v; }
    public boolean isWhatsappEnabled() { return whatsappEnabled; }
    public void setWhatsappEnabled(boolean v) { this.whatsappEnabled = v; }
    public boolean isInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(boolean v) { this.inAppEnabled = v; }
    public Integer getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(Integer v) { this.quietHoursStart = v; }
    public Integer getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(Integer v) { this.quietHoursEnd = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
