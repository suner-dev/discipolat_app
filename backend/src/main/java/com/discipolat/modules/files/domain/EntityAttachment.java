package com.discipolat.modules.files.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pièce jointe générique : lie un fichier (module fichiers) à une entité métier
 * (rapport faiseur/famille, demande membre, événement). Même pattern que
 * transfer_attachments mais multi-usage via entityType.
 */
@Entity
@Table(name = "entity_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityAttachment {

    /** Types d'entités métier pouvant porter des pièces jointes. */
    public enum EntityType { MAKER_REPORT, FAMILY_REPORT, MEMBER_REQUEST, EVENT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
