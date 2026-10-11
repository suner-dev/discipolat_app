package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import com.discipolat.modules.tenants.enums.StripePriceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Prix de catalogue d'un plan, pour une devise, un cycle et une région donnés
 * (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.5).
 *
 * <p>Cette entité remplace le trio figé {@code price_eur} / {@code price_xaf} /
 * {@code price_usd} de {@link SaasPlan} sans le supprimer : on peut donc ajouter
 * une 7ᵉ devise, ou une grille par région, sans migration de plus.</p>
 *
 * <p><b>CONTRAT D'UNITÉS</b> — {@link #amountMinor} est TOUJOURS en unités
 * mineures ISO-4217 (15000 XAF = 15 000 FCFA ; 2900 EUR = 29,00 €). Le nombre de
 * décimales vient exclusivement de
 * {@link com.discipolat.modules.currency.domain.Iso4217CurrencyValidator#minorUnits(String)}.
 * Aucun code ne doit multiplier par 100 : cette table stocke déjà la valeur
 * exacte que les prestataires attendent.</p>
 *
 * <p><b>Pas de filtre multi-tenant</b> : c'est une table de la plateforme, comme
 * {@code saas_plans}. Le prix est global, sa monétisation ne l'est pas.</p>
 */
@Entity
@Table(name = "saas_plan_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasPlanPrice {

    /** Région sentinelle pour un tarif monde entier (jamais {@code null} : cf. V245). */
    public static final String GLOBAL_REGION = "GLOBAL";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "plan_key", nullable = false, length = 30)
    private String planKey;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    /** CONTRAT §2.1 : unités mineures ISO-4217. Ne jamais lire/écrire un montant majeur ici. */
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "region_code", nullable = false, length = 10)
    @Builder.Default
    private String regionCode = GLOBAL_REGION;

    /** Canal STRONG : devise réellement débitée quand elle diffère de la devise d'affichage. */
    @Column(name = "checkout_currency", length = 3)
    private String checkoutCurrency;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /** Affiché sur la page publique et sur la landing. */
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    /** L'utilisateur PEUT choisir ce prix. Un prix visible mais non sélectionnable
     *  sert d'affichage de référence (ex. « contactez-nous pour ce tarif »). */
    @Column(name = "is_selectable", nullable = false)
    @Builder.Default
    private Boolean isSelectable = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "payment_channels", nullable = false, length = 100)
    @Builder.Default
    private String paymentChannels = PaymentChannel.CARD.name();

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stripe_price_status", nullable = false, length = 30)
    @Builder.Default
    private StripePriceStatus stripePriceStatus = StripePriceStatus.NOT_REQUIRED;

    @Column(name = "stripe_price_synced_at")
    private Instant stripePriceSyncedAt;

    @Column(name = "stripe_sync_error", columnDefinition = "TEXT")
    private String stripeSyncError;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String metadata = "{}";

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.regionCode == null || this.regionCode.isBlank()) this.regionCode = GLOBAL_REGION;
        if (this.isEnabled == null) this.isEnabled = true;
        if (this.isVisible == null) this.isVisible = true;
        if (this.isSelectable == null) this.isSelectable = true;
        if (this.sortOrder == null) this.sortOrder = 0;
        if (this.paymentChannels == null || this.paymentChannels.isBlank()) {
            this.paymentChannels = PaymentChannel.CARD.name();
        }
        if (this.stripePriceStatus == null) this.stripePriceStatus = StripePriceStatus.NOT_REQUIRED;
        if (this.metadata == null) this.metadata = "{}";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Canaux dans leur ordre d'affichage (celui du CSV fait foi). */
    public List<PaymentChannel> channels() {
        return PaymentChannel.parseCsv(paymentChannels);
    }

    /** Devise de débit si le canal STRONG est utilisé, sinon la devise d'affichage. */
    public String effectiveCheckoutCurrency() {
        return (checkoutCurrency == null || checkoutCurrency.isBlank()) ? currency : checkoutCurrency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaasPlanPrice other = (SaasPlanPrice) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}