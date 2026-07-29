package com.discipolat.modules.notifications.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    public NotificationService(NotificationRepository notificationRepository, SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.securityUtils = securityUtils;
    }

    public Notification create(UUID destinataireId, TypeNotification type, CanalNotification canal,
                               String titre, String message, UUID entiteReferenceId, String entiteReferenceType) {
        Notification notification = Notification.builder()
                .destinataireId(destinataireId)
                .type(type)
                .canal(canal)
                .titre(titre)
                .message(message)
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
        notification.setLu(true);
        notification.setDateLecture(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        UUID userId = securityUtils.getCurrentUserId();
        notificationRepository.markAllAsRead(userId);
    }
}
