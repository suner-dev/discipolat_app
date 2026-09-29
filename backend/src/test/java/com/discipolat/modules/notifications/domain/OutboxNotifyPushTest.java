package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.propagation.EntityChangeBroadcaster;
import com.discipolat.modules.audit.service.AuditEventService;
import com.discipolat.modules.core.domain.OutboxEvent;
import com.discipolat.modules.core.repository.OutboxEventRepository;
import com.discipolat.modules.core.repository.ProcessedEventRepository;
import com.discipolat.modules.core.service.OutboxConsumers;
import com.discipolat.modules.core.service.OutboxPublisher;
import com.discipolat.modules.core.service.RealTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0 — Consommateur NOTIFY de l'outbox : in-app + push.
 *
 * <p>Vérifie que l'événement produit <b>une</b> notification in-app et
 * <b>un</b> appel à la passerelle push — à travers l'interface
 * {@link PushGateway}, jamais à travers un fournisseur concret. Aucun appel
 * réseau n'est possible : la passerelle est un mock.</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboxNotifyPushTest {

    @Mock private OutboxPublisher outboxPublisher;
    @Mock private AuditEventService auditEventService;
    @Mock private RealTimeService realTimeService;
    @Mock private EntityChangeBroadcaster sseBroadcaster;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private ProcessedEventRepository processedRepository;
    @Mock private NotificationService notificationService;
    @Mock private PushGateway pushGateway;
    @Mock private PushTokenRepository pushTokenRepository;
    @Mock private NotificationPreferenceRepository notificationPreferenceRepository;

    private UUID tenantId;
    private UUID userId;
    private OutboxConsumers consumers;

    @BeforeEach
    void setUp() {
        PushNotificationService pushService = new PushNotificationService(
                pushGateway, pushTokenRepository, notificationPreferenceRepository);
        consumers = new OutboxConsumers(outboxPublisher, auditEventService, realTimeService, sseBroadcaster,
                outboxRepository, processedRepository, notificationService, pushService);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        consumers.registerAllConsumers();
    }

    @Test
    void notifyEventCreatesAnInAppNotificationAndCallsThePushGatewayOnce() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(registeredToken()));
        when(pushGateway.send(any(), any())).thenReturn(new PushResult(true, 1, 0, List.of()));

        notifyConsumerFor("TaskAssigned").accept(event("TaskAssigned", "TASK",
                Map.of("assigneeId", userId.toString(), "taskName", "Visite des familles")));

        verify(notificationService).create(eq(tenantId), eq(userId), eq(TypeNotification.TACHE_ASSIGNEE),
                eq(CanalNotification.IN_APP), eq("Tâche assignée"), any(), eq(taskId), eq("TASK"));
        verify(pushGateway, times(1)).send(anyList(), any(PushMessage.class));
    }

    @Test
    void pushCarriesTheNavigationDataOfTheEvent() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pushTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(registeredToken()));
        when(pushGateway.send(any(), any())).thenReturn(new PushResult(true, 1, 0, List.of()));

        notifyConsumerFor("TaskAssigned").accept(event("TaskAssigned", "TASK",
                Map.of("assigneeId", userId.toString(), "taskName", "Visite des familles")));

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushGateway).send(anyList(), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("Tâche assignée");
        assertThat(captor.getValue().body()).contains("Visite des familles");
        assertThat(captor.getValue().data())
                .containsEntry("eventType", "TaskAssigned")
                .containsEntry("notificationType", TypeNotification.TACHE_ASSIGNEE.name())
                .containsEntry("aggregateType", "TASK");
    }

    @Test
    void anEventWithoutAUserRecipientNotifiesNobody() {
        notifyConsumerFor("MemberRegistered").accept(event("MemberRegistered", "PERSON",
                Map.of("personId", userId.toString(), "firstName", "Jean")));

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
        verify(pushGateway, never()).send(anyList(), any());
    }

    @Test
    void aFailingInAppCreationStillLeavesThePushUntriedWithoutBreakingTheEvent() {
        when(notificationService.create(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("base indisponible"));

        // Aucune exception ne doit remonter : l'outbox reprogrammerait sinon
        // l'événement et dupliquerait la notification à chaque tentative.
        notifyConsumerFor("TaskAssigned").accept(event("TaskAssigned", "TASK",
                Map.of("assigneeId", userId.toString(), "taskName", "Visite des familles")));

        verify(pushGateway, never()).send(anyList(), any());
    }

    private final UUID taskId = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private Consumer<OutboxEvent> notifyConsumerFor(String eventType) {
        ArgumentCaptor<Consumer<OutboxEvent>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(outboxPublisher, atLeastOnce()).registerConsumer(eq(eventType), captor.capture());
        // registerAllConsumers() enregistre d'abord tous les consommateurs NOTIFY,
        // puis AUDIT / BUSINESS_HISTORY / ANALYTICS / REALTIME : le premier
        // enregistrement pour un type d'événement est donc bien consumeNotify.
        return captor.getAllValues().get(0);
    }

    private OutboxEvent event(String eventType, String aggregateType, Map<String, Object> payload) {
        return OutboxEvent.builder()
                .id(1L)
                .tenantId(tenantId)
                .aggregateType(aggregateType)
                .aggregateId(taskId)
                .eventType(eventType)
                .payloadJson(payload)
                .build();
    }

    private PushToken registeredToken() {
        PushToken entity = new PushToken();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setToken("fcm-token-outbox");
        entity.setPlatform("ANDROID");
        return entity;
    }
}
