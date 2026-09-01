package com.discipolat.modules.payments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.finances.api.FinanceTransactionRequest;
import com.discipolat.modules.finances.domain.FinanceService;
import com.discipolat.modules.finances.domain.FinanceTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Passerelle Mobile Money — Tithe & Offering 2.0.
 *
 * Parcours : scan QR ou bouton « Offrande » → intention de paiement →
 * opérateur (M-Pesa, MTN MoMo, Orange Money…) → webhook de confirmation →
 * reçu automatique + comptabilisation dans le module Finances.
 *
 * NOTE PRODUCTION : les adaptateurs opérateurs réels (APIs MTN/MoMo/Orange)
 * se branchent dans {@link #initiate} et {@link #handleWebhook}. Le flux
 * actuel simule la passerelle pour permettre le fonctionnement hors sandbox.
 */
@Service
@Transactional
public class PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayService.class);

    private final PaymentIntentRepository repository;
    private final FinanceService financeService;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;
    private final MobileMoneyProviderRegistry providerRegistry;

    public PaymentGatewayService(PaymentIntentRepository repository,
                                 FinanceService financeService,
                                 EntityPropagationPublisher propagationPublisher,
                                 SecurityUtils securityUtils,
                                 MobileMoneyProviderRegistry providerRegistry) {
        this.repository = repository;
        this.financeService = financeService;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
        this.providerRegistry = providerRegistry;
    }

    /**
     * Démarre une intention de paiement et retourne la référence opérateur.
     *
     * <p>Si un provider opérateur réel est configuré pour cet opérateur
     * (credentials présents), l'API opérateur est appelée via
     * {@link MobileMoneyProvider#initiate} et la référence, l'URL de checkout
     * et les instructions sont stockées sur l'intention. Sinon, une référence
     * locale est générée (fallback dev) et la confirmation passera par le
     * webhook opérateur signé.</p>
     */
    public PaymentIntent initiate(PaymentIntent intent) {
        intent.setTenantId(securityUtils.getCurrentTenantId());
        if (intent.getUserId() == null) {
            try {
                intent.setUserId(securityUtils.getCurrentUserId());
            } catch (Exception e) {
                // don anonyme sans compte
            }
        }
        if (intent.getStatus() == null) {
            intent.setStatus(PaymentIntent.Status.PENDING);
        }
        intent.setProviderReference(generateReference(intent));

        // Appel API opérateur réel si un provider est configuré
        MobileMoneyProvider provider = providerRegistry.find(intent.getOperator());
        if (provider != null) {
            try {
                MobileMoneyProvider.Result result = provider.initiate(intent);
                intent.setProviderReference(result.providerReference());
                intent.setProviderName(intent.getOperator().getLabel());
                intent.setCheckoutUrl(result.checkoutUrl());
                intent.setInstructions(result.instructions());
                log.info("[Gateway] Provider réel {} invoqué pour {} — ref={}",
                        intent.getOperator().getLabel(), intent.getId(), result.providerReference());
            } catch (Exception e) {
                log.error("[Gateway] Provider {} échoué, fallback référence locale",
                        intent.getOperator().getLabel(), e);
                // Le intent conserve la référence locale générée, pas de throw
            }
        } else {
            log.debug("[Gateway] Aucun provider actif pour {} — référence locale",
                    intent.getOperator().getLabel());
        }

        PaymentIntent saved = repository.save(intent);
        propagationPublisher.publishCreated("PAYMENT_INTENT", saved.getId(),
                Map.of("operator", saved.getOperator().name(),
                        "amount", saved.getAmount(),
                        "status", saved.getStatus().name()),
                "Intention de paiement " + saved.getOperator() + " " + saved.getAmount() + " " + saved.getCurrency());
        return saved;
    }

    /**
     * Webhook opérateur : confirme ou échoue le paiement.
     * À la confirmation → création du reçu financier (module Finances).
     */
    public PaymentIntent handleWebhook(String providerReference, boolean success, String reason) {
        PaymentIntent intent = repository.findByProviderReference(providerReference)
                .orElseThrow(() -> new EntityNotFoundException("PaymentIntent", "providerReference", providerReference));

        if (intent.getStatus() == PaymentIntent.Status.CONFIRMED) {
            return intent; // idempotent
        }

        if (success) {
            intent.setStatus(PaymentIntent.Status.CONFIRMED);
            intent.setConfirmedAt(LocalDateTime.now());
            Map<String, Object> tx = createFinanceReceipt(intent);
            Object txId = tx.get("id");
            if (txId instanceof UUID uuid) {
                intent.setTransactionId(uuid);
            }
        } else {
            intent.setStatus(PaymentIntent.Status.FAILED);
            intent.setFailureReason(reason != null ? reason : "Rejet opérateur");
        }
        PaymentIntent saved = repository.save(intent);
        propagationPublisher.publishStatusChanged("PAYMENT_INTENT", saved.getId(),
                PaymentIntent.Status.PENDING.name(), saved.getStatus().name(),
                "Paiement " + (success ? "confirmé" : "échoué") + ": " + saved.getAmount() + " " + saved.getCurrency());
        return saved;
    }

    /** Annulation par l'utilisateur avant confirmation (son propre paiement uniquement). */
    public PaymentIntent cancel(UUID id) {
        PaymentIntent intent = findByIdForCurrentUser(id);
        if (intent.getStatus() == PaymentIntent.Status.PENDING) {
            intent.setStatus(PaymentIntent.Status.CANCELLED);
            repository.save(intent);
        }
        return intent;
    }

    @Transactional(readOnly = true)
    public List<PaymentIntent> recent() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    /** Paiements de l'utilisateur courant — vue « Mes dons » (tous rôles). */
    @Transactional(readOnly = true)
    public List<PaymentIntent> mine() {
        return repository.findTop50ByUserIdOrderByCreatedAtDesc(securityUtils.getCurrentUserId());
    }

    /**
     * Accès à UN paiement : uniquement son auteur ou un super-utilisateur
     * (anti-IDOR — une 404 est renvoyée sinon, sans fuite d'existence).
     */
    @Transactional(readOnly = true)
    public PaymentIntent findByIdForCurrentUser(UUID id) {
        PaymentIntent intent = findById(id);
        if (!securityUtils.isSuperUser()) {
            UUID currentUserId = securityUtils.getCurrentUserId();
            if (intent.getUserId() == null || !intent.getUserId().equals(currentUserId)) {
                throw new EntityNotFoundException("PaymentIntent", id);
            }
        }
        return intent;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending", repository.countByStatus(PaymentIntent.Status.PENDING));
        stats.put("confirmed", repository.countByStatus(PaymentIntent.Status.CONFIRMED));
        stats.put("failed", repository.countByStatus(PaymentIntent.Status.FAILED));

        List<Map<String, Object>> byOperator = new ArrayList<>();
        for (Object[] row : repository.sumConfirmedByOperator()) {
            PaymentIntent.Operator op = (PaymentIntent.Operator) row[0];
            byOperator.add(Map.of(
                    "operator", op.name(),
                    "label", op.getLabel(),
                    "total", row[1],
                    "count", row[2]));
        }
        stats.put("byOperator", byOperator);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : repository.monthlyTrend()) {
            trend.add(Map.of("month", row[0], "total", row[1]));
        }
        stats.put("monthlyTrend", trend);
        return stats;
    }

    @Transactional(readOnly = true)
    public PaymentIntent findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PaymentIntent", id));
    }

    /* ------------------------------ Internes ------------------------------ */

    /** Reçu automatique : écriture comptable côté module Finances. */
    private Map<String, Object> createFinanceReceipt(PaymentIntent intent) {
        FinanceTransaction.TransactionType type = intent.getPurpose() == PaymentIntent.Purpose.DIME
                ? FinanceTransaction.TransactionType.valueOf("RECETTE")
                : FinanceTransaction.TransactionType.valueOf("RECETTE");
        String categorie = switch (intent.getPurpose()) {
            case DIME -> "DIME";
            case OFFRANDE -> "OFFRANDE";
            case PROMESSE -> "PROMESSE";
            case PROJET_SPECIAL -> "PROJET_SPECIAL";
            case DON_DIASPORA -> "DON_DIASPORA";
        };
        // Pas de try/catch ici : toute erreur de comptabilisation doit faire
        // échouer le webhook (le paiement reste PENDING, l'opérateur renverra
        // sa notification — handleWebhook est idempotent). Avaler l'exception
        // marquerait la transaction « rollback-only » sans lever d'erreur.
        return financeService.createTransaction(new FinanceTransactionRequest(
                type, categorie, intent.getAmount(),
                "Paiement " + intent.getOperator().getLabel()
                        + (intent.getPurpose() == PaymentIntent.Purpose.DIME ? " — dîme" : " — offrande")
                        + " [" + intent.getProviderReference() + "]",
                java.time.LocalDate.now()));
    }

    /** Référence unique type « MP-240822-XXXX ». En production : renvoyée par l'API opérateur. */
    private String generateReference(PaymentIntent intent) {
        String prefix = switch (intent.getOperator()) {
            case M_PESA -> "MP";
            case MTN_MOMO -> "MTN";
            case ORANGE_MONEY -> "OM";
            case AIRTEL_MONEY -> "AM";
            case WAVE -> "WV";
            case CARD -> "CB";
            case CASH -> "CSH";
        };
        String date = java.time.format.DateTimeFormatter.ofPattern("yyMMdd")
                .format(java.time.LocalDate.now());
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        return "%s-%s-%s".formatted(prefix, date, random);
    }
}
