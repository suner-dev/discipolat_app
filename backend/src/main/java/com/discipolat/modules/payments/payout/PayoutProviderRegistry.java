package com.discipolat.modules.payments.payout;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A3 (M8) — Registre des fournisseurs de décaissement réellement actifs.
 *
 * <p>Même philosophie que {@code MobileMoneyProviderRegistry} (précédent
 * interne) : Spring injecte toutes les implémentations de l'SPI, le registre
 * ne retient que celles dont {@code isEnabled()} est vrai. La sélection se
 * fait par clé de configuration ({@code stripe}, {@code paypal},
 * {@code sepa}, {@code bank}, {@code mtn}, {@code orange}, {@code mpesa}).</p>
 */
@Component
public class PayoutProviderRegistry {

    private final Map<String, PayoutProvider> all;
    private final Map<String, PayoutProvider> active;

    public PayoutProviderRegistry(List<PayoutProvider> providers) {
        Map<String, PayoutProvider> known = new LinkedHashMap<>();
        Map<String, PayoutProvider> enabled = new LinkedHashMap<>();
        for (PayoutProvider provider : providers) {
            if (provider == null || known.containsKey(provider.key())) {
                continue;   // une clé définie en double = faute de câblage, on garde la première
            }
            known.put(provider.key(), provider);
            if (provider.isEnabled()) {
                enabled.put(provider.key(), provider);
            }
        }
        this.all = Map.copyOf(known);
        this.active = Map.copyOf(enabled);
    }

    /** Le provider actif pour cette clé, s'il existe et est configuré. */
    public Optional<PayoutProvider> findActive(String key) {
        return Optional.ofNullable(key == null ? null : active.get(key.toLowerCase()));
    }

    /** Nombre de fournisseurs de décaissement réellement exploitables. */
    public int activeCount() {
        return active.size();
    }

    /** État honnête, pour le diagnostic : {key → {enabled, configured, reason}}. */
    public Map<String, Map<String, Object>> status() {
        Map<String, Map<String, Object>> status = new LinkedHashMap<>();
        all.forEach((key, provider) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            boolean configured = provider.isEnabled();
            entry.put("configured", configured);
            if (!configured) {
                entry.put("reason", provider.disabledReason());
            }
            status.put(key, entry);
        });
        return status;
    }
}
