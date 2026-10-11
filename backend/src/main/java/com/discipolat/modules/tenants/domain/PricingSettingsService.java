package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PricingDisplayMode;
import com.discipolat.modules.tenants.enums.PricingRegionSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Lecture et écriture des réglages de tarification
 * (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T2.3).
 *
 * <p>La lecture est mise en cache mémoire et invalidée à chaque écriture : la
 * page publique appelle ces réglages à chaque requête, et une lecture base à
 * chaque affichage de carte serait du gaspillage. L'écriture reste toujours
 * transactionnelle et incrémente {@code catalog_version} pour que les clients
 * sachent quand rafraîchir.</p>
 *
 * <p>Si la ligne singleton est absente (base fraîche, migration jamais passée),
 * on renvoie des réglages par défaut en mémoire plutôt que de planter : la page
 * publique doit rester lisible même en cas de migration incomplète.</p>
 */
@Service
public class PricingSettingsService {

    /** Garde-fou : si l'instance vit plus longtemps que ça sans invalidation, on relit. */
    private static final long MAX_CACHE_NANOS = 30_000_000_000L; // 30 s

    private final SaasPlanPricingSettingsRepository repository;
    private final com.discipolat.modules.currency.domain.Iso4217CurrencyValidator currencyValidator;

    private volatile SaasPlanPricingSettings cached;
    private volatile long cachedAtNanos;

    public PricingSettingsService(SaasPlanPricingSettingsRepository repository,
                                  com.discipolat.modules.currency.domain.Iso4217CurrencyValidator currencyValidator) {
        this.repository = repository;
        this.currencyValidator = currencyValidator;
    }

    /** Réglages effectifs, avec repli sur des valeurs par défaut en mémoire. */
    @Transactional(readOnly = true)
    public SaasPlanPricingSettings current() {
        SaasPlanPricingSettings snapshot = cached;
        if (snapshot != null && (System.nanoTime() - cachedAtNanos) < MAX_CACHE_NANOS) {
            return snapshot;
        }
        SaasPlanPricingSettings loaded = repository.findSingleton().orElseGet(() -> {
            SaasPlanPricingSettings defaults = SaasPlanPricingSettings.builder()
                    .id(SaasPlanPricingSettings.SINGLETON_ID)
                    .build();
            defaults.setUpdatedAt(java.time.Instant.now());
            return defaults;
        });
        cached = loaded;
        cachedAtNanos = System.nanoTime();
        return loaded;
    }

    /** Mise à jour des réglages. Incrémente {@code catalog_version}. */
    @Transactional
    public SaasPlanPricingSettings update(SaasPlanPricingSettings requested, java.util.UUID actorId) {
        SaasPlanPricingSettings settings = repository.findSingleton()
                .orElseGet(() -> SaasPlanPricingSettings.builder()
                        .id(SaasPlanPricingSettings.SINGLETON_ID).build());

        settings.setEnabledCurrencies(requested.getEnabledCurrencies());
        settings.setBaseCurrency(upper(requested.getBaseCurrency()));
        settings.setDefaultDisplayCurrency(upper(requested.getDefaultDisplayCurrency()));
        settings.setShowFallbackCurrency(requested.getShowFallbackCurrency());
        settings.setAllowUserCurrencyChoice(requested.getAllowUserCurrencyChoice());
        settings.setDisplayMode(requested.getDisplayMode());
        settings.setAllowCycleChoice(requested.getAllowCycleChoice());
        settings.setDefaultCycle(requested.getDefaultCycle());
        settings.setAllowMonthly(requested.getAllowMonthly());
        settings.setAllowYearly(requested.getAllowYearly());
        settings.setAllowRegionChoice(requested.getAllowRegionChoice());
        settings.setRegionPricingEnabled(requested.getRegionPricingEnabled());
        settings.setRegionResolution(requested.getRegionResolution());
        settings.setRegionHeaderName(requested.getRegionHeaderName());
        settings.setTrustRegionHeader(requested.getTrustRegionHeader());
        settings.setAllowRegionQueryOverride(requested.getAllowRegionQueryOverride());
        settings.setDefaultRegion(upper(requested.getDefaultRegion()));
        settings.setAllowCardChannel(requested.getAllowCardChannel());
        settings.setAllowMomoChannel(requested.getAllowMomoChannel());
        settings.setAllowStrongChannel(requested.getAllowStrongChannel());
        settings.setAllowBankChannel(requested.getAllowBankChannel());
        settings.setAllowContactChannel(requested.getAllowContactChannel());
        settings.setStrongCurrency(upper(requested.getStrongCurrency()));
        settings.setMomoOperators(requested.getMomoOperators());
        settings.setStripePriceAutoSync(requested.getStripePriceAutoSync());
        settings.setStripeSyncBatchSize(requested.getStripeSyncBatchSize());
        settings.setStripeSyncIntervalMs(requested.getStripeSyncIntervalMs());
        settings.setTrialEnabled(requested.getTrialEnabled());
        settings.setAnnualDiscountDefault(requested.getAnnualDiscountDefault());
        settings.setLandingPricingEnabled(requested.getLandingPricingEnabled());
        settings.setLandingPricingSource(requested.getLandingPricingSource());
        settings.setLandingPricingMaxPlans(requested.getLandingPricingMaxPlans());
        settings.setLandingPricingShowPrices(requested.getLandingPricingShowPrices());
        settings.setLandingPricingShowToggle(requested.getLandingPricingShowToggle());
        settings.setLandingPricingCtaLabel(requested.getLandingPricingCtaLabel());

        validate(settings);
        settings.setUpdatedBy(actorId);
        settings.setCatalogVersion((settings.getCatalogVersion() == null ? 0 : settings.getCatalogVersion()) + 1);

        SaasPlanPricingSettings saved = repository.save(settings);
        invalidate();
        return saved;
    }

    /** Invalidation du cache mémoire — appelée après toute écriture. */
    public void invalidate() {
        cached = null;
        cachedAtNanos = 0L;
    }

    /**
     * Garde-fous fonctionnels.
     *
     * <p>On refuse les réglages qui produiraient une incohérence visible, pas
     * ceux qui sont simplement inhabituels : un super-admin doit pouvoir faire
     * des choix absurdes, mais pas se laisser piéger par un état impossible.</p>
     */
    void validate(SaasPlanPricingSettings settings) {
        Set<String> enabled = settings.currencySet();
        if (enabled.isEmpty()) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Activez au moins une devise", "PRICING_NO_CURRENCY_ENABLED");
        }
        for (String currency : enabled) {
            requireKnownCurrency(currency);
        }
        requireInEnabled(settings.getBaseCurrency(), enabled, "devise de base");
        requireInEnabled(settings.getDefaultDisplayCurrency(), enabled, "devise d'affichage par défaut");

        if (settings.getAllowMonthly() == null || settings.getAllowMonthly() == Boolean.FALSE) {
            if (settings.getAllowYearly() == null || settings.getAllowYearly() == Boolean.FALSE) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "Autorisez au moins un cycle de facturation (mensuel ou annuel)",
                        "PRICING_NO_CYCLE_ENABLED");
            }
            if (settings.getDefaultCycle() == BillingCycle.MONTHLY) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "Le cycle par défaut est mensuel alors que le mensuel est interdit",
                        "PRICING_DEFAULT_CYCLE_FORBIDDEN");
            }
        }
        if (settings.getDefaultRegion() == null || settings.getDefaultRegion().isBlank()) {
            settings.setDefaultRegion("AF");
        }
        if (settings.getDisplayMode() == null) {
            settings.setDisplayMode(PricingDisplayMode.SINGLE);
        }
        if (settings.getRegionResolution() == null) {
            settings.setRegionResolution(PricingRegionSource.TENANT);
        }
        if (settings.getStripeSyncBatchSize() == null || settings.getStripeSyncBatchSize() < 1) {
            settings.setStripeSyncBatchSize(20);
        }
        if (settings.getStripeSyncBatchSize() > 200) {
            settings.setStripeSyncBatchSize(200);
        }
        if (settings.getStripeSyncIntervalMs() == null || settings.getStripeSyncIntervalMs() < 10_000L) {
            settings.setStripeSyncIntervalMs(300_000L);
        }
        if (settings.getLandingPricingMaxPlans() == null
                || settings.getLandingPricingMaxPlans() < 1
                || settings.getLandingPricingMaxPlans() > 12) {
            settings.setLandingPricingMaxPlans(4);
        }
        int discount = settings.getAnnualDiscountDefault() == null ? 0 : settings.getAnnualDiscountDefault();
        if (discount < 0 || discount > 90) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "La remise annuelle doit être comprise entre 0 et 90 %",
                    "PRICING_DISCOUNT_OUT_OF_RANGE");
        }
        // Un plan affichable sans aucun canal de paiement afficherait un CTA mort.
        if (noChannelAllowed(settings)) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Activez au moins un canal de paiement (carte, Mobile Money, devise forte, virement ou contact)",
                    "PRICING_NO_CHANNEL_ENABLED");
        }
    }

    private void requireKnownCurrency(String currency) {
        if (currency == null || currency.isBlank()) return;
        if (!currencyValidator.isSupported(currency)) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Devise inconnue du référentiel ISO-4217 : " + currency, "PRICING_UNKNOWN_CURRENCY");
        }
    }

    private void requireInEnabled(String currency, Set<String> enabled, String label) {
        String code = upper(currency);
        if (code == null || !enabled.contains(code)) {
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "La " + label + " (" + currency + ") doit figurer parmi les devises activées",
                    "PRICING_CURRENCY_NOT_ENABLED");
        }
    }

    private boolean noChannelAllowed(SaasPlanPricingSettings settings) {
        return !isTrue(settings.getAllowCardChannel())
                && !isTrue(settings.getAllowMomoChannel())
                && !isTrue(settings.getAllowStrongChannel())
                && !isTrue(settings.getAllowBankChannel())
                && !isTrue(settings.getAllowContactChannel());
    }

    private static boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /** Accès direct à la ligne persistée (usage interne / super-admin). */
    @Transactional(readOnly = true)
    public Optional<SaasPlanPricingSettings> persisted() {
        return repository.findSingleton();
    }
}