package com.discipolat.modules.demo.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demande de démonstration soumise depuis la landing page publique
 * (sans authentification). Niveau plateforme : pas de tenant_id —
 * le TenantFilter ignore les chemins /api/v1/public.
 */
@Entity
@Table(name = "demo_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemoRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "church_name", nullable = false, length = 255)
    private String churchName;

    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "NOUVEAU";

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}