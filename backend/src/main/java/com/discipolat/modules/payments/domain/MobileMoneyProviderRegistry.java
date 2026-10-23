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
 *
 * <p><strong>Priorité à l'aggrégateur.</strong> Un provider qui se déclare
 * {@link MobileMoneyProvider#aggregator()} (ex. CinetPay, un seul contrat couvrant
 * Orange Money + MTN MoMo + Wave + carte) prend le pas sur l'API opérateur directe
 * pour chaque opérateur qu'il sert. Les providers directs restent le repli dès que
 * l'aggrégateur n'est pas configuré — donc aucun changement de comportement tant
 * que les clés CinetPay ne sont pas posées (fail-closed, aucune régression).
 */
@Component
public class MobileMoneyProviderRegistry {

    private final Map<PaymentIntent.Operator, MobileMoneyProvider> active;

    public MobileMoneyProviderRegistry(List<MobileMoneyProvider> providers) {
        Map<PaymentIntent.Operator, MobileMoneyProvider> map = new EnumMap<>(PaymentIntent.Operator.class);
        for (MobileMoneyProvider p : providers) {
            if (p == null || !p.isEnabled()) {
                continue;
            }
            for (PaymentIntent.Operator operator : p.supportedOperators()) {
                MobileMoneyProvider incumbent = map.get(operator);
                // On ne conserve l'incumbent QUE s'il est aggrégateur et que le
                // nouvel arrivant est direct : un provider direct ne doit jamais
                // évincer l'aggrégateur. Sinon on écrase — ce qui préserve le
                // « dernier gagne » entre deux providers de même classe.
                boolean remplacer = incumbent == null || p.aggregator() || !incumbent.aggregator();
                if (remplacer) {
                    map.put(operator, p);
                }
            }
        }
        this.active = Map.copyOf(map);
    }

    /** Le provider activé pour cet opérateur, ou {@code null} si aucun. */
    public MobileMoneyProvider find(PaymentIntent.Operator operator) {
        return active.get(operator);
    }

    /** Nombre d'opérateurs réellement servis (aggrégateur ou direct). */
    public int activeCount() {
        return active.size();
    }
}
