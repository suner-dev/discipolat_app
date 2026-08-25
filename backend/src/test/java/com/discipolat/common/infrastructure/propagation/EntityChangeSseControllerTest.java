package com.discipolat.common.infrastructure.propagation;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EntityChangeSseController}.
 */
@ExtendWith(MockitoExtension.class)
class EntityChangeSseControllerTest {

    @Mock private EntityChangeBroadcaster broadcaster;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private EntityChangeSseController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void subscribe_registersEmitter() {
        TenantContext.setTenantId(TENANT_ID);
        SecurityTestHelper.loginAs(USER_ID);

        SseEmitter emitter = controller.subscribe(null);

        assertThat(emitter).isNotNull();
        verify(broadcaster).register(eq(TENANT_ID), any(SseEmitter.class), isNull());
    }

    @Test
    void subscribe_withEntityTypeFilter() {
        TenantContext.setTenantId(TENANT_ID);
        SecurityTestHelper.loginAs(USER_ID);

        SseEmitter emitter = controller.subscribe("SOUL,USER");

        assertThat(emitter).isNotNull();
        verify(broadcaster).register(eq(TENANT_ID), any(SseEmitter.class), eq(Set.of("SOUL", "USER")));
    }

    @Test
    void stats_returnsConnectionInfo() {
        when(broadcaster.getTotalConnectedCount()).thenReturn(5);
        when(broadcaster.getConnectedTenantIds()).thenReturn(Set.of(TENANT_ID));

        Map<String, Object> stats = controller.stats();

        assertThat(stats).containsEntry("totalConnectedClients", 5);
        assertThat(stats).containsEntry("connectedTenants", 1);
    }
}
