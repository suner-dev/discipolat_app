package com.discipolat.modules.voicenotifications.config;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * §G5.8 — Sécurité du canal temps réel :
 * CONNECT sans JWT valide = refus (plus de session anonyme) ;
 * SUBSCRIBE hors du tenant authentifié = refus ; canaux personnels et
 * /user/** = autorisés. Sans cela, deviner un UUID de tenant suffirait à
 * écouter la vie interne d'une autre église.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private MessageChannel channel;

    private WebSocketAuthInterceptor interceptor;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtTokenProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Message<byte[]> stompMessage(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private StompHeaderAccessor connectFrame(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        // Comme dans le vrai pipeline STOMP : les headers restent mutables
        // le temps que l'interceptor y écrive principal/session.
        accessor.setLeaveMutable(true);
        accessor.setSessionAttributes(new HashMap<>());
        if (token != null) {
            accessor.setNativeHeader("token", token);
        }
        return accessor;
    }

    private StompHeaderAccessor subscribeFrame(String destination, boolean withSession) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        if (withSession) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("wsTenantId", TENANT.toString());
            attrs.put("wsUserId", USER.toString());
            accessor.setSessionAttributes(attrs);
        }
        return accessor;
    }

    // ===== CONNECT =====

    @Test
    void connectWithoutTokenIsRejected() {
        Message<byte[]> msg = stompMessage(connectFrame(null));
        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msg, channel));
        assertTrue(ex.getMessage().contains("authentification JWT requise"));
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);
        Message<byte[]> msg = stompMessage(connectFrame("bad"));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msg, channel));
    }

    @Test
    void connectWithValidTokenAuthenticatesAndPersistsSession() {
        when(jwtTokenProvider.validateToken("good")).thenReturn(true);
        when(jwtTokenProvider.extractUserId("good")).thenReturn(USER);
        when(jwtTokenProvider.extractRoles("good")).thenReturn(List.of("MEMBRE"));
        when(jwtTokenProvider.extractTenantId("good")).thenReturn(TENANT);

        StompHeaderAccessor accessor = connectFrame("good");
        Message<byte[]> msg = stompMessage(accessor);
        assertDoesNotThrow(() -> interceptor.preSend(msg, channel));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "le principal STOMP doit être résolu pour /user/**");
        Map<String, Object> attrs = accessor.getSessionAttributes();
        assertNotNull(attrs);
        assertEquals(USER.toString(), attrs.get("wsUserId"));
        assertEquals(TENANT.toString(), attrs.get("wsTenantId"));
    }

    // ===== SUBSCRIBE =====

    @Test
    void subscribeToOwnTenantFirehoseIsAllowed() {
        Message<byte[]> msg = stompMessage(
                subscribeFrame("/topic/tenant:" + TENANT + "/events", true));
        assertDoesNotThrow(() -> interceptor.preSend(msg, channel));
    }

    @Test
    void subscribeToAnotherTenantFirehoseIsRejected() {
        Message<byte[]> msg = stompMessage(
                subscribeFrame("/topic/tenant:" + OTHER_TENANT + "/events", true));
        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msg, channel));
        assertTrue(ex.getMessage().contains("hors de votre tenant"));
    }

    @Test
    void subscribeVoiceChannelWithTenantPathIsScoped() {
        Message<byte[]> own = stompMessage(
                subscribeFrame("/topic/voice/tenant/" + TENANT, true));
        assertDoesNotThrow(() -> interceptor.preSend(own, channel));

        Message<byte[]> foreign = stompMessage(
                subscribeFrame("/topic/voice/tenant/" + OTHER_TENANT, true));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(foreign, channel));
    }

    @Test
    void subscribeToSomeoneElsesPersonalChannelIsRejected() {
        Message<byte[]> msg = stompMessage(
                subscribeFrame("/topic/voice/user/" + UUID.randomUUID(), true));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msg, channel));
    }

    @Test
    void subscribeOwnPersonalChannelIsAllowed() {
        Message<byte[]> msg = stompMessage(
                subscribeFrame("/topic/voice/user/" + USER, true));
        assertDoesNotThrow(() -> interceptor.preSend(msg, channel));
    }

    @Test
    void userQueueDestinationsAreHandledBySpringPrincipal() {
        Message<byte[]> msg = stompMessage(subscribeFrame("/user/queue/notifications", true));
        assertDoesNotThrow(() -> interceptor.preSend(msg, channel));
    }

    @Test
    void subscribeWithoutAuthenticatedSessionCannotReadTenantChannels() {
        Message<byte[]> msg = stompMessage(
                subscribeFrame("/topic/tenant:" + TENANT + "/events", false));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msg, channel));
    }
}
