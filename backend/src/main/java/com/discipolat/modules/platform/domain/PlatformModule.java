package com.discipolat.modules.platform.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Module de la plateforme, activable/désactivable par l'administrateur.
 * Un module désactivé est masqué dans les menus ET son API est bloquée
 * côté serveur (ModuleGateFilter).
 */
@Entity
@Table(name = "platform_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PlatformModule {

    @Id
    @Column(name = "key", nullable = false, length = 50)
    private String key;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "description")
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "section", nullable = false)
    @Builder.Default
    private String section = "Général";

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private int ordre = 0;

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
