package com.discipolat.modules.lowband;

import com.discipolat.modules.core.domain.OutboxEvent;
import com.discipolat.modules.lowband.domain.LowBandNotifyService;
import com.discipolat.modules.lowband.domain.LowBandPortalService;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.repository.SpaceMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppMessage;
import com.discipolat.modules.whatsapp.domain.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * §G5.9 — WhatsApp SORTANT : le toggle tenant et l'opt-in individuel membre
 * sont respectés ; un échec d'envoi ne casse jamais la consommation outbox.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LowBandNotifyServiceTest {

    @Mock private ObjectProvider<WhatsAppService> whatsAppProvider;
    @Mock private WhatsAppService whatsAppService;
    @Mock private LowBandPortalService portalService;
    @Mock private UserRepository userRepository;
    @Mock private PersonRepository personRepository;
    @Mock private SpaceMembershipRepository spaceMembershipRepository;

    private LowBandNotifyService service;

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @BeforeEach
    void setUp() {
        when(whatsAppProvider.getIfAvailable()).thenReturn(whatsAppService);
        service = new LowBandNotifyService(whatsAppProvider, portalService, userRepository,
                personRepository, spaceMembershipRepository);
    }

    private void toggle(boolean enabled) {
        when(portalService.isLowBandEnabled(TENANT)).thenReturn(enabled);
    }

    private User optedInUser(UUID id, String email, String phone) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPhone(phone);
        u.setWhatsappOptIn(true);
        u.setTenantId(TENANT);
        return u;
    }

    private OutboxEvent event(String type, Map<String, Object> payload) {
        OutboxEvent e = new OutboxEvent();
        e.setTenantId(TENANT);
        e.setEventType(type);
        e.setPayloadJson(payload);
        return e;
    }

    @Test
    void tenantToggleDisabled_sendsNothing() {
        toggle(false);
        service.consume(event("DressCodePublished", Map.of("title", "Culte dimanche")));
        verifyNoInteractions(whatsAppService);
        verify(userRepository, never()).findByTenantIdAndWhatsappOptInTrue(any());
    }

    @Test
    void dressCodeBroadcast_onlyReachesOptedInMembers() {
        toggle(true);
        User in = optedInUser(UUID.randomUUID(), "a@x.y", "+2411111111");
        when(userRepository.findByTenantIdAndWhatsappOptInTrue(TENANT)).thenReturn(List.of(in));

        service.consume(event("DressCodePublished", Map.of("title", "Culte des familles")));

        verify(whatsAppService).sendText(eq(TENANT), eq("+2411111111"),
                argThat(s -> s.contains("Culte des familles") && s.contains("#tenue")),
                isNull(), isNull(), eq(WhatsAppMessage.Kind.NOTIFICATION));
        verify(portalService).journal(eq(TENANT), eq("WHATSAPP"), eq("broadcast"),
                eq("OUTBOUND"), anyString());
    }

    @Test
    void optedOutUserIsNeverSentTo() {
        toggle(true);
        User out = optedInUser(UUID.randomUUID(), "b@x.y", "+2412222222");
        out.setWhatsappOptIn(false);
        String assigneeId = out.getId().toString();
        when(userRepository.findById(out.getId())).thenReturn(Optional.of(out));

        service.consume(event("TaskAssigned", Map.of("assigneeId", assigneeId)));

        verifyNoInteractions(whatsAppService);
    }

    @Test
    void taskAssignedSendsToOptedInAssigneeWithPhone() {
        toggle(true);
        User u = optedInUser(UUID.randomUUID(), "c@x.y", "+2413333333");
        when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));

        service.consume(event("TaskAssigned", Map.of("assigneeId", u.getId().toString())));

        verify(whatsAppService).sendText(eq(TENANT), eq("+2413333333"), anyString(),
                isNull(), isNull(), eq(WhatsAppMessage.Kind.NOTIFICATION));
    }

    @Test
    void sendFailureNeverBreaksOutboxConsumption() {
        toggle(true);
        User u = optedInUser(UUID.randomUUID(), "d@x.y", "+2414444444");
        when(userRepository.findByTenantIdAndWhatsappOptInTrue(TENANT)).thenReturn(List.of(u));
        doThrow(new RuntimeException("pont Meta indisponible"))
                .when(whatsAppService).sendText(any(), anyString(), anyString(), any(), any(), any());

        // Ne doit JAMAIS propager l'exception (best-effort absolu).
        service.consume(event("DressCodePublished", Map.of("title", "X")));
    }

    @Test
    void unknownEventTypeIgnored() {
        toggle(true);
        service.consume(event("SomethingElse", Map.of()));
        verifyNoInteractions(whatsAppService);
    }
}
