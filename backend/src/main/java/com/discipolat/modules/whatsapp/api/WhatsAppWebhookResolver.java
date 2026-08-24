package com.discipolat.modules.whatsapp.api;

import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.whatsapp.domain.WhatsAppConfig;
import com.discipolat.modules.whatsapp.domain.WhatsAppConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Résout le tenant émetteur d'un événement webhook WhatsApp :
 * - GET verify : par webhook_verify_token
 * - POST events : par phone_number_id présent dans entry[].changes[].value.metadata
 */
@Service
public class WhatsAppWebhookResolver {

    private final WhatsAppConfigRepository configRepository;
    private final TenantRepository tenantRepository;

    public WhatsAppWebhookResolver(WhatsAppConfigRepository configRepository,
                                   TenantRepository tenantRepository) {
        this.configRepository = configRepository;
        this.tenantRepository = tenantRepository;
    }

    public Optional<UUID> resolveTenantByVerifyToken(String verifyToken) {
        return allConfigs().stream()
                .filter(c -> verifyToken != null && verifyToken.equals(c.getWebhookVerifyToken()))
                .map(WhatsAppConfig::getTenantId)
                .findFirst();
    }

    /** Extrait metadata.phone_number_id du payload Meta et retrouve le tenant. */
    @SuppressWarnings("unchecked")
    public Optional<UUID> resolveTenantFromPayload(Map<String, Object> payload) {
        Object entryObj = payload.get("entry");
        if (!(entryObj instanceof List<?> entries) || entries.isEmpty()) return Optional.empty();
        for (Object e : entries) {
            if (!(e instanceof Map<?, ?> entry)) continue;
            Object changesObj = ((Map<?, ?>) entry).get("changes");
            if (!(changesObj instanceof List<?> changes)) continue;
            for (Object c : changes) {
                if (!(c instanceof Map<?, ?> change)) continue;
                Object valueObj = ((Map<?, ?>) change).get("value");
                if (!(valueObj instanceof Map<?, ?> value)) continue;
                Object metadataObj = ((Map<?, ?>) value).get("metadata");
                if (!(metadataObj instanceof Map<?, ?> metadata)) continue;
                Object phoneNumberIdObj = ((Map<?, ?>) metadata).get("phone_number_id");
                if (phoneNumberIdObj == null) continue;
                String phoneNumberId = String.valueOf(phoneNumberIdObj);
                return allConfigs().stream()
                        .filter(cfg -> phoneNumberId.equals(cfg.getPhoneNumberId()))
                        .map(WhatsAppConfig::getTenantId)
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private List<WhatsAppConfig> allConfigs() {
        // Les repositories tenant-aware filtrent par contexte ; pour la résolution
        // on balaie tous les tenants via un accès système.
        return configRepository.findAll();
    }
}
