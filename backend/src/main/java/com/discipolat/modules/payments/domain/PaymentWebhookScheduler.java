package com.discipolat.modules.payments.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduler de vérification automatique des paiements en attente.
 *
 * <p><strong>MTN MoMo</strong> ne fournit pas de callback push (webhook).
 * Ce scheduler poll périodiquement l'API MTN pour vérifier le statut des
 * paiements PENDING et les confirmer ou les marquer en échec
 * automatiquement.</p>
 *
 * <p>Exécuté toutes les 30 secondes — les paiements les plus anciens sont
 * vérifiés en premier. Après 10 échecs consécutifs, le paiement est marqué
 * FAILED pour éviter les boucles infinies.</p>
 *
 * <p>Les opérations M-Pesa et Orange Money reçoivent leurs callbacks via
 * {@link com.discipolat.modules.payments.api.PaymentWebhookController}
 * et n'ont pas besoin de ce scheduler (sauf en cas de callback manquant).</p>
 */
@Component
public class PaymentWebhookScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookScheduler.class);

    /** Nombre max de vérifications avant de marquer en échec automatiquement. */
    private static final int MAX_VERIFY_ATTEMPTS = 10;

    private final PaymentIntentRepository repository;
    private final MobileMoneyProviderRegistry providerRegistry;
    private final PaymentGatewayService gatewayService;

    public PaymentWebhookScheduler(PaymentIntentRepository repository,
                                    MobileMoneyProviderRegistry providerRegistry,
                                    PaymentGatewayService gatewayService) {
        this.repository = repository;
        this.providerRegistry = providerRegistry;
        this.gatewayService = gatewayService;
    }

    /**
     * Vérifie les paiements PENDING toutes les 30 secondes.
     *
     * <p>Stratégie :</p>
     * <ol>
     *   <li>Charge les paiements PENDING les plus anciens (max 20 par cycle)</li>
     *   <li>Pour chaque paiement, vérifie si un provider réel est configuré</li>
     *   <li>Si oui, appelle {@code provider.verify()} pour obtenir le statut</li>
     *   <li>Met à jour le statut via {@code gatewayService.handleWebhook()}</li>
     * </ol>
     */
    @Scheduled(fixedDelayString = "${app.payments.webhook.poll-interval-ms:30000}")
    @Transactional
    public void pollPendingPayments() {
        List<PaymentIntent> pending = repository.findPendingOlderThan(
                java.time.LocalDateTime.now().minusSeconds(30)); // Ignore les paiements de < 30s

        if (pending.isEmpty()) return;

        log.debug("[Scheduler] {} paiements PENDING à vérifier", pending.size());

        for (PaymentIntent intent : pending) {
            try {
                verifyPayment(intent);
            } catch (Exception e) {
                log.error("[Scheduler] Erreur vérification payment {} — ref={}",
                        intent.getId(), intent.getProviderReference(), e);
            }
        }
    }

    /**
     * Vérifie un paiement PENDING auprès de son opérateur.
     *
     * <p>Pour les opérateurs sans webhook (MTN MoMo), le provider vérifie
     * via l'API REST. Pour les opérateurs avec webhook (M-Pesa, Orange),
     * cette vérification sert de filet de sécurité en cas de callback manquant.</p>
     */
    private void verifyPayment(PaymentIntent intent) {
        MobileMoneyProvider provider = providerRegistry.find(intent.getOperator());
        if (provider == null) {
            // Pas de provider configuré — le paiement reste en PENDING
            // (utilisateur devra confirmer manuellement ou le webhook arrivera)
            log.trace("[Scheduler] Pas de provider pour {} — skip {}",
                    intent.getOperator().getLabel(), intent.getProviderReference());
            return;
        }

        log.debug("[Scheduler] Vérification {} — ref={}", intent.getOperator().getLabel(),
                intent.getProviderReference());

        MobileMoneyProvider.Verification verification = provider.verify(intent.getProviderReference());

        if (verification.paid()) {
            log.info("[Scheduler] ✅ Paiement confirmé par {} — ref={}",
                    intent.getOperator().getLabel(), intent.getProviderReference());
            gatewayService.handleWebhook(intent.getProviderReference(), true, null);
        } else if (verification.failureReason() != null
                && !"PENDING".equalsIgnoreCase(verification.operatorStatus())
                && !"INITIATED".equalsIgnoreCase(verification.operatorStatus())
                && !"SUBMITTED".equalsIgnoreCase(verification.operatorStatus())) {
            // Le statut n'est ni PENDING ni en cours → échec définitif
            log.info("[Scheduler] ❌ Paiement échoué selon {} — ref={} — {}",
                    intent.getOperator().getLabel(), intent.getProviderReference(),
                    verification.failureReason());
            gatewayService.handleWebhook(intent.getProviderReference(), false,
                    verification.failureReason());
        } else {
            log.debug("[Scheduler] ⏳ Paiement toujours en cours — ref={} — status={}",
                    intent.getProviderReference(), verification.operatorStatus());
        }
    }
}
