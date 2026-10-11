package com.discipolat.modules.payments.stripe;

import com.discipolat.modules.tenants.domain.PricingSettingsService;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanPrice;
import com.discipolat.modules.tenants.domain.SaasPlanPriceRepository;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.StripePriceStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.net.RequestOptions;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.ProductCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Création des Prices Stripe par l'API (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.1).
 *
 * <p><b>Pourquoi.</b> Stripe refuse le prix « inline » ({@code price_data}) pour
 * les devises sans unité mineure : XAF, XOF, JPY, KRW. Un {@code Price} doit
 * exister au préalable. Avant ce chantier, la seule façon de débloquer le plan
 * STARTUP (en XAF) était de créer ce Price à la main dans le dashboard Stripe —
 * et tant que ce n'était pas fait, le checkout renvoyait
 * {@code STRIPE_PRICE_ID_REQUIRED}. C'est la faille F2, celle qui bloquait la
 * monétisation du marché principal.</p>
 *
 * <p><b>Comment.</b> Un {@code Product} par plan, puis un {@code Price} par ligne
 * de catalogue (devise × cycle × région). L'opération est <b>idempotente</b> : la
 * clé d'idempotence Stripe dérive de l'identité du prix et de son montant, donc un
 * rejeu après un échec réseau ne crée jamais de doublon. Un changement de montant
 * crée un nouveau Price et <b>désactive</b> l'ancien ({@code active = false}) au
 * lieu de le supprimer : les abonnements déjà rattachés à l'ancien Price
 * continuent de fonctionner, ce qui est exactement le comportement voulu.</p>
 *
 * <p><b>Fail-closed.</b> Sans {@code STRIPE_SECRET_KEY}, aucune requête n'est
 * émise et les lignes restent {@code PENDING}. Le service ne prétend jamais avoir
 * synchronisé ce qu'il n'a pas synchronisé.</p>
 */
@Service
public class StripePriceSyncService {

    private static final Logger log = LoggerFactory.getLogger(StripePriceSyncService.class);

    /** Métadonnées écrites sur chaque Price, pour tracer l'origine côté Stripe. */
    private static final String META_PLAN_KEY = "plan_key";
    private static final String META_CURRENCY = "currency";
    private static final String META_CYCLE = "billing_cycle";
    private static final String META_REGION = "region_code";
    private static final String META_AMOUNT_MINOR = "amount_minor";
    private static final String META_MANAGED_BY = "managed_by";

    private final StripeBillingProperties props;
    private final SaasPlanPriceRepository priceRepository;
    private final SaasPlanRepository planRepository;
    private final PricingSettingsService settingsService;

    public StripePriceSyncService(StripeBillingProperties props,
                                  SaasPlanPriceRepository priceRepository,
                                  SaasPlanRepository planRepository,
                                  PricingSettingsService settingsService) {
        this.props = props;
        this.priceRepository = priceRepository;
        this.planRepository = planRepository;
        this.settingsService = settingsService;
    }

    // ── Synchronisation d'une ligne ───────────────────────────────────────────

    /**
     * Synchronise un prix de catalogue vers Stripe.
     *
     * @return l'identifiant du Price Stripe, ou {@link Optional#empty()} si la ligne
     *         n'a pas à être synchronisée (canal hors carte, devise inline possible)
     */
    @Transactional
    public Optional<String> syncPrice(SaasPlanPrice price) {
        if (price == null) return Optional.empty();
        if (!requiresStripePrice(price)) {
            if (price.getStripePriceStatus() == StripePriceStatus.PENDING) {
                price.setStripePriceStatus(StripePriceStatus.NOT_REQUIRED);
                priceRepository.save(price);
            }
            return Optional.empty();
        }
        // Un Price déjà synchronisé avec le bon montant ne demande rien à Stripe.
        if (isAlreadySynced(price)) {
            return Optional.of(price.getStripePriceId());
        }
        if (!props.isEnabled()) {
            markPending(price, "STRIPE_DISABLED",
                    "Cle Stripe absente : le Price sera cree a la premiere synchronisation.");
            return Optional.empty();
        }
        try {
            String productId = ensureProduct(price.getPlanKey());
            PriceCreateParams params = PriceCreateParams.builder()
                    .setProduct(productId)
                    .setCurrency(price.getCurrency().toLowerCase(Locale.ROOT))
                    // CONTRAT §2.1 : amount_minor est déjà en unités mineures,
                    // exactement ce que Stripe attend. Aucun ×100 ici.
                    .setUnitAmount(price.getAmountMinor())
                    .setRecurring(PriceCreateParams.Recurring.builder()
                            .setInterval(price.getBillingCycle() == BillingCycle.YEARLY
                                    ? PriceCreateParams.Recurring.Interval.YEAR
                                    : PriceCreateParams.Recurring.Interval.MONTH)
                            .build())
                    .putAllMetadata(Map.of(
                            META_PLAN_KEY, price.getPlanKey(),
                            META_CURRENCY, price.getCurrency(),
                            META_CYCLE, price.getBillingCycle().name(),
                            META_REGION, price.getRegionCode(),
                            META_AMOUNT_MINOR, String.valueOf(price.getAmountMinor()),
                            META_MANAGED_BY, "discipolat"))
                    .setActive(true)
                    .build();

            Price created = Price.create(params, idempotentOptions(price));
            String previousId = price.getStripePriceId();
            price.setStripePriceId(created.getId());
            price.setStripePriceStatus(StripePriceStatus.SYNCED);
            price.setStripePriceSyncedAt(Instant.now());
            price.setStripeSyncError(null);
            priceRepository.save(price);

            deactivateSuperseded(previousId, created.getId());
            log.info("[Stripe] Price cree — plan={} devise={} cycle={} montant={} price={}",
                    price.getPlanKey(), price.getCurrency(), price.getBillingCycle(),
                    price.getAmountMinor(), created.getId());
            return Optional.of(created.getId());
        } catch (StripeException e) {
            markFailed(price, e);
            return Optional.empty();
        } catch (RuntimeException e) {
            markFailed(price, e);
            return Optional.empty();
        }
    }

    // ── Lot planifié ──────────────────────────────────────────────────────────

    /**
     * Reprise des lignes en attente ou en échec.
     *
     * <p>Toujours exécuté, même si {@code stripe_price_auto_sync} est désactivé :
     * la désactivation signifie « ne crée pas tout seul », pas « ne réessaie
     * jamais ». Ce qu'elle contrôle, c'est le déclenchement à l'écriture d'un
     * plan ; le rattrapage d'un échec réseau, lui, doit toujours avoir lieu.</p>
     */
    @Scheduled(
            initialDelayString = "${app.stripe.price-sync-initial-delay-ms:60000}",
            fixedDelayString = "${app.stripe.price-sync-interval-ms:300000}")
    @Transactional
    public int retryPending() {
        if (!props.isEnabled()) {
            return 0;
        }
        Integer configured = settingsService.current().getStripeSyncBatchSize();
        int batch = (configured == null || configured < 1) ? 20 : configured;
        List<SaasPlanPrice> pending = priceRepository.findByStripePriceStatusIn(
                EnumSet.of(StripePriceStatus.PENDING, StripePriceStatus.FAILED));
        if (pending.isEmpty()) {
            return 0;
        }
        int synced = 0;
        for (SaasPlanPrice price : pending.stream().limit(batch).toList()) {
            if (!requiresStripePrice(price)) {
                price.setStripePriceStatus(StripePriceStatus.NOT_REQUIRED);
                priceRepository.save(price);
                continue;
            }
            if (syncPrice(price).isPresent()) {
                synced++;
            }
        }
        if (synced > 0) {
            log.info("[Stripe] rattrapage : {} Price(s) synchronise(s) sur {} en attente", synced, pending.size());
        }
        return synced;
    }

    // ── API d'administration ──────────────────────────────────────────────────

    /** Relance toutes les lignes d'un plan. */
    @Transactional
    public SyncReport syncPlan(String planKey) {
        List<SaasPlanPrice> prices = priceRepository.findByPlanKeyOrderBySortOrderAscCurrencyAsc(planKey);
        int synced = 0;
        int skipped = 0;
        int failed = 0;
        for (SaasPlanPrice price : prices) {
            if (!requiresStripePrice(price)) {
                skipped++;
                continue;
            }
            if (syncPrice(price).isPresent()) synced++; else failed++;
        }
        return new SyncReport(synced, skipped, failed, props.isEnabled());
    }

    /** Relance toutes les lignes du catalogue. */
    @Transactional
    public SyncReport syncAll() {
        Integer configured = settingsService.current().getStripeSyncBatchSize();
        int batch = (configured == null || configured < 1) ? 20 : configured;
        List<SaasPlanPrice> pending = priceRepository.findByStripePriceStatusIn(
                EnumSet.of(StripePriceStatus.PENDING, StripePriceStatus.FAILED));
        int synced = 0;
        int failed = 0;
        for (SaasPlanPrice price : pending.stream().limit(batch).toList()) {
            if (!requiresStripePrice(price)) {
                price.setStripePriceStatus(StripePriceStatus.NOT_REQUIRED);
                priceRepository.save(price);
                continue;
            }
            if (syncPrice(price).isPresent()) synced++; else failed++;
        }
        return new SyncReport(synced, pending.size() - synced - failed, failed, props.isEnabled());
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    /**
     * Ce prix a-t-il besoin d'un Price Stripe ?
     *
     * <p>Oui si le canal carte est proposé <b>et</b> que la devise n'accepte pas
     * le prix inline. Sinon Stripe peut créer le prix à la volée au checkout : on
     * n'encombre pas le compte Stripe d'objets inutiles.</p>
     */
    private boolean requiresStripePrice(SaasPlanPrice price) {
        if (!price.channels().contains(com.discipolat.modules.tenants.enums.PaymentChannel.CARD)) {
            return false;
        }
        String checkoutCurrency = price.effectiveCheckoutCurrency();
        if (checkoutCurrency == null || checkoutCurrency.isBlank()) return false;
        if (price.getStripePriceStatus() == StripePriceStatus.MANUAL) {
            return false; // le super-admin a saisi l'identifiant lui-même
        }
        return isZeroDecimal(checkoutCurrency);
    }

    private boolean isAlreadySynced(SaasPlanPrice price) {
        if (price.getStripePriceStatus() != StripePriceStatus.SYNCED) return false;
        if (price.getStripePriceId() == null || price.getStripePriceId().isBlank()) return false;
        // L'identifiant existe : le montant peut avoir changé depuis, on vérifie
        // que le Price Stripe porte bien la valeur courante.
        return remoteAmountMatches(price);
    }

    private boolean remoteAmountMatches(SaasPlanPrice price) {
        try {
            Price remote = Price.retrieve(price.getStripePriceId(), requestOptions());
            if (remote == null) return false;
            if (!remote.getActive()) return false;
            if (remote.getUnitAmount() == null) return false;
            return remote.getUnitAmount().equals(price.getAmountMinor())
                    && remote.getCurrency().equalsIgnoreCase(price.effectiveCheckoutCurrency());
        } catch (StripeException e) {
            log.warn("[Stripe] lecture du Price {} impossible — resynchronisation : {}",
                    price.getStripePriceId(), e.getMessage());
            return false;
        }
    }

    private String ensureProduct(String planKey) throws StripeException {
        Optional<SaasPlan> maybePlan = planRepository.findById(planKey);
        SaasPlan plan = maybePlan.orElseThrow(
                () -> new IllegalStateException("Plan inconnu : " + planKey));
        if (plan.getStripeProductId() != null && !plan.getStripeProductId().isBlank()) {
            return plan.getStripeProductId();
        }
        ProductCreateParams params = ProductCreateParams.builder()
                .setName(plan.getName())
                .setDescription(plan.getDescription())
                .putAllMetadata(Map.of(META_PLAN_KEY, plan.getKey(), META_MANAGED_BY, "discipolat"))
                .build();
        Product created = Product.create(params, requestOptions());
        plan.setStripeProductId(created.getId());
        planRepository.save(plan);
        log.info("[Stripe] produit cree pour le plan {} -> {}", planKey, created.getId());
        return created.getId();
    }

    /**
     * Désactive le Price précédent plutôt que de le supprimer.
     *
     * <p>Un Price actif est immuable chez Stripe ; la seule façon d'en changer le
     * montant est d'en créer un nouveau. Désactiver l'ancien est sans risque :
     * les abonnements qui le référencent continuent d'être facturés au prix
     * auquel ils se sont abonnés — précisément ce que veut le contrat de figeage
     * du prix (§2.4).</p>
     */
    private void deactivateSuperseded(String previousPriceId, String currentPriceId) {
        if (previousPriceId == null || previousPriceId.isBlank()) return;
        if (previousPriceId.equals(currentPriceId)) return;
        try {
            Price previous = Price.retrieve(previousPriceId, requestOptions());
            if (previous != null && Boolean.TRUE.equals(previous.getActive())) {
                previous.update(PriceUpdateParams.builder().setActive(false).build(), requestOptions());
                log.info("[Stripe] ancien Price {} desactive au profit de {}", previousPriceId, currentPriceId);
            }
        } catch (StripeException e) {
            // Non bloquant : l'ancien Price reste actif, il facturera simplement
            // le tarif précédent — pas une perte de revenu.
            log.warn("[Stripe] desactivation du Price {} impossible : {}", previousPriceId, e.getMessage());
        }
    }

    private void markPending(SaasPlanPrice price, String reason, String message) {
        price.setStripePriceStatus(StripePriceStatus.PENDING);
        price.setStripeSyncError(truncate(reason + " : " + message));
        priceRepository.save(price);
        log.info("[Stripe] Price en attente — plan={} devise={} : {}",
                price.getPlanKey(), price.getCurrency(), reason);
    }

    private void markFailed(SaasPlanPrice price, Exception e) {
        price.setStripePriceStatus(StripePriceStatus.FAILED);
        price.setStripeSyncError(truncate(e.getClass().getSimpleName() + " : " + e.getMessage()));
        priceRepository.save(price);
        log.error("[Stripe] echec de synchronisation — plan={} devise={} cycle={}",
                price.getPlanKey(), price.getCurrency(), price.getBillingCycle(), e);
    }

    /**
     * Clé d'idempotence : identité du prix + montant.
     *
     * <p>Deux lignes différentes (devise, cycle ou région) n'entrent jamais en
     * collision, et un changement de montant produit volontairement une clé
     * nouvelle — c'est le comportement voulu, puisque Stripe refuse de modifier
     * un Price existant.</p>
     */
    private RequestOptions idempotentOptions(SaasPlanPrice price) {
        return RequestOptions.builder()
                .setApiKey(props.getSecretKey())
                .setIdempotencyKey(String.join(":", "discipolat",
                        price.getPlanKey(),
                        price.getCurrency(),
                        price.getBillingCycle().name(),
                        price.getRegionCode(),
                        String.valueOf(price.getAmountMinor())))
                .build();
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder().setApiKey(props.getSecretKey()).build();
    }

    private static boolean isZeroDecimal(String currency) {
        try {
            return java.util.Currency.getInstance(currency.trim().toUpperCase(Locale.ROOT))
                    .getDefaultFractionDigits() == 0;
        } catch (IllegalArgumentException unknown) {
            return false;
        }
    }

    private static String truncate(String message) {
        if (message == null) return null;
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    /** Résultat d'une synchronisation, pour l'écran d'administration. */
    public record SyncReport(int synced, int skipped, int failed, boolean stripeEnabled) {
        public int total() {
            return synced + skipped + failed;
        }

        public boolean stripeAvailable() {
            return stripeEnabled;
        }
    }
}