package com.discipolat.modules.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P0 — Token d'appareil enregistré par un utilisateur pour recevoir des push.
 *
 * <p>Ces tokens sont <b>jetables</b> : FCM les révoque au désinstallation, à la
 * réinitialisation de l'appareil ou au renouvellement. Un token révoqué doit
 * donc être <b>élagué</b> de cette table, sinon il est réessayé à chaque
 * événement et la boucle ne s'arrête jamais (cf.
 * {@link PushResult#invalidTokens()}).</p>
 *
 * <p>Le token est unique globalement : un même appareil ne peut appartenir
 * qu'à un seul compte, même en cas de changement d'utilisateur.</p>
 */
@Entity
@Table(name = "push_tokens", indexes = {
        @Index(name = "idx_push_tokens_user", columnList = "user_id"),
        @Index(name = "idx_push_tokens_tenant", columnList = "tenant_id")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true, length = 400)
    private String token;

    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    /**
     * Le token ne doit jamais être journalisé en clair : seuls les premiers
     * caractères, comme dans {@code PushTokenController}, sont acceptables.
     */
    @Override
    public String toString() {
        return "PushToken{id=" + id
                + ", tenantId=" + tenantId
                + ", userId=" + userId
                + ", token=" + (token == null ? "null" : token.substring(0, Math.min(8, token.length())) + "…")
                + ", platform='" + platform + "'"
                + '}';
    }
}
