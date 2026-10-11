package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SaasPlan — Plans SaaS du modèle Dual-Market (G1.4 - §30-31)
 * 4 plans : DÉCOUVERTE / DÉMARRAGE / CROISSANCE / RÉSEAU & CAMPUS
 * Prix EUR ◈ FCFA ◈ USD, quotas et crédits IA par plan
 */
@Entity
@Table(name = "saas_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasPlan {

    @Id
    @Column(name = "key", nullable = false, length = 30)
    private String key;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price_monthly")
    private Long priceMonthly;

    @Column(name = "price_yearly")
    private Long priceYearly;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "limits_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String limitsJson;

    @Column(name = "features_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String featuresJson;

    @Column(name = "stripe_price_id_monthly")
    private String stripePriceIdMonthly;

    @Column(name = "stripe_price_id_yearly")
    private String stripePriceIdYearly;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // ===== G1.4 - champs manquants =====

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "seats_limit")
    private Integer seatsLimit;

    @Column(name = "storage_limit_mb")
    private Integer storageLimitMb;

    @Column(name = "modules_included_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String modulesIncludedJson;

    @Column(name = "ai_credits_limit")
    private Integer aiCreditsLimit;

    @Column(name = "price_eur")
    private Long priceEur;

    @Column(name = "price_xaf")
    private Long priceXaf;

    @Column(name = "price_usd")
    private Long priceUsd;

    @Column(name = "billing_period", length = 20)
    private String billingPeriod;

    @Column(name = "trial_days")
    @Builder.Default
    private Integer trialDays = 30;

    @Column(name = "annual_discount_pct")
    @Builder.Default
    private Integer annualDiscountPct = 17;

    @Column(name = "regions_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String regionsJson;

    @Column(name = "status", length = 20)
    private String status;

    // ===== TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1 (additif, V247) =====
    // Les colonnes price_eur/price_xaf/price_usd ci-dessus restent en place et
    // restent lisibles : le catalogue de référence vit désormais dans
    // saas_plan_prices (un prix = une devise x un cycle x une region x un canal).

    /** Produit Stripe cree une fois par plan ; les Price s'y rattachent. */
    @Column(name = "stripe_product_id")
    private String stripeProductId;

    /** PER_CURRENCY (defaut) ou PER_REGION (grille regionale prioritaire). */
    @Column(name = "pricing_model", length = 20, nullable = false)
    @Builder.Default
    private String pricingModel = "PER_CURRENCY";

    /** Nombre maximum de devises affichees sur la carte de plan (lisibilite). */
    @Column(name = "max_currencies", nullable = false)
    @Builder.Default
    private Integer maxCurrencies = 3;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.currency == null) this.currency = "XAF";
        if (this.isActive == null) this.isActive = true;
        if (this.isPublic == null) this.isPublic = true;
        if (this.trialDays == null) this.trialDays = 30;
        if (this.annualDiscountPct == null) this.annualDiscountPct = 17;
        if (this.pricingModel == null) this.pricingModel = "PER_CURRENCY";
        if (this.maxCurrencies == null) this.maxCurrencies = 3;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaasPlan saasPlan = (SaasPlan) o;
        return key != null && key.equals(saasPlan.key);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
