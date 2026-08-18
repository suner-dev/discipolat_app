package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modèle de notification configurable par l'administrateur (centre de configuration).
 *
 * <p>Un modèle est attaché à un {@link TypeNotification} donné (événement), pour un
 * tenant. Il définit le titre et le message rendus à l'émission (avec variables
 * {@code {{...}}}), les canaux de diffusion et les rôles destinataires recommandés.
 * Lorsque le {@link NotificationService} émet une notification d'un type disposant
 * d'un modèle actif, la notification reprend le titre/message rendu du modèle.
 *
 * <p>Le filtrage multi-tenant est fait explicitement par {@code tenantId} dans les
 * requêtes du repository (pas d'annotation {@code @Filter}) afin de rester fiable
 * hors contexte requête (jobs planifiés).
 */
@Entity
@Table(name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_template_tenant_event",
                columnNames = {"tenant_id", "event"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false)
    private TypeNotification event;

    @Column(name = "titre")
    private String titre;

    @Column(name = "message")
    private String message;

    @ElementCollection
    @CollectionTable(name = "notification_template_channels",
            joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "canal")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<CanalNotification> canaux = new ArrayList<>(List.of(CanalNotification.IN_APP));

    @ElementCollection
    @CollectionTable(name = "notification_template_roles",
            joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "role")
    @Builder.Default
    private List<String> rolesDestinataires = new ArrayList<>();

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

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
