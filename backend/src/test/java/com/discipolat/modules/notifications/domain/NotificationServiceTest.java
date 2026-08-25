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
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private com.discipolat.modules.users.domain.UserRepository userRepository;
    @Mock
    private org.springframework.mail.javamail.JavaMailSender mailSender;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private NotificationService service;
    private UUID currentUserId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        service = new NotificationService(notificationRepository, notificationTemplateRepository, securityUtils, userRepository, mailSender, notificationPreferenceRepository);
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
        SecurityTestHelper.loginAs(currentUserId);

        service.markAsRead(notification.getId());

        assertTrue(notification.isLu());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NonOwner_NonSuperUser_ShouldThrowAccessDenied() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        SecurityTestHelper.loginAs(UUID.randomUUID());
        when(securityUtils.isSuperUser()).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.markAsRead(notification.getId()));

        assertFalse(notification.isLu());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_NonOwner_SuperUser_ShouldMarkRead() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        SecurityTestHelper.loginAs(UUID.randomUUID());
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
        SecurityTestHelper.loginAs(currentUserId);
        when(notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(currentUserId, pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        var result = service.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(currentUserId, result.getContent().get(0).getDestinataireId());
    }

    @Test
    void create_WithExplicitTenant_ShouldPersistTenantId() {
        UUID tenantId = UUID.randomUUID();
        UUID destinataireId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(
                tenantId, destinataireId, TypeNotification.ALERTE_ABSENCE, CanalNotification.IN_APP,
                "Titre", "Message", UUID.randomUUID(), "SOUL");

        assertEquals(tenantId, saved.getTenantId());
        assertEquals(destinataireId, saved.getDestinataireId());
        verify(notificationRepository).save(argThat(n -> tenantId.equals(n.getTenantId())));
    }

    @Test
    void create_WithActiveTemplate_ShouldRenderTitleAndMessage() {
        UUID tenantId = UUID.randomUUID();
        UUID destinataireId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .event(TypeNotification.ALERTE_ABSENCE)
                .titre("{{type}} — Alerte personnalisée")
                .message("Entité : {{entiteType}} — sur mesure")
                .canaux(new java.util.ArrayList<>(List.of(CanalNotification.PUSH, CanalNotification.IN_APP)))
                .actif(true)
                .build();
        when(notificationTemplateRepository.findByTenantIdAndEventAndActifTrue(tenantId, TypeNotification.ALERTE_ABSENCE))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(
                tenantId, destinataireId, TypeNotification.ALERTE_ABSENCE, CanalNotification.IN_APP,
                "Titre par défaut", "Message par défaut", refId, "SOUL");

        assertEquals("ALERTE_ABSENCE — Alerte personnalisée", saved.getTitre());
        assertEquals("Entité : SOUL — sur mesure", saved.getMessage());
        assertEquals(CanalNotification.IN_APP, saved.getCanal());
    }

    @Test
    void create_WithActiveTemplateWithoutFallbackCanal_ShouldPickTemplateChannel() {
        UUID tenantId = UUID.randomUUID();
        UUID destinataireId = UUID.randomUUID();
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .event(TypeNotification.INFORMATION)
                .titre("Custom")
                .message("Custom message")
                .canaux(new java.util.ArrayList<>(List.of(CanalNotification.PUSH)))
                .actif(true)
                .build();
        when(notificationTemplateRepository.findByTenantIdAndEventAndActifTrue(tenantId, TypeNotification.INFORMATION))
                .thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(
                tenantId, destinataireId, TypeNotification.INFORMATION, CanalNotification.EMAIL,
                "Titre", "Message", null, null);

        assertEquals("Custom", saved.getTitre());
        assertEquals(CanalNotification.PUSH, saved.getCanal());
    }

    @Test
    void create_WithoutTemplate_ShouldKeepProvidedTitleAndMessage() {
        UUID tenantId = UUID.randomUUID();
        UUID destinataireId = UUID.randomUUID();
        when(notificationTemplateRepository.findByTenantIdAndEventAndActifTrue(tenantId, TypeNotification.INFORMATION))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(
                tenantId, destinataireId, TypeNotification.INFORMATION, CanalNotification.IN_APP,
                "Titre par défaut", "Message par défaut", null, null);

        assertEquals("Titre par défaut", saved.getTitre());
        assertEquals("Message par défaut", saved.getMessage());
        assertEquals(CanalNotification.IN_APP, saved.getCanal());
    }

    @Test
    void create_WithoutTenantContext_ShouldPersistNullTenantId() {
        // Sans contexte de requête ni tenant explicite (jobs planifiés) : la
        // recherche de modèle renvoie vide et la création ne doit pas crasher.
        UUID destinataireId = UUID.randomUUID();
        when(notificationTemplateRepository.findByTenantIdAndEventAndActifTrue(null, TypeNotification.INFORMATION))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Notification saved = service.create(
                destinataireId, TypeNotification.INFORMATION, CanalNotification.IN_APP,
                "Titre", "Message", null, null);

        assertNull(saved.getTenantId());
        verify(notificationRepository).save(any(Notification.class));
    }
}
