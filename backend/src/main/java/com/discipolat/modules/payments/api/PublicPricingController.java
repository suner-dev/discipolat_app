package com.discipolat.modules.payments.api;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.payments.domain.PaymentChannelResolver;
import com.discipolat.modules.payments.stripe.StripeBillingProperties;
import com.discipolat.modules.tenants.api.PublicSaasPlanController;
import com.discipolat.modules.tenants.domain.PricingCatalogService;
import com.discipolat.modules.tenants.domain.PricingRegionResolver;
import com.discipolat.modules.tenants.domain.PricingSettingsService;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanPricingSettings;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogue de tarifs public (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T3.1).
 *
 * <p>Trois lectures, trois usages :</p>
 * <ul>
 *   <li>{@code /config} — ce que le visiteur a le droit de faire (choix de devise,
 *       de cycle, de région, canaux ouverts). Tout y est décidé par le super-admin ;</li>
 *   <li>{@code /plans} — les prix résolus pour la devise/le cycle/la région demandés ;</li>
 *   <li>{@code /currency} — la devise et la région effectivement retenues, pour
 *       que le client affiche l'indicateur sans le deviner.</li>
 * </ul>
 *
 * <p>Points de contrat visibles ici :</p>
 * <ul>
 *   <li>un plan sans prix dans la devise demandée est renvoyé avec
 *       {@code display.available = false} — <b>jamais</b> un montant à 0 ;</li>
 *   <li>les canaux annoncés sont ceux réellement disponibles (prestataire configuré)
 *       : un bouton « Carte bancaire » qui mène à un échec n'est pas une option ;</li>
 *   <li>aucune information de tenant n'est exposée.</li>
 * </ul>
 *
 * <p><b>Pourquoi ce contrôleur vit dans {@code payments} et non dans
 * {@code tenants}.</b> Il expose des <em>canaux de paiement</em> : dire à un
 * visiteur « carte bancaire disponible » est une affirmation du module
 * paiements, pas du catalogue. Le placer dans {@code tenants} créait l'arête
 * {@code tenants -> payments}, qui se refermait en cycle
 * {@code payments <-> tenants} puisque {@code payments} consulte déjà les
 * plans. Ici l'arête va dans le sens inverse — {@code payments -> tenants} —
 * déjà admise par le gel d'architecture.</p>
 */
@RestController
@RequestMapping("/api/v1/public/pricing")
public class PublicPricingController {

    /** 60 s : l_PARAM_ADMIN est plus rare que la lecture, et le catalogue change peu. */
    private static final Duration CACHE = Duration.ofSeconds(60);

    private final PricingCatalogService catalogService;
    private final PricingSettingsService settingsService;
    private final PricingRegionResolver regionResolver;
    private final PaymentChannelResolver channelResolver;
    private final StripeBillingProperties stripeProperties;
    private final Iso4217CurrencyValidator currencyValidator;
    /** Injecté et réutilisé : la page publique l'appelle sur chaque réponse. */
    private final ObjectMapper objectMapper;

    public PublicPricingController(ObjectMapper objectMapper,
                                   PricingCatalogService catalogService,
                                   PricingSettingsService settingsService,
                                   PricingRegionResolver regionResolver,
                                   PaymentChannelResolver channelResolver,
                                   StripeBillingProperties stripeProperties,
                                   Iso4217CurrencyValidator currencyValidator) {
        this.objectMapper = objectMapper;
        this.catalogService = catalogService;
        this.settingsService = settingsService;
        this.regionResolver = regionResolver;
        this.channelResolver = channelResolver;
        this.stripeProperties = stripeProperties;
        this.currencyValidator = currencyValidator;
    }

    // ── GET /api/v1/public/pricing/config ─────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config(@RequestParam(required = false) String currency,
                                                      @RequestParam(required = false) String region,
                                                      HttpServletRequest request) {
        SaasPlanPricingSettings settings = settingsService.current();
        String resolvedCurrency = catalogService.resolveDisplayCurrency(
                settings.isUserCurrencyChoiceAllowed() ? currency : settings.getDefaultDisplayCurrency());
        String resolvedRegion = regionResolver
                .resolve(request, region, null)
                .orElse(settings.getDefaultRegion());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("catalogVersion", settings.getCatalogVersion());
        body.put("displayMode", settings.getDisplayMode().name());
        body.put("baseCurrency", settings.getBaseCurrency());
        body.put("defaultCurrency", settings.getDefaultDisplayCurrency());
        body.put("resolvedCurrency", resolvedCurrency);
        body.put("resolvedRegion", resolvedRegion);
        body.put("regionPricingEnabled", settings.isRegionPricingEnabled());
        body.put("allowCurrencyChoice", settings.isUserCurrencyChoiceAllowed());
        body.put("allowCycleChoice", settings.isCycleChoiceAllowed());
        body.put("allowRegionChoice", settings.isRegionChoiceAllowed());
        body.put("allowMonthly", settings.getAllowMonthly());
        body.put("allowYearly", settings.getAllowYearly());
        body.put("defaultCycle", settings.getDefaultCycle().name());
        body.put("trialEnabled", settings.isTrialEnabled());
        body.put("stripeEnabled", stripeProperties.isEnabled());
        body.put("enabledCurrencies", currencyCatalog(settings, resolvedCurrency));
        body.put("regions", regions(settings));
        body.put("channels", channels(settings));
        body.put("landing", landing(settings));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(CACHE).cachePublic()).body(body);
    }

    // ── GET /api/v1/public/pricing/plans ──────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> plans(@RequestParam(required = false) String currency,
                                                     @RequestParam(required = false) String cycle,
                                                     @RequestParam(required = false) String region,
                                                     HttpServletRequest request) {
        SaasPlanPricingSettings settings = settingsService.current();
        String requestedCurrency = settings.isUserCurrencyChoiceAllowed() ? currency : settings.getDefaultDisplayCurrency();
        String resolvedCurrency = catalogService.resolveDisplayCurrency(requestedCurrency);
        BillingCycle requestedCycle = BillingCycle.fromString(cycle);
        BillingCycle resolvedCycle = settings.isCycleAllowed(requestedCycle)
                ? requestedCycle
                : settings.getDefaultCycle();
        String resolvedRegion = regionResolver.resolve(request, region, null)
                .orElse(settings.getDefaultRegion());

        PricingCatalogService.PublicCatalog catalog =
                catalogService.publicCatalog(resolvedCurrency, resolvedCycle, resolvedRegion);

        List<Map<String, Object>> plans = new ArrayList<>(catalog.plans().size());
        for (PricingCatalogService.PublicPlan publicPlan : catalog.plans()) {
            plans.add(toPlanView(publicPlan, settings, resolvedCurrency, resolvedCycle));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("catalogVersion", settings.getCatalogVersion());
        body.put("currency", resolvedCurrency);
        body.put("cycle", resolvedCycle.name());
        body.put("region", resolvedRegion);
        body.put("plans", plans);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(CACHE).cachePublic()).body(body);
    }

    /** Un plan isolé — utile pour un lien profond « voir ce tarif ». */
    @GetMapping("/plans/{key}")
    public ResponseEntity<Map<String, Object>> plan(@org.springframework.web.bind.annotation.PathVariable String key,
                                                    @RequestParam(required = false) String currency,
                                                    @RequestParam(required = false) String cycle,
                                                    @RequestParam(required = false) String region,
                                                    HttpServletRequest request) {
        PricingCatalogService.PublicCatalog catalog = catalogService.publicCatalog(
                catalogService.resolveDisplayCurrency(currency),
                BillingCycle.fromString(cycle),
                regionResolver.resolve(request, region, null).orElse(settingsService.current().getDefaultRegion()));
        Optional<PricingCatalogService.PublicPlan> found = catalog.plans().stream()
                .filter(publicPlan -> publicPlan.plan().getKey().equalsIgnoreCase(key))
                .findFirst();
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("catalogVersion", catalog.settings().getCatalogVersion());
        body.put("currency", catalog.currency());
        body.put("cycle", catalog.cycle().name());
        body.put("region", catalog.region());
        body.put("plan", toPlanView(found.get(), catalog.settings(), catalog.currency(), catalog.cycle()));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(CACHE).cachePublic()).body(body);
    }

    // ── GET /api/v1/public/pricing/currency ───────────────────────────────────

    /** Devise et région retenues pour cet appelant — l'indicateur ne devine rien. */
    @GetMapping("/currency")
    public ResponseEntity<Map<String, Object>> resolvedCurrency(@RequestParam(required = false) String currency,
                                                                @RequestParam(required = false) String region,
                                                                HttpServletRequest request) {
        SaasPlanPricingSettings settings = settingsService.current();
        String requested = settings.isUserCurrencyChoiceAllowed() ? currency : settings.getDefaultDisplayCurrency();
        String resolved = catalogService.resolveDisplayCurrency(requested);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currency", resolved);
        body.put("requested", requested);
        body.put("honored", resolved.equalsIgnoreCase(requested));
        body.put("region", regionResolver.resolve(request, region, null).orElse(settings.getDefaultRegion()));
        body.put("decimals", decimalsOf(resolved));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    // ── Vue publique d'un plan ───────────────────────────────────────────────

    private Map<String, Object> toPlanView(PricingCatalogService.PublicPlan publicPlan,
                                           SaasPlanPricingSettings settings,
                                           String resolvedCurrency, BillingCycle resolvedCycle) {
        SaasPlan plan = publicPlan.plan();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("key", plan.getKey());
        view.put("name", plan.getName());
        view.put("description", plan.getDescription());
        view.put("sortOrder", plan.getSortOrder());
        view.put("isPublic", plan.getIsPublic());
        view.put("status", plan.getStatus());
        view.put("trialDays", settings.isTrialEnabled() ? plan.getTrialDays() : 0);
        view.put("annualDiscountPct", plan.getAnnualDiscountPct());
        view.put("seatsLimit", plan.getSeatsLimit());
        view.put("storageLimitMb", plan.getStorageLimitMb());
        view.put("aiCreditsLimit", plan.getAiCreditsLimit());
        view.put("limits", parseJson(plan.getLimitsJson()));
        view.put("features", parseJson(plan.getFeaturesJson()));
        view.put("modules", parseJsonArray(plan.getModulesIncludedJson()));

        List<Map<String, Object>> prices = new ArrayList<>();
        for (PricingCatalogService.ResolvedPrice price : publicPlan.prices()) {
            prices.add(toPriceView(price));
        }
        view.put("prices", prices);
        view.put("display", toDisplayView(publicPlan.display(), settings, resolvedCurrency, resolvedCycle));
        return view;
    }

    private Map<String, Object> toPriceView(PricingCatalogService.ResolvedPrice price) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("currency", price.currency());
        view.put("cycle", price.cycle().name());
        view.put("amountMinor", price.amountMinor());
        // Les deux champs : amountMinor est autoritaire, amount sert à l'affichage.
        view.put("amount", price.amount());
        view.put("decimals", price.decimals());
        view.put("region", price.regionCode());
        view.put("isRegional", price.regional());
        view.put("fromFallback", price.fromFallback());
        view.put("selectable", price.selectable());
        view.put("checkoutCurrency", price.checkoutCurrency());
        view.put("channels", price.channels().stream().map(Enum::name).toList());
        view.put("stripePriceStatus", price.stripePriceStatus());
        return view;
    }

    /**
     * Le prix à afficher pour le visiteur, et ce qu'il peut en faire.
     *
     * <p>{@code available = false} est une réponse de premier ordre : elle dit au
     * client de ne rien afficher comme montant et de proposer un contact, plutôt
     * que de laisser un « 0 » se lire comme un plan gratuit.</p>
     */
    private Map<String, Object> toDisplayView(PricingCatalogService.ResolvedPrice price,
                                              SaasPlanPricingSettings settings,
                                              String resolvedCurrency, BillingCycle resolvedCycle) {
        Map<String, Object> view = new LinkedHashMap<>();
        if (price == null) {
            view.put("available", false);
            view.put("currency", resolvedCurrency);
            view.put("cycle", resolvedCycle.name());
            view.put("amountMinor", null);
            view.put("amount", null);
            view.put("decimals", decimalsOf(resolvedCurrency));
            view.put("channels", List.of());
            view.put("selectable", false);
            view.put("reason", "NO_PRICE_IN_CURRENCY");
            return view;
        }
        view.put("available", true);
        view.put("amountMinor", price.amountMinor());
        view.put("amount", price.amount());
        view.put("currency", price.currency());
        view.put("decimals", price.decimals());
        view.put("cycle", price.cycle().name());
        view.put("region", price.regionCode());
        view.put("isRegional", price.regional());
        view.put("fromFallback", price.fromFallback());
        view.put("selectable", price.selectable());
        view.put("checkoutCurrency", price.checkoutCurrency());
        view.put("chargedInDifferentCurrency",
                price.checkoutCurrency() != null && !price.checkoutCurrency().equalsIgnoreCase(price.currency()));
        view.put("channels", availableChannels(price));
        view.put("reason", null);
        return view;
    }

    /**
     * Canaux réellement disponibles pour un prix résolu.
     *
     * <p>Le prix est déclaré sans sa ligne brute (le {@code id} est absent une
     * fois passé par le catalogue), donc on delegate à la surcharge « résolu »
     * du resolver, qui applique la même politique. Un canal déclaré mais
     * indisponible est retiré : mieux vaut proposer « nous contacter » qu'un
     * bouton vers un échec.</p>
     */
    private List<String> availableChannels(PricingCatalogService.ResolvedPrice price) {
        return channelResolver
                .available(price.currency(), price.checkoutCurrency(), price.stripePriceId(),
                        price.stripePriceStatus(), price.channels())
                .stream()
                .map(Enum::name)
                .toList();
    }

    // ── Sous-payloads de /config ─────────────────────────────────────────────

    private List<Map<String, Object>> currencyCatalog(SaasPlanPricingSettings settings, String resolvedCurrency) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String code : settings.currencySet().stream().sorted().toList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", code);
            entry.put("symbol", safeSymbol(code));
            entry.put("name", safeName(code));
            entry.put("decimals", decimalsOf(code));
            entry.put("selected", code.equalsIgnoreCase(resolvedCurrency));
            list.add(entry);
        }
        return list;
    }

    private List<Map<String, Object>> regions(SaasPlanPricingSettings settings) {
        List<Map<String, Object>> list = new ArrayList<>();
        PricingRegionResolver.regions().forEach((code, labelKey) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", code);
            entry.put("labelKey", labelKey);
            list.add(entry);
        });
        if (!settings.isRegionPricingEnabled()) {
            list.clear();
        }
        return list;
    }

    private Map<String, Boolean> channels(SaasPlanPricingSettings settings) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (PaymentChannel channel : PaymentChannel.values()) {
            boolean allowed = switch (channel) {
                case CARD -> Boolean.TRUE.equals(settings.getAllowCardChannel());
                case MOMO -> Boolean.TRUE.equals(settings.getAllowMomoChannel());
                case STRONG -> Boolean.TRUE.equals(settings.getAllowStrongChannel());
                case BANK -> Boolean.TRUE.equals(settings.getAllowBankChannel());
                case CONTACT -> Boolean.TRUE.equals(settings.getAllowContactChannel());
            };
            map.put(channel.name(), allowed);
        }
        return map;
    }

    private Map<String, Object> landing(SaasPlanPricingSettings settings) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", settings.getLandingPricingEnabled());
        map.put("source", settings.getLandingPricingSource().name());
        map.put("maxPlans", settings.getLandingPricingMaxPlans());
        map.put("showPrices", settings.getLandingPricingShowPrices());
        map.put("showToggle", settings.getLandingPricingShowToggle());
        map.put("ctaLabel", settings.getLandingPricingCtaLabel());
        return map;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private int decimalsOf(String currency) {
        if (currency == null || currency.isBlank()) return 2;
        try {
            return currencyValidator.minorUnits(currency);
        } catch (RuntimeException unknown) {
            return 2;
        }
    }

    private String safeSymbol(String code) {
        try {
            return java.util.Currency.getInstance(code).getSymbol(Locale.US);
        } catch (RuntimeException unknown) {
            return code;
        }
    }

    private String safeName(String code) {
        try {
            return java.util.Currency.getInstance(code).getDisplayName(Locale.ENGLISH);
        } catch (RuntimeException unknown) {
            return code;
        }
    }

    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(raw, new TypeReference<>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<Object> parseJsonArray(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return objectMapper.readValue(raw, new TypeReference<>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }
}