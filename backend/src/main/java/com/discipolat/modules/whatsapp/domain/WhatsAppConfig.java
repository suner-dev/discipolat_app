package com.discipolat.modules.whatsapp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_configs")

@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
public class WhatsAppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Phone Number ID fourni par Meta WhatsApp Business Cloud API. */
    @Column(name = "phone_number_id", length = 64)
    private String phoneNumberId;

    /** Identifiant public du numéro affiché (ex. 22507000000). */
    @Column(name = "display_phone_number", length = 32)
    private String displayPhoneNumber;

    /** Token d'accès chiffré AES avant persistance. */
    @Column(name = "access_token_encrypted", columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    /** Token de vérification du webhook (verify token Meta). */
    @Column(name = "webhook_verify_token", length = 128)
    private String webhookVerifyToken;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /** Message envoyé en réponse à #rejoindre. */
    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
