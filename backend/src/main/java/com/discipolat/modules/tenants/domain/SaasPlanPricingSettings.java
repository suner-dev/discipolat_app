package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PricingDisplayMode;
import com.discipolat.modules.tenants.enums.PricingRegionSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Réglages plateforme de la tarification — table SINGLETON ({@code id = 1}).
 *
 * <p>TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.5. Centralise <b>toute</b> la
 * paramétration : devises autorisées, mode d'affichage, politique régionale,
 * canaux de paiement, synchronisation Stripe, essai/remise, section landing.</p>
 *
 * <p>La règle du produit est simple : <b>le super-admin décide de tout</b>. Rien
 * ici n'est imposé à l'utilisateur final au-delà de ce que le super-admin a
 * explicitement autorisé (choix de devise, choix de cycle, choix de région).</p>
 *
 * <p><b>Pas de filtre multi-tenant</b> : c'est une table de la plateforme.</p>
 */
@Entity
@Table(name = "saas_pricing_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasPlanPricingSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id")
    private Short id;

    // ── Devises ───────────────────────────────────────────────────────────────

    @Column(name = "base_currency", nullable = false, length = 3)
    @Builder.Default
    private String baseCurrency = "XAF";

    @Column(name = "default_display_currency", nullable = false, length = 3)
    @Builder.Default
    private String defaultDisplayCurrency = "XAF";

    @Column(name = "enabled_currencies", nullable = false, columnDefinition = "char(3)[]")
    @Builder.Default
    private String[] enabledCurrencies = {"XAF", "XOF", "EUR", "USD", "CAD", "GBP", "CHF"};

    /** Repli sur {@link #baseCurrency} quand la devise demandée n'a pas de prix.
     *  {@code false} par défaut : mieux vaut « tarif non communiqué » qu'un prix inventé. */
    @Column(name = "show_fallback_currency", nullable = false)
    @Builder.Default
    private Boolean showFallbackCurrency = false;

    @Column(name = "allow_user_currency_choice", nullable = false)
    @Builder.Default
    private Boolean allowUserCurrencyChoice = true;

    // ── Modes d'affichage ─────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", nullable = false, length = 30)
    @Builder.Default
    private PricingDisplayMode displayMode = PricingDisplayMode.SINGLE;

    @Column(name = "allow_cycle_choice", nullable = false)
    @Builder.Default
    private Boolean allowCycleChoice = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_cycle", nullable = false, length = 20)
    @Builder.Default
    private BillingCycle defaultCycle = BillingCycle.MONTHLY;

    @Column(name = "allow_monthly", nullable = false)
    @Builder.Default
    private Boolean allowMonthly = true;

    @Column(name = "allow_yearly", nullable = false)
    @Builder.Default
    private Boolean allowYearly = true;

    @Column(name = "allow_region_choice", nullable = false)
    @Builder.Default
    private Boolean allowRegionChoice = false;

    // ── Régions ───────────────────────────────────────────────────────────────

    @Column(name = "region_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean regionPricingEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_resolution", nullable = false, length = 30)
    @Builder.Default
    private PricingRegionSource regionResolution = PricingRegionSource.TENANT;

    @Column(name = "region_header_name", nullable = false, length = 64)
    @Builder.Default
    private String regionHeaderName = "X-Discipolat-Region";

    @Column(name = "trust_region_header", nullable = false)
    @Builder.Default
    private Boolean trustRegionHeader = false;

    @Column(name = "allow_region_query_override", nullable = false)
    @Builder.Default
    private Boolean allowRegionQueryOverride = true;

    @Column(name = "default_region", nullable = false, length = 10)
    @Builder.Default
    private String defaultRegion = "AF";

    // ── Canaux de paiement ────────────────────────────────────────────────────

    @Column(name = "allow_card_channel", nullable = false)
    @Builder.Default
    private Boolean allowCardChannel = true;

    @Column(name = "allow_momo_channel", nullable = false)
    @Builder.Default
    private Boolean allowMomoChannel = false;

    @Column(name = "allow_strong_channel", nullable = false)
    @Builder.Default
    private Boolean allowStrongChannel = true;

    @Column(name = "allow_bank_channel", nullable = false)
    @Builder.Default
    private Boolean allowBankChannel = true;

    @Column(name = "allow_contact_channel", nullable = false)
    @Builder.Default
    private Boolean allowContactChannel = true;

    @Column(name = "strong_currency", nullable = false, length = 3)
    @Builder.Default
    private String strongCurrency = "EUR";

    @Column(name = "momo_operators", nullable = false, columnDefinition = "varchar(50)[]")
    @Builder.Default
    private String[] momoOperators = {"MTN_MOMO", "ORANGE_MONEY", "M_PESA"};

    // ── Stripe ────────────────────────────────────────────────────────────────

    @Column(name = "stripe_price_auto_sync", nullable = false)
    @Builder.Default
    private Boolean stripePriceAutoSync = false;

    @Column(name = "stripe_sync_batch_size", nullable = false)
    @Builder.Default
    private Integer stripeSyncBatchSize = 20;

    @Column(name = "stripe_sync_interval_ms", nullable = false)
    @Builder.Default
    private Long stripeSyncIntervalMs = 300_000L;

    /**
     * Plan attribue a une inscription en ligne SANS paiement.
     *
     * <p>Un visiteur qui s'inscrit depuis {@code /register?plan=startup} cree un
     * compte, il n'a rien paye. Lui poser un plan payant produirait un tenant
     * que le systeme croit deja abonne — donc impossible a facturer apres coup.
     * On le ramene donc au plan gratuit, et la souscription se fait ensuite par
     * le checkout, ou par le super-admin pour le provisionnement.</p>
     */
    @Column(name = "free_plan_key", nullable = false, length = 30)
    @Builder.Default
    private String freePlanKey = "DISCOVERY";

    // ── Essai et remise ───────────────────────────────────────────────────────

    @Column(name = "trial_enabled", nullable = false)
    @Builder.Default
    private Boolean trialEnabled = true;

    @Column(name = "annual_discount_default", nullable = false)
    @Builder.Default
    private Integer annualDiscountDefault = 17;

    // ── Landing ───────────────────────────────────────────────────────────────

    @Column(name = "landing_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean landingPricingEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "landing_pricing_source", nullable = false, length = 20)
    @Builder.Default
    private PricingSource landingPricingSource = PricingSource.API;

    @Column(name = "landing_pricing_max_plans", nullable = false)
    @Builder.Default
    private Integer landingPricingMaxPlans = 4;

    @Column(name = "landing_pricing_show_prices", nullable = false)
    @Builder.Default
    private Boolean landingPricingShowPrices = true;

    @Column(name = "landing_pricing_show_toggle", nullable = false)
    @Builder.Default
    private Boolean landingPricingShowToggle = true;

    @Column(name = "landing_pricing_cta_label")
    private String landingPricingCtaLabel;

    // ── Journalisation ────────────────────────────────────────────────────────

    /** Incrémenté à chaque écriture : le client public l'interroge pour savoir
     *  s'il doit rafraîchir son cache — pas de WebSocket nécessaire. */
    @Column(name = "catalog_version", nullable = false)
    @Builder.Default
    private Integer catalogVersion = 1;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    /** Source du contenu de la section tarifs de la landing. */
    public enum PricingSource {
        /** Catalogue dynamique — source de vérité unique. */
        API,
        /** 3 tiers « Sur devis » historiques, code en dur. */
        STATIC
    }

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = Instant.now();
        if (this.id == null) this.id = SINGLETON_ID;
        if (this.catalogVersion == null) this.catalogVersion = 1;
        if (this.enabledCurrencies == null || this.enabledCurrencies.length == 0) {
            this.enabledCurrencies = new String[]{"XAF", "XOF", "EUR", "USD", "CAD", "GBP", "CHF"};
        }
        if (this.momoOperators == null) {
            this.momoOperators = new String[0];
        }
        if (this.freePlanKey == null || this.freePlanKey.isBlank()) {
            this.freePlanKey = "DISCOVERY";
        }
    }

    // ── Accesseurs sûrs (les colonnes sont NOT NULL en base, mais une ligne
    //    insérée hors applicatif peut contenir un null) ─────────────────────────

    public boolean isCurrencyEnabled(String currency) {
        if (currency == null) return false;
        String code = currency.trim().toUpperCase(java.util.Locale.ROOT);
        return currencySet().contains(code);
    }

    public boolean isRegionPricingEnabled() {
        return Boolean.TRUE.equals(regionPricingEnabled);
    }

    public boolean isUserCurrencyChoiceAllowed() {
        return Boolean.TRUE.equals(allowUserCurrencyChoice);
    }

    public boolean isCycleChoiceAllowed() {
        return Boolean.TRUE.equals(allowCycleChoice);
    }

    public boolean isRegionChoiceAllowed() {
        return Boolean.TRUE.equals(allowRegionChoice);
    }

    public boolean isFallbackCurrencyAllowed() {
        return Boolean.TRUE.equals(showFallbackCurrency);
    }

    public boolean isTrialEnabled() {
        return Boolean.TRUE.equals(trialEnabled);
    }

    /** Le cycle demandé est-il autorisé par le super-admin ? */
    public boolean isCycleAllowed(BillingCycle cycle) {
        if (cycle == null) return false;
        return cycle == BillingCycle.YEARLY
                ? Boolean.TRUE.equals(allowYearly)
                : Boolean.TRUE.equals(allowMonthly);
    }

    public Set<String> currencySet() {
        if (enabledCurrencies == null) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (String currency : enabledCurrencies) {
            if (currency != null && !currency.isBlank()) {
                result.add(currency.trim().toUpperCase(java.util.Locale.ROOT));
            }
        }
        return Set.copyOf(result);
    }

    public List<String> momoOperatorList() {
        if (momoOperators == null) return List.of();
        return Arrays.stream(momoOperators)
                .filter(operator -> operator != null && !operator.isBlank())
                .map(operator -> operator.trim().toUpperCase(java.util.Locale.ROOT))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id != null && id.equals(((SaasPlanPricingSettings) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}