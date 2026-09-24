package com.discipolat.modules.webhooks.domain;

import com.discipolat.common.infrastructure.propagation.EntityChangedEvent;
import com.discipolat.common.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookEntityEventListenerTest {

    @Mock
    private WebhookService webhookService;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void executesWebhookInTheEventTenantContext() {
        UUID tenantId = UUID.randomUUID();
        UUID previousTenantId = UUID.randomUUID();
        TenantContext.setTenantId(previousTenantId);
        when(webhookService.fire(anyString(), any())).thenAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
            return 1;
        });
        WebhookEntityEventListener listener = new WebhookEntityEventListener(webhookService);
        EntityChangedEvent event = new EntityChangedEvent(
                this, "FAMILY", UUID.randomUUID(), EntityChangedEvent.ChangeType.UPDATED,
                Map.of(), Map.of("name", "Famille Test"), null, "update", tenantId);

        listener.onEntityChanged(event);

        verify(webhookService).fire(anyString(), any());
        assertThat(TenantContext.getTenantId()).isEqualTo(previousTenantId);
    }
}
