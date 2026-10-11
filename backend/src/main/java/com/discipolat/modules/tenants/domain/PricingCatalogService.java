package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Résolution des prix de catalogue (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T2).
 *
 * <p><b>Point d'entrée unique et obligatoire</b> pour toute lecture d'un prix.
 * Ni {@code SaasPlan.priceMonthly}, ni {@code priceEur}/{@code priceXaf}/{@code priceUsd}
 * ne doivent plus être lus pour facturer : ce sont des montants <b>majeurs</b>
 * hérités d'une époque où rien ne stipulait l'unité, et c'est exactement ce qui
 * faisait débiter 0,29 € au lieu de 29 € (faille F1).</p>
 *
 * <p>Ordre de résolution d'un prix (contrat §2.2) :</p>
 * <ol>
 *   <li>prix <b>régional</b> exact (si la tarification régionale est active) ;</li>
 *   <li>prix <b>GLOBAL</b> de la même devise/cycle ;</li>
 *   <li>repli optionnel sur la devise de base — <b>désactivé par défaut</b>.</li>
 * </ol>
 *
 * <p>Si aucune étape ne produit de prix, on renvoie {@link Optional#empty()} : le
 * client affiche « tarif non communiqué ». On ne renvoie <b>jamais</b> un prix
 * estimé ni un 0 de substitution — c'est la seule façon de ne jamais facturer à
 * un prix faux.</p>
 *
 * <p>Table sans filtre multi-tenant : le catalogue est global.</p>
 */
@Service
public class PricingCatalogService {

    private final SaasPlanRepository planRepository;
    private final SaasPlanPriceRepository priceRepository;
    private final PricingSettingsService settingsService;
    private final Iso4217CurrencyValidator currencyValidator;

    public PricingCatalogService(SaasPlanRepository planRepository,
                                 SaasPlanPriceRepository priceRepository,
                                 PricingSettingsService settingsService,
                                 Iso4217CurrencyValidator currencyValidator) {
        this.planRepository = planRepository;
        this.priceRepository = priceRepository;
        this.settingsService = settingsService;
        this.currencyValidator = currencyValidator;
    }

    // ── Résolution d'un prix unitaire ─────────────────────────────────────────

    /**
     * Résout le prix d'un plan pour une devise / un cycle / une région.
     *
     * @param requestedRegion région demandée, ou {@code null} pour n'utiliser que le GLOBAL
     * @return le prix résolu, ou {@link Optional#empty()} si aucun tarif n'est configuré
     */
    @Transactional(readOnly = true)
    public Optional<ResolvedPrice> resolvePrice(String planKey, String requestedCurrency,
                                                BillingCycle cycle, String requestedRegion) {
        SaasPlanPricingSettings settings = settingsService.current();
        String currency = normalizeCurrency(requestedCurrency);
        BillingCycle effectiveCycle = cycle == null ? settings.getDefaultCycle() : cycle;

        if (currency == null || !settings.isCurrencyEnabled(currency)) {
            return Optional.empty();
        }

        // 1) prix régional exact
        if (settings.isRegionPricingEnabled() && requestedRegion != null && !requestedRegion.isBlank()) {
            Optional<SaasPlanPrice> regional = find(planKey, currency, effectiveCycle, normalizeRegion(requestedRegion));
            if (regional.isPresent()) {
                return Optional.of(toResolved(regional.get(), settings, true, false));
            }
        }

        // 2) prix GLOBAL
        Optional<SaasPlanPrice> global = find(planKey, currency, effectiveCycle, SaasPlanPrice.GLOBAL_REGION);
        if (global.isPresent()) {
            return Optional.of(toResolved(global.get(), settings, false, false));
        }

        // 3) repli optionnel sur la devise de base — jamais actif par défaut
        if (settings.isFallbackCurrencyAllowed()) {
            String base = normalizeCurrency(settings.getBaseCurrency());
            if (base != null && !base.equals(currency)) {
                Optional<SaasPlanPrice> fallback = find(planKey, base, effectiveCycle, SaasPlanPrice.GLOBAL_REGION);
                if (fallback.isPresent()) {
                    return Optional.of(toResolved(fallback.get(), settings, false, true));
                }
            }
        }
        return Optional.empty();
    }

    /** Résolution avec repli sur le cycle autorisé quand le cycle demandé est interdit. */
    @Transactional(readOnly = true)
    public Optional<ResolvedPrice> resolvePriceRespectingPolicy(String planKey, String requestedCurrency,
                                                                 BillingCycle requestedCycle, String region) {
        SaasPlanPricingSettings settings = settingsService.current();
        BillingCycle cycle = requestedCycle == null ? settings.getDefaultCycle() : requestedCycle;
        if (!settings.isCycleAllowed(cycle)) {
            cycle = settings.getDefaultCycle();
        }
        return resolvePrice(planKey, requestedCurrency, cycle, region);
    }

    // ── Prix annuel calculé (faille F8) ───────────────────────────────────────

    /**
     * Prix annuel : <b>le prix saisi est prioritaire</b>. S'il n'existe pas, on
     * le calcule à partir du mensuel et de la remise annuelle du plan.
     *
     * <p>Avant ce chantier, {@code annual_discount_pct = 17} était stocké mais
     * jamais appliqué : les prix annuels seedés intégraient déjà la remise, ce
     * qui créait deux vérités concurrentes. On garde le prix saisi comme
     * référence et on ne calcule qu'en dernier recours, en le signalant.</p>
     */
    @Transactional(readOnly = true)
    public Optional<ResolvedAnnualPrice> resolveAnnualPrice(String planKey, String currency, String region) {
        Optional<ResolvedPrice> explicit = resolvePrice(planKey, currency, BillingCycle.YEARLY, region);
        if (explicit.isPresent()) {
            return Optional.of(new ResolvedAnnualPrice(explicit.get(), false));
        }
        SaasPlanPricingSettings settings = settingsService.current();
        Optional<ResolvedPrice> monthly = resolvePrice(planKey, currency, BillingCycle.MONTHLY, region);
        SaasPlan plan = planKey == null ? null
                : planRepository.findById(planKey.trim().toUpperCase(Locale.ROOT)).orElse(null);
        Integer discountPct = plan != null && plan.getAnnualDiscountPct() != null
                ? plan.getAnnualDiscountPct()
                : settings.getAnnualDiscountDefault();
        if (monthly.isEmpty() || discountPct == null || discountPct <= 0) {
            return Optional.empty();
        }
        ResolvedPrice base = monthly.get();
        long gross = Math.multiplyExact(Math.multiplyExact(base.amountMinor(), 12L),
                (100L - discountPct));
        long amountMinor = Math.round(gross / 100.0);
        ResolvedPrice computed = base.withAmountMinor(amountMinor);
        return Optional.of(new ResolvedAnnualPrice(computed, true));
    }

    // ── Catalogue public ──────────────────────────────────────────────────────

    /**
     * Catalogue public résolu pour une devise / un cycle / une région.
     *
     * <p>Correction de la faille F3 : le filtre Historically posé sur la constante
     * {@code SaasPlanService.PUBLIC_PLAN_KEYS} (appliqué deux fois) rendait
     * <b>invisible tout plan créé par le super-admin</b>, même avec
     * {@code is_public = true}. Ici la seule condition est « publié et actif ».</p>
     */
    @Transactional(readOnly = true)
    public PublicCatalog publicCatalog(String requestedCurrency, BillingCycle requestedCycle, String region) {
        SaasPlanPricingSettings settings = settingsService.current();
        String currency = resolveDisplayCurrency(requestedCurrency);
        BillingCycle cycle = (requestedCycle == null || !settings.isCycleAllowed(requestedCycle))
                ? settings.getDefaultCycle()
                : requestedCycle;

        List<SaasPlan> plans = planRepository.findByIsActiveTrueAndIsPublicTrueOrderBySortOrderAsc().stream()
                .filter(plan -> !isDisabled(plan))
                .toList();

        List<PublicPlan> publicPlans = new ArrayList<>(plans.size());
        for (SaasPlan plan : plans) {
            Optional<ResolvedPrice> display = resolvePrice(plan.getKey(), currency, cycle, region);
            List<ResolvedPrice> allPrices = resolveVisiblePrices(plan.getKey(), cycle, region);
            publicPlans.add(new PublicPlan(plan, display.orElse(null), allPrices));
        }
        return new PublicCatalog(settings, currency, cycle, region, List.copyOf(publicPlans));
    }

    /**
     * Tous les prix visibles d'un plan pour un cycle donné, dans la devise
     * demandée et — si la tarification régionale est active — dans le GLOBAL.
     *
     * <p>Sert le mode {@code LIST_ALL} (rendu historique de la page publique) et
     * la carte d'administration.</p>
     */
    @Transactional(readOnly = true)
    public List<ResolvedPrice> resolveVisiblePrices(String planKey, BillingCycle cycle, String region) {
        SaasPlanPricingSettings settings = settingsService.current();
        Set<String> currencies = settings.currencySet();
        Map<String, ResolvedPrice> byCurrency = new LinkedHashMap<>();

        for (String currency : currencies) {
            Optional<ResolvedPrice> resolved = resolvePrice(planKey, currency, cycle, region);
            resolved.ifPresent(price -> byCurrency.putIfAbsent(price.currency(), price));
        }
        return byCurrency.values().stream()
                .sorted(Comparator.comparingInt((ResolvedPrice p) -> p.sortOrder()).thenComparing(ResolvedPrice::currency))
                .toList();
    }

    /** Devise à afficher : celle demandée si autorisée, sinon celle par défaut. */
    @Transactional(readOnly = true)
    public String resolveDisplayCurrency(String requestedCurrency) {
        SaasPlanPricingSettings settings = settingsService.current();
        String currency = normalizeCurrency(requestedCurrency);
        if (currency != null && settings.isCurrencyEnabled(currency)) {
            return currency;
        }
        return normalizeCurrency(settings.getDefaultDisplayCurrency()) != null
                ? normalizeCurrency(settings.getDefaultDisplayCurrency())
                : normalizeCurrency(settings.getBaseCurrency());
    }

    /** La demande du client est-elle recevable au regard de la politique du super-admin ? */
    @Transactional(readOnly = true)
    public boolean isCurrencyRequestAllowed(String requestedCurrency) {
        if (!settingsService.current().isUserCurrencyChoiceAllowed()) {
            return false;
        }
        return resolveDisplayCurrency(requestedCurrency).equals(normalizeCurrency(requestedCurrency));
    }

    // ── Outils internes ───────────────────────────────────────────────────────

    private Optional<SaasPlanPrice> find(String planKey, String currency, BillingCycle cycle, String regionCode) {
        if (planKey == null || planKey.isBlank()) return Optional.empty();
        String key = TenantPlanPolicy.canonicalizePlanKey(planKey);
        if (key == null) return Optional.empty();
        return priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
                key, currency, cycle, regionCode)
                .filter(price -> Boolean.TRUE.equals(price.getIsEnabled()));
    }

    private ResolvedPrice toResolved(SaasPlanPrice price, SaasPlanPricingSettings settings,
                                     boolean regional, boolean fallback) {
        int decimals = currencyValidator.minorUnits(price.getCurrency());
        List<PaymentChannel> channels = declaredChannels(price, settings);
        return new ResolvedPrice(
                price.getPlanKey(),
                price.getCurrency(),
                price.getBillingCycle(),
                price.getRegionCode(),
                price.getAmountMinor(),
                currencyValidator.fromMinorUnits(price.getCurrency(), price.getAmountMinor()).toPlainString(),
                decimals,
                regional,
                fallback,
                Boolean.TRUE.equals(price.getIsSelectable()),
                Boolean.TRUE.equals(price.getIsVisible()),
                channels,
                price.effectiveCheckoutCurrency(),
                Boolean.TRUE.equals(price.getIsSelectable()) && price.channels().contains(PaymentChannel.STRONG),
                price.getStripePriceStatus() == null ? null : price.getStripePriceStatus().name(),
                price.getSortOrder() == null ? 0 : price.getSortOrder(),
                price.getId());
    }

    /**
     * Canaux effectivement proposés, filtrés par ce que le super-admin autorise.
     * Le canal CARD n'est retiré que si {@code allow_card_channel} est coupé :
     * les autres canaux sont validés par {@code PaymentChannelResolver}, qui
     * connaît la disponibilité technique des prestataires.
     */
    private List<PaymentChannel> declaredChannels(SaasPlanPrice price, SaasPlanPricingSettings settings) {
        List<PaymentChannel> result = new ArrayList<>();
        for (PaymentChannel channel : price.channels()) {
            boolean allowed = switch (channel) {
                case CARD -> Boolean.TRUE.equals(settings.getAllowCardChannel());
                case MOMO -> Boolean.TRUE.equals(settings.getAllowMomoChannel());
                case STRONG -> Boolean.TRUE.equals(settings.getAllowStrongChannel());
                case BANK -> Boolean.TRUE.equals(settings.getAllowBankChannel());
                case CONTACT -> Boolean.TRUE.equals(settings.getAllowContactChannel());
            };
            if (allowed) result.add(channel);
        }
        if (result.isEmpty() && Boolean.TRUE.equals(settings.getAllowContactChannel())) {
            // Aucun canal payable : on ne laisse jamais l'appelant sans issue.
            result.add(PaymentChannel.CONTACT);
        }
        return List.copyOf(result);
    }

    private static boolean isDisabled(SaasPlan plan) {
        String status = plan.getStatus();
        return status != null && (status.equalsIgnoreCase("INACTIVE") || status.equalsIgnoreCase("DEPRECATED"));
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) return null;
        String code = currency.trim().toUpperCase(Locale.ROOT);
        return code.matches("[A-Z]{3}") ? code : null;
    }

    private static String normalizeRegion(String region) {
        if (region == null || region.isBlank()) return SaasPlanPrice.GLOBAL_REGION;
        return region.trim().toUpperCase(Locale.ROOT);
    }

    // ── Types de sortie ───────────────────────────────────────────────────────

    /** Prix unitaire résolu, déjà converti en unités mineures exactes. */
    public record ResolvedPrice(
            String planKey,
            String currency,
            BillingCycle cycle,
            String regionCode,
            long amountMinor,
            String amount,
            int decimals,
            boolean regional,
            boolean fromFallback,
            boolean selectable,
            boolean visible,
            List<PaymentChannel> channels,
            String checkoutCurrency,
            boolean strongCurrencyAvailable,
            String stripePriceStatus,
            int sortOrder,
            java.util.UUID id) {

        public BigDecimal amountMajor() {
            return BigDecimal.valueOf(amountMinor, decimals);
        }

        /** Prix identique avec un autre montant — utilisé pour le prix annuel calculé. */
        public ResolvedPrice withAmountMinor(long newAmountMinor) {
            BigDecimal major = BigDecimal.valueOf(newAmountMinor, decimals);
            return new ResolvedPrice(planKey, currency, cycle, regionCode, newAmountMinor,
                    major.toPlainString(), decimals, regional, fromFallback, selectable, visible,
                    channels, checkoutCurrency, strongCurrencyAvailable, stripePriceStatus,
                    sortOrder, id);
        }
    }

    /** Prix annuel, avec le drapeau « calculé » (faille F8). */
    public record ResolvedAnnualPrice(ResolvedPrice price, boolean computed) { }

    /** Un plan tel que vu par un visiteur. */
    public record PublicPlan(SaasPlan plan, ResolvedPrice display, List<ResolvedPrice> prices) { }

    /** Catalogue complet, avec les réglages qui l'ont produit. */
    public record PublicCatalog(SaasPlanPricingSettings settings, String currency,
                                BillingCycle cycle, String region, List<PublicPlan> plans) { }
}