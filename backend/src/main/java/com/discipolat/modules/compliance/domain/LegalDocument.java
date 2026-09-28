package com.discipolat.modules.compliance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Document légal versionné (CGU, PRIVACY, DPA, CONSENT_ART9…).
 *
 * <p>Chaque acceptation de consentement référence (code, version) : la preuve
 * RGPD (art. 7) consiste à pouvoir retrouver le texte exact accepté par
 * l'utilisateur, même après publication d'une nouvelle version.</p>
 */
@Entity
@Table(name = "legal_documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_legal_documents_code_version_lang", columnNames = {"code", "version", "language"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Code métier stable : CGU, PRIVACY, DPA, CONSENT_ART9. */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "language", nullable = false, length = 5)
    @Builder.Default
    private String language = "fr";

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = true;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.publishedAt == null) this.publishedAt = Instant.now();
        if (this.language == null) this.language = "fr";
        if (this.published == null) this.published = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegalDocument that = (LegalDocument) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
