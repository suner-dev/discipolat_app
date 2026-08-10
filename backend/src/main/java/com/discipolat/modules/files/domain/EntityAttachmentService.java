package com.discipolat.modules.files.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Pièces jointes génériques : lie des documents du module Fichiers à une entité
 * métier (rapport faiseur/famille, demande membre, événement). Le remplacement est
 * complet (delete + relink), comme pour les pièces jointes des demandes de transfert.
 * Réutilisé par tous les formulaires — un seul mécanisme, pas de systèmes parallèles.
 */
@Service
public class EntityAttachmentService {

    private final EntityAttachmentRepository attachmentRepository;
    private final FileEntityRepository fileEntityRepository;
    private final SecurityUtils securityUtils;

    public EntityAttachmentService(EntityAttachmentRepository attachmentRepository,
                                   FileEntityRepository fileEntityRepository,
                                   SecurityUtils securityUtils) {
        this.attachmentRepository = attachmentRepository;
        this.fileEntityRepository = fileEntityRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Remplace la liste complète des pièces jointes d'une entité.
     * Passer une liste vide (ou non nulle) retire toutes les pièces jointes.
     */
    @Transactional
    public void replace(EntityAttachment.EntityType entityType, UUID entityId, List<UUID> fileIds) {
        if (fileIds == null) return;
        attachmentRepository.deleteByEntityTypeAndEntityId(entityType, entityId);
        for (UUID fileId : fileIds) {
            if (!fileEntityRepository.existsById(fileId)) {
                throw new BusinessRuleException("Pièce jointe introuvable : " + fileId);
            }
            attachmentRepository.save(EntityAttachment.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .fileId(fileId)
                    .uploadedBy(securityUtils.getCurrentUserId())
                    .build());
        }
    }

    /** Items de réponse (id de liaison, fileId, nom, url/chemin) pour une entité. */
    @Transactional(readOnly = true)
    public List<AttachmentItem> itemsFor(EntityAttachment.EntityType entityType, UUID entityId) {
        return attachmentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId).stream()
                .map(a -> new AttachmentItem(a.getId(), a.getFileId(),
                        fileEntityRepository.findById(a.getFileId()).map(FileEntity::getNom).orElse(null),
                        fileEntityRepository.findById(a.getFileId()).map(FileEntity::getChemin).orElse(null)))
                .toList();
    }

    /** Item léger de pièce jointe exposé dans les réponses API. */
    public record AttachmentItem(UUID id, UUID fileId, String nom, String url) {}
}
