package com.discipolat.modules.notifications.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final SecurityUtils securityUtils;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationTemplateRepository notificationTemplateRepository,
                               SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.securityUtils = securityUtils;
    }

    public Notification create(UUID destinataireId, TypeNotification type, CanalNotification canal,
                               String titre, String message, UUID entiteReferenceId, String entiteReferenceType) {
        // Tenant hérité du contexte de requête (JWT) — obligatoire (colonne NOT NULL).
        return create(TenantContext.getTenantId(), destinataireId, type, canal,
                titre, message, entiteReferenceId, entiteReferenceType);
    }

    /**
     * Création avec tenant explicite — utilisé par les jobs planifiés
     * (escalade d'absentéisme, rappels) qui tournent SANS contexte de requête :
     * le tenant est dérivé de l'entité concernée (ex : l'âme de la notification).
     */
    public Notification create(UUID tenantId, UUID destinataireId, TypeNotification type, CanalNotification canal,
                               String titre, String message, UUID entiteReferenceId, String entiteReferenceType) {
        // Rendu à partir d'un modèle actif (centre de configuration admin) — défensif :
        // une anomalie de modèle ne doit jamais empêcher l'émission d'une notification.
        String renderedTitre = titre;
        String renderedMessage = message;
        CanalNotification effectiveCanal = canal;
        try {
            var template = notificationTemplateRepository
                    .findByTenantIdAndEventAndActifTrue(tenantId, type).orElse(null);
            if (template != null) {
                String t = NotificationTemplateService.render(template.getTitre(), type, entiteReferenceType);
                if (t != null) renderedTitre = t;
                String m = NotificationTemplateService.render(template.getMessage(), type, entiteReferenceType);
                if (m != null) renderedMessage = m;
                effectiveCanal = NotificationTemplateService.preferredCanal(template.getCanaux(), canal);
            }
        } catch (Exception e) {
            log.warn("Modèle de notification non appliqué pour {} : {}", type, e.getMessage());
        }

        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .destinataireId(destinataireId)
                .type(type)
                .canal(effectiveCanal)
                .titre(renderedTitre)
                .message(renderedMessage)
                .lu(false)
                .entiteReferenceId(entiteReferenceId)
                .entiteReferenceType(entiteReferenceType)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> findAll(Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();
        return notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> findUnread(Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();
        return notificationRepository.findByDestinataireIdAndLuFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        UUID userId = securityUtils.getCurrentUserId();
        return notificationRepository.countByDestinataireIdAndLuFalse(userId);
    }

    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification", id));
        UUID currentUserId = securityUtils.getCurrentUserId();
        boolean owner = notification.getDestinataireId().equals(currentUserId);
        if (!owner && !securityUtils.isSuperUser()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : cette notification ne vous appartient pas");
        }
        notification.setLu(true);
        notification.setDateLecture(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        UUID userId = securityUtils.getCurrentUserId();
        notificationRepository.markAllAsRead(userId);
    }
}
