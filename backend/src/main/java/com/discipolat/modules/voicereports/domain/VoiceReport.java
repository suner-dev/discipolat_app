package com.discipolat.modules.voicereports.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rapport vocal de terrain — le faiseur dicte hors réseau, la transcription
 * est stockée localement (Drift) puis synchronisée. L'IA extrait les entités.
 */
@Entity
@Table(name = "voice_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class VoiceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "duration_seconds", nullable = false)
    @Builder.Default
    private int durationSeconds = 0;

    /** Transcription texte (Whisper local côté mobile ou saisie). */
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    /** Entités extraites par l'IA : JSON {personnes:[], humeur, besoinPriere, actions:[]} */
    @Column(name = "extracted_entities", columnDefinition = "TEXT")
    private String extractedEntities;

    @Column(name = "related_soul_id")
    private UUID relatedSoulId;

    @Column(name = "related_family_id")
    private UUID relatedFamilyId;

    /** Vrai si dicté hors ligne puis synchronisé. */
    @Column(name = "synced_offline", nullable = false)
    @Builder.Default
    private boolean syncedOffline = false;

    /** Extraction d'entités déjà exécutée. */
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
