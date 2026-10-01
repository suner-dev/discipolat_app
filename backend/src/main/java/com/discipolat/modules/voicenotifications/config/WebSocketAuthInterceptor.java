package com.discipolat.modules.voicenotifications.config;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepteur de sécurité WebSocket STOMP — valide le token JWT lors du Handshake.
 *
 * <p>Le token JWT peut être transmis de deux façons :</p>
 * <ul>
 *   <li>En header HTTP : {@code Authorization: Bearer <token>}</li>
 *   <li>En paramètre STOMP : {@code CONNECT token:<jwt>}</li>
 * </ul>
 *
 * <p>Une fois validé, l'authentification est stockée dans le contexte de sécurité
 * pour permettre le contrôle d'accès par rôle sur les topics WebSocket.</p>
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null || !jwtTokenProvider.validateToken(token)) {
                // §G5.8 — refus explicite : plus de session STOMP anonyme.
                log.warn("[WebSocket] CONNECT refusé — token JWT absent ou invalide");
                throw new org.springframework.messaging.MessageDeliveryException(
                        message, "Connexion WebSocket refusée : authentification JWT requise");
            }
            try {
                UUID userId = jwtTokenProvider.extractUserId(token);
                List<String> roles = jwtTokenProvider.extractRoles(token);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);

                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Stocker les infos utilisateur dans les headers de session pour accès ultérieur
                accessor.setNativeHeader("userId", userId.toString());
                accessor.setNativeHeader("roles", String.join(",", roles));

                // Extraire et stocker le tenantId pour l'isolation multi-tenant des channels
                UUID tenantId = null;
                try {
                    tenantId = jwtTokenProvider.extractTenantId(token);
                    if (tenantId != null) {
                        accessor.setNativeHeader("tenantId", tenantId.toString());
                        log.debug("[WebSocket] Tenant isolé — tenantId={}", tenantId);
                    }
                } catch (Exception e) {
                    log.warn("[WebSocket] Impossible d'extraire tenantId: {}", e.getMessage());
                }

                // §G5.8 — persister dans les attributs de SESSION : les frames
                // SUBSCRIBE ultérieures ne portent pas ces headers natifs.
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null) {
                    sessionAttributes.put("wsUserId", userId.toString());
                    if (tenantId != null) {
                        sessionAttributes.put("wsTenantId", tenantId.toString());
                    }
                }

                log.debug("[WebSocket] Authentifié — userId={}, roles={}", userId, roles);
            } catch (Exception e) {
                log.warn("[WebSocket] Échec extraction token JWT: {}", e.getMessage());
                throw new org.springframework.messaging.MessageDeliveryException(
                        message, "Connexion WebSocket refusée : token illisible");
            }
        }

        // §G5.8 — autorisation SUBSCRIBE : un client ne peut s'abonner qu'aux
        // canaux de SON tenant (firehose /topic/tenant:{id}/** inclus). Sans
        // cela, n'importe qui devinerait l'UUID d'un autre tenant et lirait
        // sa vie interne (membres, tâches, dress codes…).
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor, message);
        }

        return message;
    }

    /**
     * §G5.8 — Un SUBSCRIBE ne peut cibler que :
     * <ul>
     *   <li>les canaux {@code tenant:{uuid}} / {@code voice/tenant/{uuid}} du
     *       tenant authentifié à la connexion ;</li>
     *   <li>les canaux {@code voice/user/{userId}} de l'utilisateur même ;</li>
     *   <li>les destinations {@code /user/…} (résolues par Spring contre le
     *       principal de LA session — impossibles à spoof).</li>
     * </ul>
     * Toute destination hors de ce périmètre → frame rejetée (MessageDeliveryException).
     */
    private void authorizeSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();
        if (destination == null || destination.startsWith("/user")) {
            return; // /user/** est lié au principal de la session par Spring
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String sessionTenant = sessionAttributes == null ? null : (String) sessionAttributes.get("wsTenantId");
        String sessionUser = sessionAttributes == null ? null : (String) sessionAttributes.get("wsUserId");

        // Tout UUID de tenant présent dans la destination doit être le nôtre.
        java.util.regex.Matcher tenantMatcher =
                java.util.regex.Pattern.compile("tenant[/:]{1}([0-9a-fA-F-]{36})").matcher(destination);
        while (tenantMatcher.find()) {
            if (sessionTenant == null || !sessionTenant.equalsIgnoreCase(tenantMatcher.group(1))) {
                denySubscribe(destination, message);
            }
        }
        // /topic/voice/user/{id} — personnel uniquement.
        if (destination.startsWith("/topic/voice/user/")) {
            String target = destination.substring("/topic/voice/user/".length());
            if (sessionUser == null || !sessionUser.equalsIgnoreCase(target)) {
                denySubscribe(destination, message);
            }
        }
    }

    private void denySubscribe(String destination, Message<?> message) {
        log.warn("[WebSocket] SUBSCRIBE refusé (hors tenant/session) : {}", destination);
        throw new org.springframework.messaging.MessageDeliveryException(
                message, "Subscription refusée : canal hors de votre tenant");
    }

    /**
     * Extrait le token JWT des headers STOMP ou du paramètre de connexion.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Essayer le header Authorization
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Essayer le paramètre STOMP "token"
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader;
        }

        // 3. Essayer les headers SIMP
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            String sessionToken = (String) sessionAttributes.get("token");
            if (sessionToken != null && !sessionToken.isBlank()) {
                return sessionToken;
            }
        }

        return null;
    }
}
