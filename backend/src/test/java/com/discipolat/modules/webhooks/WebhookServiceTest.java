package com.discipolat.modules.webhooks;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.webhooks.domain.ApiKey;
import com.discipolat.modules.webhooks.domain.ApiKeyRepository;
import com.discipolat.modules.webhooks.domain.WebhookDeliveryLog;
import com.discipolat.modules.webhooks.domain.WebhookRegistration;
import com.discipolat.modules.webhooks.domain.WebhookRegistrationRepository;
import com.discipolat.modules.webhooks.domain.WebhookDeliveryLogRepository;
import com.discipolat.modules.webhooks.domain.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private WebhookRegistrationRepository webhookRepository;
    @Mock private WebhookDeliveryLogRepository logRepository;
    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private WebhookService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WebhookService(webhookRepository, logRepository, apiKeyRepository,
                propagationPublisher, securityUtils);
        lenient().when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        lenient().when(logRepository.save(any(WebhookDeliveryLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void hmacSignature_deterministeEtSensibleAuSecret() {
        String s1 = WebhookService.hmacSha256("payload", "secret1");
        String s1bis = WebhookService.hmacSha256("payload", "secret1");
        String s2 = WebhookService.hmacSha256("payload", "secret2");
        assertThat(s1).isEqualTo(s1bis);
        assertThat(s1).isNotEqualTo(s2);
        assertThat(s1).hasSize(64); // SHA-256 hexadécimal
    }

    @Test
    void fire_neNotifieQueLesWebhooksAbonnes() {
        WebhookRegistration tous = WebhookRegistration.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).name("Tous")
                .url("http://localhost:9/x").secret("s1").events("*").active(true).build();
        WebhookRegistration specifique = WebhookRegistration.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).name("Souls only")
                .url("http://localhost:9/y").secret("s2").events("SOUL_UPDATED").active(true).build();
        when(webhookRepository.findAll()).thenReturn(List.of(tous, specifique));

        int delivered = service.fire("SOUL_CREATED", Map.of("nom", "Jean"));

        assertThat(delivered).isEqualTo(1);
        org.mockito.Mockito.verify(logRepository).save(org.mockito.ArgumentMatchers.argThat(
                l -> !l.isSuccess() && l.getWebhookId().equals(tous.getId())));
    }

    @Test
    void listensTo_respecteLaListeDEvenements() {
        WebhookRegistration reg = WebhookRegistration.builder().events("SOUL_CREATED, FINANCE_TRANSACTION").build();
        assertThat(reg.listensTo("SOUL_CREATED")).isTrue();
        assertThat(reg.listensTo("finance_transaction")).isTrue(); // insensible à la casse
        assertThat(reg.listensTo("OTHER_EVENT")).isFalse();
    }

    @Test
    void createApiKey_hashStocke_etCleBruteRetourneeUneSeuleFois() {
        ApiKey saved = ApiKey.builder()
                .id(UUID.randomUUID()).name("Zapier").prefix("dk_ABCDEFGHI").keyHash("hash")
                .scopes("read").build();
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.createApiKey("Zapier", "read");

        assertThat(result).containsKeys("key", "warning");
        assertThat((String) result.get("key")).startsWith("dk_");
        // L'entité réellement persistée contient le hash de la clé brute
        org.mockito.ArgumentCaptor<ApiKey> captor = org.mockito.ArgumentCaptor.forClass(ApiKey.class);
        org.mockito.Mockito.verify(apiKeyRepository).save(captor.capture());
        ApiKey persisted = captor.getValue();
        assertThat(persisted.getKeyHash()).isNotEqualTo(result.get("key"));
        assertThat(persisted.getKeyHash()).hasSize(64);
        assertThat(persisted.getPrefix()).isEqualTo(((String) result.get("key")).substring(0, 11));
    }
}
