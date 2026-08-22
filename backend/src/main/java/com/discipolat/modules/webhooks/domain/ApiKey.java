package com.discipolat.modules.webhooks.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clé API publique (connecteur écosystème : Zapier, Make, intégrations externes).
 * La clé brute n'est stockée nulle part — uniquement son hash SHA-256 et son préfixe.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    /** SHA-256 hexadécimal de la clé complète. */
    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    /** 8 premiers caractères de la clé, pour identification visuelle. */
    @Column(name = "prefix", nullable = false)
    private String prefix;

    /** Scopes séparés par virgule : read, write, admin. */
    @Column(name = "scopes", nullable = false)
    @Builder.Default
    private String scopes = "read";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
