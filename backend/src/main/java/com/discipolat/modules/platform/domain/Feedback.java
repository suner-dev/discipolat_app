package com.discipolat.modules.platform.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Retour d'un testeur (bug, suggestion, problème UX...) envoyé via le
 * widget de feedback intégré à l'application. Statut géré par l'admin
 * (NOUVEAU → EN_COURS → RÉSOLU / REJETÉ).
 */
@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private String priority = "MOYENNE";

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "browser", length = 200)
    private String browser;

    @Column(name = "device", length = 200)
    private String device;

    @Column(name = "os", length = 200)
    private String os;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "NOUVEAU";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
