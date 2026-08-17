package com.discipolat.modules.platform.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Entrée de menu configurable : libellé, icône, section, ordre et rôles
 * visibles. Chaque menu peut être rattaché à un module (sa visibilité dépend
 * alors de l'activation de celui-ci) ou autonome.
 */
@Entity
@Table(name = "menu_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MenuEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key", nullable = false, unique = true, length = 50)
    private String key;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "href", nullable = false)
    private String href;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "section", nullable = false)
    @Builder.Default
    private String section = "Général";

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private int ordre = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Column(name = "module_key", length = 50)
    private String moduleKey;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

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

    public boolean isVisibleForRole(String role) {
        return roles == null || roles.isEmpty() || roles.contains(role);
    }
}
