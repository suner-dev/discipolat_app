package com.discipolat.modules.notifications.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SecurityUtils securityUtils;

    private NotificationService service;
    private UUID currentUserId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, securityUtils);
        currentUserId = UUID.randomUUID();
        notification = Notification.builder()
                .id(UUID.randomUUID())
                .destinataireId(currentUserId)
                .type(TypeNotification.INFORMATION)
                .canal(CanalNotification.IN_APP)
                .titre("Titre")
                .message("Message")
                .lu(false)
                .build();
    }

    @Test
    void markAsRead_Owner_ShouldMarkRead() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

        service.markAsRead(notification.getId());

        assertTrue(notification.isLu());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NonOwner_NonSuperUser_ShouldThrowAccessDenied() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(securityUtils.isSuperUser()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.markAsRead(notification.getId()));

        assertFalse(notification.isLu());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_NonOwner_SuperUser_ShouldMarkRead() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(securityUtils.isSuperUser()).thenReturn(true);

        service.markAsRead(notification.getId());

        assertTrue(notification.isLu());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_Unknown_ShouldThrowEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.markAsRead(id));
    }

    @Test
    void findAll_ShouldOnlyReturnCurrentUserNotifications() {
        var pageable = PageRequest.of(0, 20);
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(currentUserId, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        var result = service.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(currentUserId, result.getContent().get(0).getDestinataireId());
    }
}
