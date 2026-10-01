package com.discipolat.modules.whatsapp.domain;

import com.discipolat.common.infrastructure.security.CryptoService;
import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @Mock private WhatsAppConfigRepository configRepository;
    @Mock private WhatsAppMessageRepository messageRepository;
    @Mock private WhatsAppReminderRepository reminderRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private CryptoService cryptoService;
    @Mock private PlatformFeatureFlagService featureFlagService;
    // PORT Develop1 — le portail basse connexion est branchu en optionnel ;
    // un provider non stubbe rend getIfAvailable() == null (portail coupe).
    @Mock private org.springframework.beans.factory.ObjectProvider<
            com.discipolat.modules.lowband.domain.LowBandPortalService> lowBandProvider;

    @Test
    void signedWebhookIsStoredWhenOutboundFeatureIsDisabled() {
        when(configRepository.findByTenantId(any())).thenReturn(Optional.empty());
        when(messageRepository.save(any(WhatsAppMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        WhatsAppService service = new WhatsAppService(configRepository, messageRepository,
                reminderRepository, soulRepository, cryptoService, featureFlagService,
                // PORT Develop1 — le portail basse connexion est optionnel : un
                // ObjectProvider vide laisse le service inactif, comme ici.
                lowBandProvider);
        Map<String, Object> payload = Map.of("entry", List.of(Map.of("changes", List.of(Map.of("value",
                Map.of("messages", List.of(Map.of("from", "+33123456789", "id", "wamid-1",
                        "text", Map.of("body", "hello")))))))));

        assertThatCode(() -> service.handleWebhook(UUID.randomUUID(), payload)).doesNotThrowAnyException();

        verify(messageRepository).save(any(WhatsAppMessage.class));
        verify(featureFlagService, never()).requireEnabled(anyString());
    }
}
