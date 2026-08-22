package com.discipolat.modules.facerec.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gabarit biométrique facial — empreinte perceptuelle (dHash 256 bits) d'un
 * visage de référence, calculée côté serveur depuis la photo d'enrôlement.
 * Utilisée pour le pointage des présences par reconnaissance faciale.
 *
 * Note vie privée : aucune image n'est stockée — uniquement l'empreinte non
 * réversible. Conforme au module RGPD (droit à l'effacement = suppression
 * de la ligne).
 */
@Entity
@Table(name = "face_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_face_user", columnNames = {"tenant_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FaceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Compte utilisateur rattaché (pointage présence). */
    @Column(name = "user_id")
    private UUID userId;

    /** Âme rattachée optionnellement (visiteurs non-comptes). */
    @Column(name = "soul_id")
    private UUID soulId;

    /** Nom d'affichage au moment de l'enrôlement. */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** Empreinte perceptuelle 256 bits encodée en hexadécimal (64 caractères). */
    @Column(name = "descriptor_hash", nullable = false, length = 64)
    private String descriptorHash;

    /**
     * Diversité mesurée du gabarit (bits distincts / 256). Une empreinte trop
     * homogène (photo unie, écran) est signalée comme faible qualité.
     */
    @Column(name = "quality_score", nullable = false)
    @Builder.Default
    private double qualityScore = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
