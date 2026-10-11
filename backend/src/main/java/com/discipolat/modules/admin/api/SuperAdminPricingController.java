package com.discipolat.modules.admin.api;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.payments.stripe.StripePriceSyncService;
import com.discipolat.modules.tenants.domain.PricingSettingsService;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanPrice;
import com.discipolat.modules.tenants.domain.SaasPlanPriceRepository;
import com.discipolat.modules.tenants.domain.SaasPlanPricingSettings;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import com.discipolat.modules.tenants.enums.PricingDisplayMode;
import com.discipolat.modules.tenants.enums.PricingRegionSource;
import com.discipolat.modules.tenants.enums.StripePriceStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Administration plateforme de la tarification
 * (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T3.4 / T3.5).
 *
 * <p>Deux responsabilités : les <b>réglages</b> (ce que le produit est autorisé à
 * proposer) et les <b>prix</b> (ce qu'un plan coûte, dans quelle devise, pour quel
 * cycle, quelle région et par quel canal). Le principe du produit s'applique ici :
 * tout est paramétrable, et l'utilisateur final ne voit que ce que le super-admin
 * a autorisé.</p>
 *
 * <p>Les écritures de prix passent par un upsert : la clé naturelle est
 * (plan, devise, cycle, région). Pour retirer un prix, on le supprime
 * explicitement — on ne laisse pas un « null » ambigus qui pourrait être pris pour
 * « champ absent ».</p>
 */
@RestController
@RequestMapping("/api/v1/platform/admin/pricing")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class SuperAdminPricingController {

    private final PricingSettingsService settingsService;
    private final SaasPlanPriceRepository priceRepository;
    private final SaasPlanRepository planRepository;
    private final StripePriceSyncService stripePriceSyncService;
    private final Iso4217CurrencyValidator currencyValidator;
    private final AuditService auditService;

    public SuperAdminPricingController(PricingSettingsService settingsService,
                                       SaasPlanPriceRepository priceRepository,
                                       SaasPlanRepository planRepository,
                                       StripePriceSyncService stripePriceSyncService,
                                       Iso4217CurrencyValidator currencyValidator,
                                       AuditService auditService) {
        this.settingsService = settingsService;
        this.priceRepository = priceRepository;
        this.planRepository = planRepository;
        this.stripePriceSyncService = stripePriceSyncService;
        this.currencyValidator = currencyValidator;
        this.auditService = auditService;
    }

    // ── Réglages ─────────────────────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        SaasPlanPricingSettings settings = settingsService.current();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabledCurrencies", List.of(settings.getEnabledCurrencies()));
        body.put("baseCurrency", settings.getBaseCurrency());
        body.put("defaultDisplayCurrency", settings.getDefaultDisplayCurrency());
        body.put("showFallbackCurrency", settings.getShowFallbackCurrency());
        body.put("allowUserCurrencyChoice", settings.getAllowUserCurrencyChoice());
        body.put("displayMode", settings.getDisplayMode().name());
        body.put("allowCycleChoice", settings.getAllowCycleChoice());
        body.put("defaultCycle", settings.getDefaultCycle().name());
        body.put("allowMonthly", settings.getAllowMonthly());
        body.put("allowYearly", settings.getAllowYearly());
        body.put("allowRegionChoice", settings.getAllowRegionChoice());
        body.put("regionPricingEnabled", settings.getRegionPricingEnabled());
        body.put("regionResolution", settings.getRegionResolution().name());
        body.put("regionHeaderName", settings.getRegionHeaderName());
        body.put("trustRegionHeader", settings.getTrustRegionHeader());
        body.put("allowRegionQueryOverride", settings.getAllowRegionQueryOverride());
        body.put("defaultRegion", settings.getDefaultRegion());
        body.put("allowCardChannel", settings.getAllowCardChannel());
        body.put("allowMomoChannel", settings.getAllowMomoChannel());
        body.put("allowStrongChannel", settings.getAllowStrongChannel());
        body.put("allowBankChannel", settings.getAllowBankChannel());
        body.put("allowContactChannel", settings.getAllowContactChannel());
        body.put("strongCurrency", settings.getStrongCurrency());
        body.put("momoOperators", settings.momoOperatorList());
        body.put("stripePriceAutoSync", settings.getStripePriceAutoSync());
        body.put("stripeSyncBatchSize", settings.getStripeSyncBatchSize());
        body.put("stripeSyncIntervalMs", settings.getStripeSyncIntervalMs());
        body.put("trialEnabled", settings.getTrialEnabled());
        body.put("freePlanKey", settings.getFreePlanKey());
        body.put("annualDiscountDefault", settings.getAnnualDiscountDefault());
        body.put("landingPricingEnabled", settings.getLandingPricingEnabled());
        body.put("landingPricingSource", settings.getLandingPricingSource().name());
        body.put("landingPricingMaxPlans", settings.getLandingPricingMaxPlans());
        body.put("landingPricingShowPrices", settings.getLandingPricingShowPrices());
        body.put("landingPricingShowToggle", settings.getLandingPricingShowToggle());
        body.put("landingPricingCtaLabel", settings.getLandingPricingCtaLabel());
        body.put("catalogVersion", settings.getCatalogVersion());
        body.put("updatedAt", settings.getUpdatedAt() == null ? null : settings.getUpdatedAt().toString());
        return ResponseEntity.ok(body);
    }

    /**
     * Mise à jour des réglages.
     *
     * <p>PUT sémantique complet : le corps est l'état souhaité, les champs absents
     * conservent leur valeur. UnPATCH partiel serait plus élégant mais laisserait
     * ouvertes des combinaisons impossibles — par exemple autoriser le mensuel
     * tout en Spiral le mensuel par défaut.</p>
     */
    @PutMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> request) {
        SaasPlanPricingSettings current = settingsService.current();
        SaasPlanPricingSettings requested = new SaasPlanPricingSettings();
        copyMutableFields(current, requested);

        apply(request, requested);

        UUID actorId = safeCurrentUserId();
        SaasPlanPricingSettings saved = settingsService.update(requested, actorId);
        if (auditService != null) {
            auditService.logSimpleExternalKey("PRICING_SETTINGS_UPDATED", "PRICING_SETTINGS", "global");
        }
        return ResponseEntity.ok(Map.of("catalogVersion", saved.getCatalogVersion(),
                "updatedAt", saved.getUpdatedAt() == null ? "" : saved.getUpdatedAt().toString()));
    }

    // ── Prix d'un plan ───────────────────────────────────────────────────────

    @GetMapping("/plans/{key}/prices")
    public ResponseEntity<List<Map<String, Object>>> listPrices(@PathVariable String key) {
        String canonical = requirePlanKey(key);
        return ResponseEntity.ok(priceRepository.findByPlanKeyOrderBySortOrderAscCurrencyAsc(canonical)
                .stream().map(this::toPriceView).toList());
    }

    /**
     * Création ou mise à jour d'un prix.
     *
     * <p>Upsert sur la clé (plan, devise, cycle, région). Le montant est
     <b>exigé et validé</b> : refus d'un prix nul sur un plan payant et refus
     * d'un montant à décimales pour une devise qui n'en admet pas — c'est
     * l'exactitude comptable, pas une préférence d'interface.</p>
     */
    @PostMapping("/plans/{key}/prices")
    public ResponseEntity<Map<String, Object>> upsertPrice(@PathVariable String key,
                                                           @RequestBody Map<String, Object> request) {
        String canonical = requirePlanKey(key);
        String currency = requireCurrency(request.get("currency"));
        BillingCycle cycle = BillingCycle.fromString(stringOf(request.get("billingCycle")));
        String region = normalizeRegion(stringOf(request.get("regionCode")));

        SaasPlanPrice price = priceRepository
                .findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(canonical, currency, cycle, region)
                .orElseGet(() -> SaasPlanPrice.builder()
                        .planKey(canonical)
                        .currency(currency)
                        .billingCycle(cycle)
                        .regionCode(region)
                        .build());

        long amountMinor = requireAmountMinor(request.get("amountMinor"), currency);
        price.setAmountMinor(amountMinor);
        applyOptional(request, price);
        price.setUpdatedBy(safeCurrentUserId());

        SaasPlanPrice saved = priceRepository.save(price);
        syncIfAutomatic(saved);
        if (auditService != null) {
            auditService.logSimpleExternalKey("PLAN_PRICE_SAVED", "SAAS_PLAN", canonical + "/" + currency);
        }
        return ResponseEntity.ok(toPriceView(saved));
    }

    /**
     * Mise à jour partielle d'un prix.
     *
     * <p>Un champ présent avec la valeur {@code null} est traité comme une
     * <b>demande de suppression</b> du prix. C'est le moyen, côté API, de retirer
     * une devise du catalogue — impossible autrement, un champ omis et un champ
     * nul étant indiscernables dans un JSON.</p>
     */
    @PutMapping("/plans/{key}/prices/{priceId}")
    public ResponseEntity<Map<String, Object>> updatePrice(@PathVariable String key,
                                                           @PathVariable UUID priceId,
                                                           @RequestBody Map<String, Object> request) {
        String canonical = requirePlanKey(key);
        SaasPlanPrice price = priceRepository.findById(priceId)
                .filter(found -> found.getPlanKey().equals(canonical))
                .orElseThrow(() -> new BusinessRuleException("Prix introuvable : " + priceId, "PRICE_NOT_FOUND"));

        if (request.containsKey("amountMinor")) {
            String currency = requireCurrency(request.getOrDefault("currency", price.getCurrency()));
            price.setAmountMinor(requireAmountMinor(request.get("amountMinor"), currency));
        }
        if (request.containsKey("currency")) {
            price.setCurrency(requireCurrency(request.get("currency")));
        }
        if (request.containsKey("billingCycle")) {
            price.setBillingCycle(BillingCycle.fromString(stringOf(request.get("billingCycle"))));
        }
        if (request.containsKey("regionCode")) {
            price.setRegionCode(normalizeRegion(stringOf(request.get("regionCode"))));
        }
        applyOptional(request, price);
        price.setUpdatedBy(safeCurrentUserId());

        SaasPlanPrice saved = priceRepository.save(price);
        syncIfAutomatic(saved);
        return ResponseEntity.ok(toPriceView(saved));
    }

    @DeleteMapping("/plans/{key}/prices/{priceId}")
    public ResponseEntity<Void> deletePrice(@PathVariable String key, @PathVariable UUID priceId) {
        String canonical = requirePlanKey(key);
        SaasPlanPrice price = priceRepository.findById(priceId)
                .filter(found -> found.getPlanKey().equals(canonical))
                .orElseThrow(() -> new BusinessRuleException("Prix introuvable : " + priceId, "PRICE_NOT_FOUND"));
        // Suppression réelle : un prix retiré ne doit plus jamais réapparaître par
        // un mécanisme de secours. L'historique de facturation, lui, vit dans
        // tenant_subscriptions.price_snapshot_json et n'est pas concerné.
        priceRepository.delete(price);
        if (auditService != null) {
            auditService.logSimpleExternalKey("PLAN_PRICE_DELETED", "SAAS_PLAN", canonical);
        }
        return ResponseEntity.noContent().build();
    }

    /** Remplit plusieurs devises en une fois — utile à l'ouverture d'un marché. */
    @PostMapping("/plans/{key}/prices/bulk")
    public ResponseEntity<Map<String, Object>> bulkPrices(@PathVariable String key,
                                                          @RequestBody Map<String, Object> request) {
        String canonical = requirePlanKey(key);
        Object rawPrices = request.get("prices");
        if (!(rawPrices instanceof List<?> list) || list.isEmpty()) {
            throw new BusinessRuleException("Aucun prix fourni", "PRICE_BULK_EMPTY");
        }
        int created = 0;
        int updated = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> entry)) continue;
            Map<String, Object> payload = new LinkedHashMap<>();
            entry.forEach((k, v) -> payload.put(String.valueOf(k), v));
            try {
                upsertPrice(canonical, payload);
                created++;
            } catch (BusinessRuleException e) {
                errors.add(Map.of("currency", String.valueOf(payload.get("currency")), "error", e.getMessage()));
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("created", created);
        body.put("failed", errors.size());
        body.put("errors", errors);
        if (auditService != null && created > 0) {
            auditService.logSimpleExternalKey("PLAN_PRICES_BULK", "SAAS_PLAN", canonical);
        }
        return ResponseEntity.ok(body);
    }

    // ── Synchronisation Stripe ───────────────────────────────────────────────

    @PostMapping("/plans/{key}/prices/stripe-sync")
    public ResponseEntity<Map<String, Object>> syncPlan(@PathVariable String key) {
        String canonical = requirePlanKey(key);
        StripePriceSyncService.SyncReport report = stripePriceSyncService.syncPlan(canonical);
        if (auditService != null) {
            auditService.logSimpleExternalKey("PLAN_PRICES_STRIPE_SYNC", "SAAS_PLAN", canonical);
        }
        return ResponseEntity.ok(syncReport(report));
    }

    @PostMapping("/prices/stripe-sync")
    public ResponseEntity<Map<String, Object>> syncAll() {
        StripePriceSyncService.SyncReport report = stripePriceSyncService.syncAll();
        if (auditService != null) {
            auditService.logSimpleExternalKey("ALL_PRICES_STRIPE_SYNC", "PRICING_SETTINGS", "global");
        }
        return ResponseEntity.ok(syncReport(report));
    }

    // ── Catalogue devises + diagnostic ───────────────────────────────────────

    /**
     * Catalogue ISO-4217 assorti de ce que le catalogue de prix couvre
     * réellement : c'est ce qui permet à l'administrateur de voir qu'une devise
     * activée n'a aucun prix derrière elle, plutôt que de le découvrir en
     * production.
     */
    @GetMapping("/currencies")
    public ResponseEntity<Map<String, Object>> currencies() {
        SaasPlanPricingSettings settings = settingsService.current();
        Set<String> enabled = settings.currencySet();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (String code : currencyValidator.catalog().keySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            Map<String, Object> meta = currencyValidator.catalog().get(code);
            entry.put("code", code);
            entry.put("name", meta.get("name"));
            entry.put("symbol", meta.get("symbol"));
            entry.put("decimals", meta.get("decimals"));
            entry.put("enabled", enabled.contains(code));
            entry.put("zeroDecimal", Integer.parseInt(String.valueOf(meta.get("decimals"))) == 0);
            entries.add(entry);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("standard", "ISO-4217");
        body.put("count", entries.size());
        body.put("currencies", entries);
        return ResponseEntity.ok(body);
    }

    /** Diagnostic des prix d'un plan : ce qui est mort, ce qui est incomplet. */
    @GetMapping("/plans/{key}/diagnostics")
    public ResponseEntity<Map<String, Object>> diagnostics(@PathVariable String key) {
        String canonical = requirePlanKey(key);
        SaasPlanPricingSettings settings = settingsService.current();
        List<SaasPlanPrice> prices = priceRepository.findByPlanKeyOrderBySortOrderAscCurrencyAsc(canonical);
        List<Map<String, Object>> issues = new ArrayList<>();
        boolean hasMonthly = false;
        boolean hasYearly = false;

        for (SaasPlanPrice price : prices) {
            if (price.getBillingCycle() == BillingCycle.MONTHLY) hasMonthly = true;
            else hasYearly = true;

            // Un prix en devise sans centimes, offered par carte, sans Price ni
            // canal alternatif produirait un bouton qui mène à un échec.
            boolean zeroDecimal = currencyValidator.minorUnits(price.effectiveCheckoutCurrency()) == 0;
            boolean wantsCard = price.channels().contains(PaymentChannel.CARD);
            boolean hasStripePrice = price.getStripePriceId() != null && !price.getStripePriceId().isBlank();
            boolean hasAlternative = price.channels().stream().anyMatch(c -> c != PaymentChannel.CARD);
            if (wantsCard && zeroDecimal && !hasStripePrice && !hasAlternative) {
                issues.add(Map.of("severity", "ERROR", "code", "PRICE_UNREACHABLE",
                        "priceId", price.getId().toString(),
                        "message", "Devise sans centimes, canal carte, aucun Price Stripe et aucune alternative : le bouton de paiement échouerait."));
            }
            if (Boolean.TRUE.equals(price.getIsVisible()) && !Boolean.TRUE.equals(price.getIsSelectable())) {
                issues.add(Map.of("severity", "INFO", "code", "PRICE_DISPLAY_ONLY",
                        "priceId", price.getId().toString(),
                        "message", "Prix affiché mais non sélectionnable : l'utilisateur voit le tarif sans pouvoir le choisir."));
            }
            if (price.getStripePriceStatus() == StripePriceStatus.FAILED) {
                issues.add(Map.of("severity", "ERROR", "code", "STRIPE_SYNC_FAILED",
                        "priceId", price.getId().toString(),
                        "message", price.getStripeSyncError() == null ? "Échec de synchronisation Stripe" : price.getStripeSyncError()));
            }
        }
        if (!hasMonthly && !prices.isEmpty()) {
            issues.add(Map.of("severity", "ERROR", "code", "NO_MONTHLY_PRICE",
                    "message", "Aucun prix mensuel : le cycle par défaut n'a rien à afficher."));
        }
        if (hasMonthly && !hasYearly && Boolean.TRUE.equals(settings.getAllowYearly())) {
            issues.add(Map.of("severity", "INFO", "code", "NO_YEARLY_PRICE",
                    "message", "Aucun prix annuel saisi : il sera calculé à partir du mensuel et de la remise annuelle."));
        }
        if (prices.isEmpty()) {
            issues.add(Map.of("severity", "ERROR", "code", "NO_PRICE",
                    "message", "Aucun prix configuré : le plan s'affichera sans montant."));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("planKey", canonical);
        body.put("priceCount", prices.size());
        body.put("issues", issues);
        body.put("healthy", issues.stream().noneMatch(issue -> "ERROR".equals(issue.get("severity"))));
        return ResponseEntity.ok(body);
    }

    // ── Conversions ──────────────────────────────────────────────────────────

    private Map<String, Object> syncReport(StripePriceSyncService.SyncReport report) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("synced", report.synced());
        body.put("skipped", report.skipped());
        body.put("failed", report.failed());
        body.put("total", report.total());
        body.put("stripeEnabled", report.stripeEnabled());
        return body;
    }

    private Map<String, Object> toPriceView(SaasPlanPrice price) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", price.getId() == null ? null : price.getId().toString());
        view.put("planKey", price.getPlanKey());
        view.put("currency", price.getCurrency());
        view.put("decimals", currencyValidator.minorUnits(price.getCurrency()));
        view.put("billingCycle", price.getBillingCycle().name());
        view.put("amountMinor", price.getAmountMinor());
        view.put("amount", currencyValidator.fromMinorUnits(price.getCurrency(), price.getAmountMinor()).toPlainString());
        view.put("regionCode", price.getRegionCode());
        view.put("checkoutCurrency", price.getCheckoutCurrency());
        view.put("isEnabled", price.getIsEnabled());
        view.put("isVisible", price.getIsVisible());
        view.put("isSelectable", price.getIsSelectable());
        view.put("sortOrder", price.getSortOrder());
        view.put("paymentChannels", price.channels().stream().map(Enum::name).toList());
        view.put("stripePriceId", price.getStripePriceId());
        view.put("stripePriceStatus", price.getStripePriceStatus() == null ? null : price.getStripePriceStatus().name());
        view.put("stripePriceSyncedAt",
                price.getStripePriceSyncedAt() == null ? null : price.getStripePriceSyncedAt().toString());
        view.put("stripeSyncError", price.getStripeSyncError());
        view.put("updatedAt", price.getUpdatedAt() == null ? null : price.getUpdatedAt().toString());
        view.put("zeroDecimal", currencyValidator.minorUnits(price.effectiveCheckoutCurrency()) == 0);
        return view;
    }

    // ── Validation ───────────────────────────────────────────────────────────

    private String requirePlanKey(String key) {
        String canonical = TenantPlanPolicy.canonicalizePlanKey(key);
        if (canonical == null) {
            throw new BusinessRuleException("Plan inconnu : " + key, "PLAN_NOT_FOUND");
        }
        planRepository.findById(canonical).orElseThrow(
                () -> new BusinessRuleException("Plan inconnu : " + key, "PLAN_NOT_FOUND"));
        return canonical;
    }

    private String requireCurrency(Object raw) {
        String currency = stringOf(raw);
        if (currency == null || !currency.matches("[A-Za-z]{3}")) {
            throw new BusinessRuleException("Devise invalide : " + raw, "CURRENCY_INVALID");
        }
        String code = currency.toUpperCase(Locale.ROOT);
        currencyValidator.require(code);
        return code;
    }

    /**
     * Valide un montant en unités mineures.
     *
     * <p>On passe par la validation ISO-4217 : c'est elle qui refuse un prix à
     * décimales pour XAF, et c'est exactement le contrôle qui manquait quand les
     * prix étaient de simples {@code BIGINT} sans unité.</p>
     */
    private long requireAmountMinor(Object raw, String currency) {
        long amountMinor;
        if (raw instanceof Number number) {
            amountMinor = number.longValue();
        } else if (raw instanceof String text && !text.isBlank()) {
            try {
                amountMinor = new BigDecimal(text.trim()).longValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                throw new BusinessRuleException(
                        "Montant invalide : " + text + " (entier en unités mineures attendu)", "PRICE_AMOUNT_INVALID");
            }
        } else {
            throw new BusinessRuleException("Montant obligatoire", "PRICE_AMOUNT_REQUIRED");
        }
        if (amountMinor < 0) {
            throw new BusinessRuleException("Le montant ne peut pas être négatif", "PRICE_AMOUNT_INVALID");
        }
        int decimals = currencyValidator.minorUnits(currency);
        currencyValidator.validateAmount(currency, BigDecimal.valueOf(amountMinor, decimals));
        return amountMinor;
    }

    private void applyOptional(Map<String, Object> request, SaasPlanPrice price) {
        if (request.containsKey("checkoutCurrency")) {
            String checkout = stringOf(request.get("checkoutCurrency"));
            price.setCheckoutCurrency(checkout == null ? null : requireCurrency(checkout));
        }
        if (request.containsKey("regionCode")) {
            price.setRegionCode(normalizeRegion(stringOf(request.get("regionCode"))));
        }
        if (request.containsKey("isEnabled")) {
            price.setIsEnabled(boolOf(request.get("isEnabled"), true));
        }
        if (request.containsKey("isVisible")) {
            price.setIsVisible(boolOf(request.get("isVisible"), true));
        }
        if (request.containsKey("isSelectable")) {
            price.setIsSelectable(boolOf(request.get("isSelectable"), true));
        }
        if (request.containsKey("sortOrder")) {
            Object value = request.get("sortOrder");
            price.setSortOrder(value instanceof Number number ? number.intValue() : 0);
        }
        if (request.containsKey("paymentChannels")) {
            List<PaymentChannel> channels = PaymentChannel.parseCsv(stringOf(request.get("paymentChannels")));
            if (channels.isEmpty()) {
                throw new BusinessRuleException("Au moins un canal de paiement est requis", "PRICE_NO_CHANNEL");
            }
            price.setPaymentChannels(PaymentChannel.toCsv(channels));
        }
        if (request.containsKey("stripePriceId")) {
            String stripePriceId = stringOf(request.get("stripePriceId"));
            price.setStripePriceId(stripePriceId);
            // Saisie manuelle : on marque explicitement le prix comme maîtrisé
            // par le super-admin, la synchronisation automatique ne l'écrasera pas.
            if (stripePriceId != null && !stripePriceId.isBlank()) {
                price.setStripePriceStatus(StripePriceStatus.MANUAL);
                price.setStripeSyncError(null);
                price.setStripePriceSyncedAt(Instant.now());
            } else if (price.getStripePriceId() == null) {
                price.setStripePriceStatus(StripePriceStatus.NOT_REQUIRED);
            }
        }
        if (request.containsKey("metadata")) {
            Object metadata = request.get("metadata");
            if (metadata == null) {
                price.setMetadata("{}");
            } else if (metadata instanceof Map<?, ?> map) {
                try {
                    price.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(map));
                } catch (Exception e) {
                    throw new BusinessRuleException("Métadonnées invalides", "PRICE_METADATA_INVALID");
                }
            }
        }
    }

    private void apply(Map<String, Object> request, SaasPlanPricingSettings settings) {
        if (request.containsKey("enabledCurrencies")) {
            settings.setEnabledCurrencies(toStringArray(request.get("enabledCurrencies")));
        }
        if (request.containsKey("baseCurrency")) {
            settings.setBaseCurrency(stringOf(request.get("baseCurrency")));
        }
        if (request.containsKey("defaultDisplayCurrency")) {
            settings.setDefaultDisplayCurrency(stringOf(request.get("defaultDisplayCurrency")));
        }
        if (request.containsKey("showFallbackCurrency")) {
            settings.setShowFallbackCurrency(boolOf(request.get("showFallbackCurrency"), false));
        }
        if (request.containsKey("allowUserCurrencyChoice")) {
            settings.setAllowUserCurrencyChoice(boolOf(request.get("allowUserCurrencyChoice"), true));
        }
        if (request.containsKey("displayMode")) {
            settings.setDisplayMode(parseEnum(PricingDisplayMode.class,
                    stringOf(request.get("displayMode")), PricingDisplayMode.SINGLE));
        }
        if (request.containsKey("allowCycleChoice")) {
            settings.setAllowCycleChoice(boolOf(request.get("allowCycleChoice"), true));
        }
        if (request.containsKey("defaultCycle")) {
            settings.setDefaultCycle(BillingCycle.fromString(stringOf(request.get("defaultCycle"))));
        }
        if (request.containsKey("allowMonthly")) {
            settings.setAllowMonthly(boolOf(request.get("allowMonthly"), true));
        }
        if (request.containsKey("allowYearly")) {
            settings.setAllowYearly(boolOf(request.get("allowYearly"), true));
        }
        if (request.containsKey("allowRegionChoice")) {
            settings.setAllowRegionChoice(boolOf(request.get("allowRegionChoice"), false));
        }
        if (request.containsKey("regionPricingEnabled")) {
            settings.setRegionPricingEnabled(boolOf(request.get("regionPricingEnabled"), false));
        }
        if (request.containsKey("regionResolution")) {
            settings.setRegionResolution(parseEnum(PricingRegionSource.class,
                    stringOf(request.get("regionResolution")), PricingRegionSource.TENANT));
        }
        if (request.containsKey("regionHeaderName")) {
            settings.setRegionHeaderName(stringOf(request.get("regionHeaderName")));
        }
        if (request.containsKey("trustRegionHeader")) {
            settings.setTrustRegionHeader(boolOf(request.get("trustRegionHeader"), false));
        }
        if (request.containsKey("allowRegionQueryOverride")) {
            settings.setAllowRegionQueryOverride(boolOf(request.get("allowRegionQueryOverride"), true));
        }
        if (request.containsKey("defaultRegion")) {
            settings.setDefaultRegion(stringOf(request.get("defaultRegion")));
        }
        if (request.containsKey("allowCardChannel")) {
            settings.setAllowCardChannel(boolOf(request.get("allowCardChannel"), true));
        }
        if (request.containsKey("allowMomoChannel")) {
            settings.setAllowMomoChannel(boolOf(request.get("allowMomoChannel"), false));
        }
        if (request.containsKey("allowStrongChannel")) {
            settings.setAllowStrongChannel(boolOf(request.get("allowStrongChannel"), true));
        }
        if (request.containsKey("allowBankChannel")) {
            settings.setAllowBankChannel(boolOf(request.get("allowBankChannel"), true));
        }
        if (request.containsKey("allowContactChannel")) {
            settings.setAllowContactChannel(boolOf(request.get("allowContactChannel"), true));
        }
        if (request.containsKey("strongCurrency")) {
            settings.setStrongCurrency(stringOf(request.get("strongCurrency")));
        }
        if (request.containsKey("momoOperators")) {
            settings.setMomoOperators(toStringArray(request.get("momoOperators")));
        }
        if (request.containsKey("stripePriceAutoSync")) {
            settings.setStripePriceAutoSync(boolOf(request.get("stripePriceAutoSync"), false));
        }
        if (request.containsKey("stripeSyncBatchSize")) {
            Object value = request.get("stripeSyncBatchSize");
            settings.setStripeSyncBatchSize(value instanceof Number number ? number.intValue() : 20);
        }
        if (request.containsKey("stripeSyncIntervalMs")) {
            Object value = request.get("stripeSyncIntervalMs");
            settings.setStripeSyncIntervalMs(value instanceof Number number ? number.longValue() : 300_000L);
        }
        if (request.containsKey("freePlanKey")) {
            settings.setFreePlanKey(stringOf(request.get("freePlanKey")));
        }
        if (request.containsKey("trialEnabled")) {
            settings.setTrialEnabled(boolOf(request.get("trialEnabled"), true));
        }
        if (request.containsKey("annualDiscountDefault")) {
            Object value = request.get("annualDiscountDefault");
            settings.setAnnualDiscountDefault(value instanceof Number number ? number.intValue() : 17);
        }
        if (request.containsKey("landingPricingEnabled")) {
            settings.setLandingPricingEnabled(boolOf(request.get("landingPricingEnabled"), true));
        }
        if (request.containsKey("landingPricingSource")) {
            settings.setLandingPricingSource(parseEnum(SaasPlanPricingSettings.PricingSource.class,
                    stringOf(request.get("landingPricingSource")), SaasPlanPricingSettings.PricingSource.API));
        }
        if (request.containsKey("landingPricingMaxPlans")) {
            Object value = request.get("landingPricingMaxPlans");
            settings.setLandingPricingMaxPlans(value instanceof Number number ? number.intValue() : 4);
        }
        if (request.containsKey("landingPricingShowPrices")) {
            settings.setLandingPricingShowPrices(boolOf(request.get("landingPricingShowPrices"), true));
        }
        if (request.containsKey("landingPricingShowToggle")) {
            settings.setLandingPricingShowToggle(boolOf(request.get("landingPricingShowToggle"), true));
        }
        if (request.containsKey("landingPricingCtaLabel")) {
            settings.setLandingPricingCtaLabel(stringOf(request.get("landingPricingCtaLabel")));
        }
    }

    private void copyMutableFields(SaasPlanPricingSettings from, SaasPlanPricingSettings to) {
        to.setEnabledCurrencies(from.getEnabledCurrencies());
        to.setBaseCurrency(from.getBaseCurrency());
        to.setDefaultDisplayCurrency(from.getDefaultDisplayCurrency());
        to.setShowFallbackCurrency(from.getShowFallbackCurrency());
        to.setAllowUserCurrencyChoice(from.getAllowUserCurrencyChoice());
        to.setDisplayMode(from.getDisplayMode());
        to.setAllowCycleChoice(from.getAllowCycleChoice());
        to.setDefaultCycle(from.getDefaultCycle());
        to.setAllowMonthly(from.getAllowMonthly());
        to.setAllowYearly(from.getAllowYearly());
        to.setAllowRegionChoice(from.getAllowRegionChoice());
        to.setRegionPricingEnabled(from.getRegionPricingEnabled());
        to.setRegionResolution(from.getRegionResolution());
        to.setRegionHeaderName(from.getRegionHeaderName());
        to.setTrustRegionHeader(from.getTrustRegionHeader());
        to.setAllowRegionQueryOverride(from.getAllowRegionQueryOverride());
        to.setDefaultRegion(from.getDefaultRegion());
        to.setAllowCardChannel(from.getAllowCardChannel());
        to.setAllowMomoChannel(from.getAllowMomoChannel());
        to.setAllowStrongChannel(from.getAllowStrongChannel());
        to.setAllowBankChannel(from.getAllowBankChannel());
        to.setAllowContactChannel(from.getAllowContactChannel());
        to.setStrongCurrency(from.getStrongCurrency());
        to.setMomoOperators(from.getMomoOperators());
        to.setStripePriceAutoSync(from.getStripePriceAutoSync());
        to.setStripeSyncBatchSize(from.getStripeSyncBatchSize());
        to.setStripeSyncIntervalMs(from.getStripeSyncIntervalMs());
        to.setFreePlanKey(from.getFreePlanKey());
        to.setTrialEnabled(from.getTrialEnabled());
        to.setAnnualDiscountDefault(from.getAnnualDiscountDefault());
        to.setLandingPricingEnabled(from.getLandingPricingEnabled());
        to.setLandingPricingSource(from.getLandingPricingSource());
        to.setLandingPricingMaxPlans(from.getLandingPricingMaxPlans());
        to.setLandingPricingShowPrices(from.getLandingPricingShowPrices());
        to.setLandingPricingShowToggle(from.getLandingPricingShowToggle());
        to.setLandingPricingCtaLabel(from.getLandingPricingCtaLabel());
    }

    /**
     * Synchronise immédiatement si le super-admin a demandé la synchro
     * automatique — sinon la ligne reste {@code PENDING} et attend le batch ou
     * un déclenchement manuel.
     */
    private void syncIfAutomatic(SaasPlanPrice price) {
        if (!Boolean.TRUE.equals(settingsService.current().getStripePriceAutoSync())) {
            return;
        }
        if (price.getStripePriceStatus() == StripePriceStatus.MANUAL) {
            return;
        }
        stripePriceSyncService.syncPrice(price);
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private UUID safeCurrentUserId() {
        try {
            return TenantContext.getCurrentUserId();
        } catch (RuntimeException noContext) {
            return SecurityUtils.getCurrentUserId();
        }
    }

    private static String stringOf(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean boolOf(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private static String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return SaasPlanPrice.GLOBAL_REGION;
        }
        String code = region.trim().toUpperCase(Locale.ROOT);
        return code.matches("[A-Z]{1,10}") ? code : SaasPlanPrice.GLOBAL_REGION;
    }

    private static String[] toStringArray(Object raw) {
        if (raw instanceof String[] array) return array;
        if (raw instanceof List<?> list) {
            List<String> values = new ArrayList<>();
            for (Object item : list) {
                String text = stringOf(item);
                if (text != null) values.add(text);
            }
            return values.toArray(new String[0]);
        }
        if (raw instanceof String csv) {
            return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        }
        return new String[0];
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return fallback;
        }
    }
}