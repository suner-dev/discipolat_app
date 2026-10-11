package com.discipolat.modules.payments.stripe;

import com.discipolat.modules.payments.domain.WebhookLog;
import com.discipolat.modules.payments.domain.WebhookLogService;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.tenants.enums.SubscriptionStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Traite les événements webhook Stripe et maintient {@code tenant_subscriptions}
 * alignée sur la réalité du billing Stripe (activation, périodes, dunning,
 * résiliation). Idempotent grâce à {@code stripe_webhook_events}.
 *
 * <p>Dunning : {@code invoice.payment_failed} place l'abonnement en PAST_DUE
 * (Stripe réessaie selon la politique configurée dans le tableau de bord) ;
 * {@code invoice.paid} le remet en ACTIVE et synchronise la fin de période ;
 * {@code customer.subscription.deleted} annule et rétrograde le tenant sur le
 * plan gratuit canonique DISCOVERY.</p>
 */
@Service
public class StripeBillingWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingWebhookService.class);

    private final StripeBillingProperties props;
    private final StripeBillingService billingService;
    private final StripeWebhookEventRepository eventRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final WebhookLogService webhookLogService;

    public StripeBillingWebhookService(StripeBillingProperties props,
                                       StripeBillingService billingService,
                                       StripeWebhookEventRepository eventRepository,
                                       TenantSubscriptionRepository subscriptionRepository,
                                       TenantRepository tenantRepository,
                                       WebhookLogService webhookLogService) {
        this.props = props;
        this.billingService = billingService;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.webhookLogService = webhookLogService;
    }

    /**
     * Point d'entrée : idempotence + routage. Retourne false si l'événement a
     * déjà été traité (le contrôleur répond alors 200 sans rejouer).
     */
    @Transactional
    public boolean handle(Event event, String sourceIp, String rawBody) {
        if (eventRepository.existsById(event.getId())) {
            log.debug("[Stripe:Webhook] événement {} déjà traité — ignoré", event.getId());
            return false;
        }
        StripeWebhookEvent tracked = new StripeWebhookEvent(event.getId(), event.getType());
        eventRepository.save(tracked);

        WebhookLog audit = webhookLogService.logReceived("STRIPE", "/webhooks/stripe", sourceIp,
                rawBody, Map.of("event_id", event.getId(), "type", event.getType()));
        long start = System.currentTimeMillis();
        try {
            route(event);
            tracked.setStatus("PROCESSED");
            tracked.setProcessedAt(Instant.now());
            eventRepository.save(tracked);
            webhookLogService.markVerified(audit, true, (int) (System.currentTimeMillis() - start));
            return true;
        } catch (RuntimeException | StripeException e) {
            tracked.setStatus("ERROR");
            tracked.setError(truncate(e.getMessage()));
            eventRepository.save(tracked);
            webhookLogService.markError(audit, e.getMessage(), 500, (int) (System.currentTimeMillis() - start));
            log.error("[Stripe:Webhook] traitement en échec — type={} id={}", event.getType(), event.getId(), e);
            throw new IllegalStateException("Stripe event processing failed", e);
        }
    }

    private void route(Event event) throws StripeException {
        switch (event.getType()) {
            case "checkout.session.completed" -> onCheckoutCompleted(event);
            case "customer.subscription.created", "customer.subscription.updated" -> onSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> onSubscriptionDeleted(event);
            case "invoice.paid" -> onInvoicePaid(event);
            case "invoice.payment_failed" -> onInvoicePaymentFailed(event);
            default -> log.debug("[Stripe:Webhook] événement {} non traité (sans effet)", event.getType());
        }
    }

    // ── checkout.session.completed : lier client/abonnement Stripe au tenant ──

    private void onCheckoutCompleted(Event event) throws StripeException {
        Session session = deserialize(event, Session.class);
        if (session == null) return;
        UUID tenantId = parseUuid(session.getClientReferenceId());
        if (tenantId == null && session.getMetadata() != null) {
            tenantId = parseUuid(session.getMetadata().get("tenant_id"));
        }
        String stripeSubscriptionId = session.getSubscription();
        if (tenantId == null) {
            log.warn("[Stripe:Webhook] checkout.session.completed sans tenant identifiable — session={}",
                    session.getId());
            return;
        }

        TenantSubscription sub = resolveLocalSubscription(tenantId, stripeSubscriptionId, session.getMetadata());
        sub.setStripeCustomerId(session.getCustomer());
        if (stripeSubscriptionId != null) {
            sub.setStripeSubscriptionId(stripeSubscriptionId);
        }
        Subscription remote = fetchSubscription(stripeSubscriptionId);
        if (remote != null) {
            applyRemoteState(sub, remote);
        } else {
            // Stripe indisponible : statut minimal, la consolidation se fera au
            // prochain événement. Ne jamais bloquer l'activation du tenant.
            sub.setStatus(sub.getStatus() == null ? SubscriptionStatus.TRIAL : sub.getStatus());
        }
        subscriptionRepository.save(sub);

        String planKey = sub.getPlanKey();
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            if (planKey != null && !planKey.equalsIgnoreCase(tenant.getPlan())) {
                tenant.setPlan(planKey);
                tenantRepository.save(tenant);
            }
        });
        log.info("[Stripe:Webhook] ✅ checkout complété — tenant={} plan={} stripeSub={}",
                tenantId, planKey, stripeSubscriptionId);
    }

    // ── customer.subscription.* : synchronisation d'état ──

    private void onSubscriptionUpdated(Event event) throws StripeException {
        Subscription remote = deserialize(event, Subscription.class);
        if (remote == null) return;
        syncRemoteSubscription(remote);
    }

    private void onSubscriptionDeleted(Event event) throws StripeException {
        Subscription remote = deserialize(event, Subscription.class);
        if (remote == null) return;
        Optional<TenantSubscription> local = subscriptionRepository.findFirstByStripeSubscriptionId(remote.getId());
        if (local.isEmpty()) {
            log.warn("[Stripe:Webhook] subscription.deleted inconnue — {}", remote.getId());
            return;
        }
        TenantSubscription sub = local.get();
        sub.setStatus(SubscriptionStatus.CANCELED);
        sub.setCanceledAt(Instant.now());
        sub.setCancelAtPeriodEnd(false);
        subscriptionRepository.save(sub);
        downgradeTenantToFree(sub.getTenantId());
        log.info("[Stripe:Webhook] abonnement résilié — tenant={} → plan DISCOVERY", sub.getTenantId());
    }

    // ── Dunning : invoice.paid / invoice.payment_failed ──

    private void onInvoicePaid(Event event) throws StripeException {
        Invoice invoice = deserialize(event, Invoice.class);
        if (invoice == null) return;
        updateFromInvoice(invoice, SubscriptionStatus.ACTIVE);
    }

    private void onInvoicePaymentFailed(Event event) throws StripeException {
        Invoice invoice = deserialize(event, Invoice.class);
        if (invoice == null) return;
        updateFromInvoice(invoice, SubscriptionStatus.PAST_DUE);
        log.warn("[Stripe:Webhook] ⚠️ paiement échoué (dunning Stripe actif) — invoice={} sub={}",
                invoice.getId(), extractSubscriptionId(invoice));
    }

    private void updateFromInvoice(Invoice invoice, SubscriptionStatus newStatus) throws StripeException {
        String stripeSubId = extractSubscriptionId(invoice);
        if (stripeSubId == null) {
            return;
        }
        Optional<TenantSubscription> local = subscriptionRepository.findFirstByStripeSubscriptionId(stripeSubId);
        if (local.isEmpty()) {
            return;
        }
        TenantSubscription sub = local.get();
        sub.setStatus(newStatus);
        if (newStatus == SubscriptionStatus.ACTIVE) {
            Subscription remote = fetchSubscription(stripeSubId);
            if (remote != null) {
                applyRemoteState(sub, remote);
            } else if (invoice.getLines() != null) {
                log.debug("[Stripe:Webhook] période non synchronisée (retrieve indisponible) — invoice={}",
                        invoice.getId());
            }
            // Réactivation après PAST_DUE : rétablir le plan payant sur le tenant.
            tenantRepository.findById(sub.getTenantId()).ifPresent(tenant -> {
                if (sub.getPlanKey() != null && !sub.getPlanKey().equalsIgnoreCase(tenant.getPlan())) {
                    tenant.setPlan(sub.getPlanKey());
                    tenantRepository.save(tenant);
                }
            });
        }
        subscriptionRepository.save(sub);
    }

    // ── Helpers ──

    private void syncRemoteSubscription(Subscription remote) {
        UUID tenantId = parseUuid(remote.getMetadata() == null
                ? null : remote.getMetadata().get("tenant_id"));
        if (tenantId == null) {
            Optional<TenantSubscription> byStripeId =
                    subscriptionRepository.findFirstByStripeSubscriptionId(remote.getId());
            if (byStripeId.isEmpty()) {
                log.warn("[Stripe:Webhook] subscription sans tenant ni liaison locale — {}", remote.getId());
                return;
            }
            TenantSubscription sub = byStripeId.get();
            applyRemoteState(sub, remote);
            subscriptionRepository.save(sub);
            return;
        }
        TenantSubscription sub = resolveLocalSubscription(tenantId, remote.getId(), remote.getMetadata());
        applyRemoteState(sub, remote);
        subscriptionRepository.save(sub);
    }

    /** Associe l'abonnement Stripe à une ligne locale existante, au courant, ou en crée une. */
    private TenantSubscription resolveLocalSubscription(UUID tenantId, String stripeSubscriptionId,
                                                        Map<String, String> metadata) {
        if (stripeSubscriptionId != null) {
            Optional<TenantSubscription> byStripeId = subscriptionRepository.findFirstByStripeSubscriptionId(stripeSubscriptionId);
            if (byStripeId.isPresent()) {
                return byStripeId.get();
            }
        }
        Optional<TenantSubscription> current = subscriptionRepository.findCurrentByTenantId(tenantId);
        if (current.isPresent()) {
            return current.get();
        }
        String planKey = metadata != null && metadata.get("plan_key") != null
                ? TenantPlanPolicy.canonicalizePlanKey(metadata.get("plan_key")) : null;
        if (planKey == null) {
            planKey = "STARTUP";
        }
        Instant now = Instant.now();
        TenantSubscription fresh = TenantSubscription.builder()
                .tenantId(tenantId)
                .planKey(planKey)
                .status(SubscriptionStatus.TRIAL)
                .billingCycle(metadata != null ? metadata.getOrDefault("billing_cycle", "monthly") : "monthly")
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plusSeconds(30L * 24 * 3600))
                // Le montant sera figé par applyRemoteState depuis le Price Stripe.
                .priceSource("STRIPE")
                .build();
        return subscriptionRepository.save(fresh);
    }

    private void applyRemoteState(TenantSubscription local, Subscription remote) {
        local.setStatus(mapStatus(remote.getStatus()));
        if (remote.getCurrentPeriodEnd() != null) {
            local.setCurrentPeriodEnd(Instant.ofEpochSecond(remote.getCurrentPeriodEnd()));
        }
        if (remote.getCurrentPeriodStart() != null) {
            local.setCurrentPeriodStart(Instant.ofEpochSecond(remote.getCurrentPeriodStart()));
        }
        if (remote.getCancelAtPeriodEnd() != null) {
            local.setCancelAtPeriodEnd(remote.getCancelAtPeriodEnd());
        }
        if (remote.getTrialEnd() != null) {
            local.setTrialEndsAt(Instant.ofEpochSecond(remote.getTrialEnd()));
        }
        if (remote.getCustomer() != null) {
            local.setStripeCustomerId(remote.getCustomer());
        }
        if (remote.getId() != null) {
            local.setStripeSubscriptionId(remote.getId());
        }
        freezePriceFromStripe(local, remote);
    }

    /**
     * Fige le prix constaté par Stripe sur l'abonnement local (contrat §2.4).
     *
     * <p>Stripe est l'autorité de fait : c'est lui qui débitera au prochain
     * renouvellement. Tant que l'abonnement local ignore le montant du Price
     * auquel il est rattaché, l'écran « Facturation » afficherait le tarif du
     * catalogue — qui peut différer — et un litige de facturation serait
     * irrécupérable. On lit donc l'item de ligne du premier poste.</p>
     *
     * <p>Le figeage est <b>unique</b> : il ne s'écrase pas à chaque webhook. Un
     * changement de tarif ne doit pas réécrire l'historique d'un abonnement.</p>
     */
    private void freezePriceFromStripe(TenantSubscription local, Subscription remote) {
        if (local.getPriceMinor() != null || remote == null || remote.getId() == null) {
            return;
        }
        try {
            com.stripe.model.SubscriptionItem item = com.stripe.model.SubscriptionItem.list(
                    com.stripe.param.SubscriptionItemListParams.builder()
                            .setSubscription(remote.getId())
                            .setLimit(1L)
                            .build(),
                    billingService.requestOptions())
                    .getData().stream().findFirst().orElse(null);
            if (item == null || item.getPrice() == null) {
                return;
            }
            com.stripe.model.Price price = item.getPrice();
            String planKey = local.getPlanKey() == null
                    ? null : local.getPlanKey().toUpperCase(Locale.ROOT);
            String metadataPlan = price.getMetadata() == null ? null : price.getMetadata().get("plan_key");
            // Garde-fou : ne jamais figer le prix d'un autre plan sur cet abonnement.
            if (planKey != null && metadataPlan != null && !planKey.equalsIgnoreCase(metadataPlan)) {
                log.warn("[Stripe:Webhook] Price {} rattaché au plan {} mais abonnement sur {} — figeage ignoré",
                        price.getId(), metadataPlan, planKey);
                return;
            }
            if (price.getUnitAmount() == null || price.getCurrency() == null) {
                return;
            }
            String currency = price.getCurrency().toUpperCase(Locale.ROOT);
            local.setPriceMinor(price.getUnitAmount());
            local.setPriceCurrency(currency);
            local.setPriceSource("STRIPE");
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("frozenAt", Instant.now().toString());
            snapshot.put("planKey", planKey);
            snapshot.put("stripePriceId", price.getId());
            snapshot.put("stripeSubscriptionId", remote.getId());
            snapshot.put("currency", currency);
            snapshot.put("amountMinor", price.getUnitAmount());
            try {
                local.setPriceSnapshotJson(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(snapshot));
            } catch (Exception ignored) {
                local.setPriceSnapshotJson("{}");
            }
            log.info("[Stripe:Webhook] prix figé — tenant={} {} {} minor (price={})",
                    local.getTenantId(), price.getUnitAmount(), currency, price.getId());
        } catch (Exception e) {
            // Non bloquant : le webhook d'état reste traité même si la lecture du
            // Price échoue. Le prix sera figé au prochain passage.
            log.warn("[Stripe:Webhook] lecture du Price impossible pour sub={} : {}",
                    remote.getId(), e.getMessage());
        }
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) return SubscriptionStatus.PENDING_CHANGE;
        return switch (stripeStatus) {
            case "trialing" -> SubscriptionStatus.TRIAL;
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "paused" -> SubscriptionStatus.PAUSED;
            case "incomplete_expired" -> SubscriptionStatus.EXPIRED;
            default -> SubscriptionStatus.PENDING_CHANGE; // incomplete, incomplete_expired…
        };
    }

    private void downgradeTenantToFree(UUID tenantId) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setPlan("DISCOVERY");
            tenantRepository.save(tenant);
        });
    }

    private Subscription fetchSubscription(String id) {
        if (id == null || id.isBlank() || !props.isEnabled()) {
            return null;
        }
        try {
            return Subscription.retrieve(id, billingService.requestOptions());
        } catch (StripeException e) {
            log.warn("[Stripe:Webhook] retrieve subscription {} en échec: {}", id, e.getMessage());
            return null;
        }
    }

    private String extractSubscriptionId(Invoice invoice) {
        // stripe-java 24.x : l'abonnement est exposé directement sur la facture.
        return invoice.getSubscription();
    }

    private <T> T deserialize(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<?> optional = deserializer.getObject();
        if (optional.isPresent() && type.isInstance(optional.get())) {
            return type.cast(optional.get());
        }
        log.warn("[Stripe:Webhook] désérialisation impossible — type={} apiVersion={}",
                event.getType(), event.getApiVersion());
        return null;
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
