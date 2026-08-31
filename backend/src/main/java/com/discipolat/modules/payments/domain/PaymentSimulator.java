package com.discipolat.modules.payments.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simulateur de passerelle Mobile Money (dîmes & offrandes 2.0).
 *
 * En mode simulation ({@code app.payments.simulate-auto-confirm=true}), il
 * confirme AUTOMATIQUEMENT les intentions de paiement restées PENDING après
 * un court délai, sans passerelle opérateur réelle. Cela rend le module
 * entièrement fonctionnel de bout en bout (don → confirmation → reçu →
 * comptabilisation Finances → propagation SSE) pour tous les rôles, y compris
 * en production si l'option est activée volontairement.
 *
 * En mode réel ({@code simulate-auto-confirm=false}, défaut), ce simulateur
 * ne fait rien : les paiements ne sont confirmés que par le webhook opérateur.
 */
@Service
public class PaymentSimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentSimulator.class);

    private final PaymentGatewayService paymentGatewayService;
    private final PaymentIntentRepository repository;

    @Value("${app.payments.simulate-auto-confirm:false}")
    private boolean simulateAutoConfirm;

    @Value("${app.payments.simulate-confirm-delay-ms:6000}")
    private long confirmDelayMs;

    public PaymentSimulator(PaymentGatewayService paymentGatewayService,
                            PaymentIntentRepository repository) {
        this.paymentGatewayService = paymentGatewayService;
        this.repository = repository;
    }

    /**
     * Balayage périodique : confirme toute intention PENDING ayant dépassé le
     * délai de simulation. Idempotent (le webhook ignore déjà les confirmés).
     */
    @Scheduled(fixedDelayString = "${app.payments.simulate-scan-interval-ms:5000}")
    public void sweepPending() {
        if (!simulateAutoConfirm) {
            return;
        }
        LocalDateTime threshold = LocalDateTime.now().minusNanos(confirmDelayMs * 1_000_000L);
        List<PaymentIntent> pending;
        try {
            pending = repository.findPendingOlderThan(threshold);
        } catch (Exception e) {
            // Démarrage (tables pas encore prêtes) ou tenant indisponible : rien à faire.
            log.debug("PaymentSimulator sweep skipped: {}", e.getMessage());
            return;
        }
        for (PaymentIntent intent : pending) {
            try {
                paymentGatewayService.handleWebhook(intent.getProviderReference(), true, null);
                log.info("PaymentSimulator auto-confirmed {} ({})", intent.getProviderReference(),
                        intent.getOperator().getLabel());
            } catch (Exception e) {
                log.warn("PaymentSimulator failed to confirm {}: {}", intent.getProviderReference(),
                        e.getMessage());
            }
        }
    }
}
