package com.discipolat.modules.transfers.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pièce jointe d'une demande de transfert.
 * Réutilise le module fichiers existant (table files) via un lien file_id.
 */
@Entity
@Table(name = "transfer_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_request_id", nullable = false)
    private UUID transferRequestId;

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
