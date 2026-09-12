package com.discipolat.modules.messages.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.voicenotifications.config.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket configuration with tenant isolation.
 * Channels are prefixed with tenant:{tenantId}: to ensure isolation.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable tenant-isolated broker
        // Channels will be prefixed with tenant:{tenantId}: by the auth interceptor
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        // User destinations for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main messaging endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Voice notifications endpoint
        registry.addEndpoint("/ws-church")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint("/ws-church")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}

/**
 * Tenant-aware destination user name provider.
 * Extracts tenant from Principal and includes it in destination resolution.
 */
@Component
class TenantDestinationUserNameProvider implements DestinationUserNameProvider {

    private final SimpUserRegistry userRegistry;

    public TenantDestinationUserNameProvider(SimpUserRegistry userRegistry) {
        this.userRegistry = userRegistry;
    }

    @Override
    public String getDestinationUserName(Principal user) {
        if (user == null) return null;
        String name = user.getName();
        
        // Extract tenant from principal if available
        // The auth interceptor should have set tenant context
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return "tenant:" + tenantId + ":" + name;
        }
        return name;
    }
}