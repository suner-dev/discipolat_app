package com.discipolat.modules.files.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

/**
 * Pièces jointes génériques (entity_attachments) : le service partagé remplace la
 * liste complète des fichiers d'une entité métier (rapport, demande membre,
 * événement) et refuse les fichiers inexistants — même mécanisme que les transferts.
 */
@ExtendWith(MockitoExtension.class)
class EntityAttachmentServiceTest {

    @Mock private EntityAttachmentRepository attachmentRepository;
    @Mock private FileEntityRepository fileEntityRepository;
    @Mock private SecurityUtils securityUtils;

    private EntityAttachmentService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID entityId = UUID.randomUUID();
    private final UUID fichier1 = UUID.randomUUID();
    private final UUID fichier2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new EntityAttachmentService(attachmentRepository, fileEntityRepository, securityUtils);
    }

    @Test
    void replace_remplaceCompletementLesPiecesJointes() {
        SecurityTestHelper.loginAs(userId);
        when(fileEntityRepository.existsById(fichier1)).thenReturn(true);
        when(fileEntityRepository.existsById(fichier2)).thenReturn(true);

        service.replace(EntityAttachment.EntityType.MAKER_REPORT, entityId, List.of(fichier1, fichier2));

        verify(attachmentRepository).deleteByEntityTypeAndEntityId(EntityAttachment.EntityType.MAKER_REPORT, entityId);
        var captor = org.mockito.ArgumentCaptor.forClass(EntityAttachment.class);
        verify(attachmentRepository, times(2)).save(captor.capture());
        assertEquals(List.of(fichier1, fichier2),
                captor.getAllValues().stream().map(EntityAttachment::getFileId).toList());
        captor.getAllValues().forEach(a -> {
            assertEquals(EntityAttachment.EntityType.MAKER_REPORT, a.getEntityType());
            assertEquals(entityId, a.getEntityId());
            assertEquals(userId, a.getUploadedBy());
        });
    }

    @Test
    void replace_listeVide_retireToutesLesPiecesJointes() {
        service.replace(EntityAttachment.EntityType.EVENT, entityId, List.of());

        verify(attachmentRepository).deleteByEntityTypeAndEntityId(EntityAttachment.EntityType.EVENT, entityId);
        verify(attachmentRepository, never()).save(any(EntityAttachment.class));
    }

    @Test
    void replace_fichierInexistant_throwBusinessRule() {
        when(fileEntityRepository.existsById(fichier1)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.replace(EntityAttachment.EntityType.MEMBER_REQUEST, entityId, List.of(fichier1)));
        // La suppression a bien eu lieu avant l'échec (transaction rollback côté appelant)
        verify(attachmentRepository).deleteByEntityTypeAndEntityId(any(), any());
        verify(attachmentRepository, never()).save(any(EntityAttachment.class));
    }

    @Test
    void itemsFor_retourneNomEtCheminDesFichiers() {
        EntityAttachment a1 = EntityAttachment.builder()
                .id(UUID.randomUUID()).entityType(EntityAttachment.EntityType.FAMILY_REPORT)
                .entityId(entityId).fileId(fichier1).build();
        when(attachmentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
                EntityAttachment.EntityType.FAMILY_REPORT, entityId)).thenReturn(List.of(a1));
        when(fileEntityRepository.findById(fichier1)).thenReturn(Optional.of(
                FileEntity.builder().id(fichier1).nom("Synthèse.pdf").chemin("https://drive/1").build()));

        List<EntityAttachmentService.AttachmentItem> items = service.itemsFor(
                EntityAttachment.EntityType.FAMILY_REPORT, entityId);

        assertEquals(1, items.size());
        assertEquals("Synthèse.pdf", items.get(0).nom());
        assertEquals("https://drive/1", items.get(0).url());
        assertEquals(fichier1, items.get(0).fileId());
    }
}
