package com.discipolat.modules.notifications.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0 — Respect du canal PUSH dans les préférences utilisateur.
 *
 * <p>Un utilisateur qui a coupé les notifications push ne doit jamais être
 * sollicité, même si ses appareils sont enregistrés. L'in-app, elle, continue
 * d'être créée par {@code OutboxConsumers}.</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferencePushTest {

    @Mock private PushGateway pushGateway;
    @Mock private PushTokenRepository pushTokenRepository;
    @Mock private NotificationPreferenceRepository notificationPreferenceRepository;

    private PushNotificationService service;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(pushGateway, pushTokenRepository, notificationPreferenceRepository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void pushIsSuppressedWhenTheUserDisabledThePushChannel() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference(false)));

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.sentCount()).isZero();
        verify(pushTokenRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
        verify(pushGateway, never()).send(any(), any());
    }

    @Test
    void pushIsSentWhenTheUserKeepsThePushChannelEnabled() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference(true)));
        when(pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(registeredToken()));
        when(pushGateway.send(any(), any())).thenReturn(new PushResult(true, 1, 0, List.of()));

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.sentCount()).isEqualTo(1);
        verify(pushGateway).send(List.of("fcm-token-actif"), new PushMessage("Titre", "Corps", Map.of()));
    }

    @Test
    void pushIsSentWhenNoPreferenceIsRecorded() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(registeredToken()));
        when(pushGateway.send(any(), any())).thenReturn(new PushResult(true, 1, 0, List.of()));

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.sentCount()).isEqualTo(1);
        verify(pushGateway).send(any(), any());
    }

    @Test
    void unreadablePreferencesFallBackToSendingRatherThanSilentlyLosingNotifications() {
        when(notificationPreferenceRepository.findByUserId(userId))
                .thenThrow(new IllegalStateException("table preferences indisponible"));
        when(pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(registeredToken()));
        when(pushGateway.send(any(), any())).thenReturn(new PushResult(true, 1, 0, List.of()));

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.sentCount()).isEqualTo(1);
    }

    @Test
    void isPushAllowedReflectsTheRecordedPreference() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference(false)));
        assertThat(service.isPushAllowed(userId)).isFalse();
    }

    private NotificationPreference preference(boolean pushEnabled) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setPushEnabled(pushEnabled);
        preference.setInAppEnabled(true);
        return preference;
    }

    private PushToken registeredToken() {
        PushToken entity = new PushToken();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setToken("fcm-token-actif");
        entity.setPlatform("ANDROID");
        return entity;
    }
}
