package com.discipolat.modules.notifications.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0 — Élagage des tokens déclarés invalides par la passerelle.
 *
 * <p>Le dépôt est simulé par un état réel (liste en mémoire) afin que le test
 * prouve l'absence de boucle : après le premier envoi, le token mort a
 * disparu, donc le second envoi ne cible plus personne et ne rappelle pas la
 * passerelle.</p>
 */
@ExtendWith(MockitoExtension.class)
class PushTokenCleanupTest {

    private static final String DEAD_TOKEN = "fcm-token-revoque";

    @Mock private PushGateway pushGateway;
    @Mock private PushTokenRepository pushTokenRepository;
    @Mock private NotificationPreferenceRepository notificationPreferenceRepository;

    private final List<PushToken> store = new ArrayList<>();

    private PushNotificationService service;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(pushGateway, pushTokenRepository, notificationPreferenceRepository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        store.add(token(userId, DEAD_TOKEN));
        when(pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenAnswer(invocation -> new ArrayList<>(store));
    }

    /** Le dépôt simule un élagage réel : la liste en mémoire se vide. */
    private void stubReaping() {
        when(pushTokenRepository.deleteInvalidTokens(eq(tenantId), anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<String> tokens = invocation.getArgument(1);
                    int before = store.size();
                    store.removeIf(t -> tokens.contains(t.getToken()));
                    return before - store.size();
                });
    }

    @Test
    void tokenReportedInvalidByTheGatewayIsDeletedFromTheRepository() {
        stubReaping();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushGateway.send(any(), any()))
                .thenReturn(new PushResult(false, 0, 1, List.of(DEAD_TOKEN)));

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.invalidTokens()).containsExactly(DEAD_TOKEN);
        verify(pushTokenRepository).deleteInvalidTokens(tenantId, List.of(DEAD_TOKEN));
        assertThat(store).isEmpty();
    }

    @Test
    void aSecondIdenticalCallDoesNotSendAgainBecauseTheTokenIsGone() {
        stubReaping();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushGateway.send(any(), any()))
                .thenReturn(new PushResult(false, 0, 1, List.of(DEAD_TOKEN)));

        service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());
        PushResult second = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        verify(pushGateway, times(1)).send(any(), any());
        verify(pushTokenRepository, times(1)).deleteInvalidTokens(eq(tenantId), anyCollection());
        assertThat(second.sentCount()).isZero();
    }

    @Test
    void aValidTokenIsKeptAndReusedOnTheNextPush() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushGateway.send(any(), any())).thenReturn(new PushResult(true, 1, 0, List.of()));

        service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());
        service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        verify(pushGateway, times(2)).send(any(), any());
        verify(pushTokenRepository, never()).deleteInvalidTokens(any(), anyCollection());
        assertThat(store).hasSize(1);
    }

    @Test
    void aFailingGatewayNeverBreaksTheCallerAndNeverElaguesAnything() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushGateway.send(any(), any())).thenThrow(new IllegalStateException("réseau indisponible"));

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.success()).isFalse();
        verify(pushTokenRepository, never()).deleteInvalidTokens(any(), anyCollection());
        assertThat(store).hasSize(1);
    }

    @Test
    void noDeviceRegisteredMeansNoCallToTheGateway() {
        store.clear();
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PushResult result = service.pushToUser(tenantId, userId, "Titre", "Corps", Map.of());

        assertThat(result.sentCount()).isZero();
        verify(pushGateway, never()).send(any(), any());
    }

    private PushToken token(UUID owner, String value) {
        PushToken entity = new PushToken();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUserId(owner);
        entity.setToken(value);
        entity.setPlatform("ANDROID");
        return entity;
    }
}
