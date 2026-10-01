package com.discipolat.modules.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * G5.4 (§55-2) — Demande d'accès émise depuis l'écran « Accès refusé » du
 * frontend. Ligne réelle et traçable : anti-spam (fenêtre de cooldown) et
 * historique pour le responsable notifié. Ne remplace JAMAIS une
 * autorisation : l'octroi passe par l'éditeur de rôles/permissions (G1.x).
 */
@Entity
@Table(name = "access_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "permission_key", nullable = false)
    private String permissionKey;

    @Column(name = "resource_label")
    private String resourceLabel;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "notified_count", nullable = false)
    private Integer notifiedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "handled_at")
    private Instant handledAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "NOTIFIED";
        }
        if (notifiedCount == null) {
            notifiedCount = 0;
        }
    }
}
