package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores transcribed sermons with metadata.
 * When a sermon is recorded, the audio is uploaded, transcribed,
 * and the text is stored for search, study, and sharing.
 */
@Entity
@Table(name = "sermon_transcriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SermonTranscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "speaker")
    private String speaker;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "theme")
    private String theme;

    @Column(name = "reference_biblique")
    private String referenceBiblique;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_verses", columnDefinition = "TEXT")
    private String keyVerses;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "language")
    @Builder.Default
    private String language = "fr";

    @Column(name = "transcription_status")
    @Builder.Default
    private String transcriptionStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @Column(name = "transcribed_at")
    private LocalDateTime transcribedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
