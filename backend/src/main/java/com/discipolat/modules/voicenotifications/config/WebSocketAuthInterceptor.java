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

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtTokenProvider.validateToken(token)) {
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
                    try {
                        UUID tenantId = jwtTokenProvider.extractTenantId(token);
                        if (tenantId != null) {
                            accessor.setNativeHeader("tenantId", tenantId.toString());
                            log.debug("[WebSocket] Tenant isolé — tenantId={}", tenantId);
                        }
                    } catch (Exception e) {
                        log.warn("[WebSocket] Impossible d'extraire tenantId: {}", e.getMessage());
                    }

                    log.debug("[WebSocket] Authentifié — userId={}, roles={}", userId, roles);
                } catch (Exception e) {
                    log.warn("[WebSocket] Échec extraction token JWT: {}", e.getMessage());
                }
            } else {
                log.warn("[WebSocket] Token JWT invalide ou absent");
            }
        }

        return message;
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
