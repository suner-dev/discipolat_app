package com.discipolat.modules.payments.domain;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Résout le provider opérateur adapté à un {@link PaymentIntent.Operator}.
 * Spring injecte automatiquement toutes les implémentations de
 * {@link MobileMoneyProvider} (SPI) ; le registre ne retient que celles
 * réellement activées (credentials présents).
 */
@Component
public class MobileMoneyProviderRegistry {

    private final Map<PaymentIntent.Operator, MobileMoneyProvider> active;

    public MobileMoneyProviderRegistry(List<MobileMoneyProvider> providers) {
        Map<PaymentIntent.Operator, MobileMoneyProvider> map = new EnumMap<>(PaymentIntent.Operator.class);
        for (MobileMoneyProvider p : providers) {
            if (p != null && p.isEnabled()) {
                map.put(p.operator(), p);
            }
        }
        this.active = Map.copyOf(map);
    }

    /** Le provider activé pour cet opérateur, ou {@code null} si aucun. */
    public MobileMoneyProvider find(PaymentIntent.Operator operator) {
        return active.get(operator);
    }

    /** Nombre de providers opérateurs réellement activés. */
    public int activeCount() {
        return active.size();
    }
}
