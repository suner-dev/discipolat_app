package com.discipolat.modules.payments.domain;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.payments.payout.PayoutProvidersProperties;
import com.discipolat.modules.payments.stripe.StripeBillingProperties;
import com.discipolat.modules.tenants.domain.PricingSettingsService;
import com.discipolat.modules.tenants.domain.SaasPlanPrice;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Source de vérité unique sur la disponibilité des canaux de paiement
 * (TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T6.4).
 *
 * <p>Le frontend ne décide <b>jamais</b> seul de la disponibilité d'un canal : il
 * reflète cette liste. Deux raisons, l'une produit et l'une financière :</p>
 * <ul>
 *   <li><b>Produit</b> : afficher « Carte bancaire » quand {@code STRIPE_SECRET_KEY}
 *       est absente produit un bouton mort — c'est exactement le bug
 *       {@code STRIPE_PRICE_ID_REQUIRED} qui rendait STARTUP non souscriptable ;</li>
 *   <li><b>Finance</b> : proposer un canal qui débiterait dans une devise ou avec
 *       un prestataire non configuré, c'est promettre un encaissement qu'on ne
 *       peut pas honorer.</li>
 * </ul>
 *
 * <p>Un canal déclaré mais indisponible est <b>retiré</b> de la liste, pas laissé
 * en place avec une mention d'attente : l'utilisateur voit alors le canal suivant
 * (ou « nous contacter »), ce qui est un parcours honnête.</p>
 */
@Component
public class PaymentChannelResolver {

    /**
     * Devises sans unité mineure : Stripe refuse {@code price_data} inline pour
     * celles-ci (faille F2). Sans Price pré-créé, le canal carte est inutilisable.
     *
     * <p>{@code Set.of} refuse les doublons à la construction — d'où l'absence
     * de XAF en double, et le recours à {@link #isZeroDecimal} qui va de toute
     * façon consulter {@link Iso4217CurrencyValidator} pour les devises
     * moins courantes.</p>
     */
    private static final Set<String> ZERO_DECIMAL = Set.of("XAF", "XOF", "JPY", "KRW", "CLP", "PYG", "RWF", "UGX", "VUV");

    private final PricingSettingsService settingsService;
    private final StripeBillingProperties stripeProperties;
    private final PaymentProviderProperties providerProperties;
    private final MobileMoneyProviderRegistry momoRegistry;
    private final PayoutProvidersProperties payoutProviders;
    private final Iso4217CurrencyValidator currencyValidator;

    public PaymentChannelResolver(PricingSettingsService settingsService,
                                  StripeBillingProperties stripeProperties,
                                  PaymentProviderProperties providerProperties,
                                  MobileMoneyProviderRegistry momoRegistry,
                                  PayoutProvidersProperties payoutProviders,
                                  Iso4217CurrencyValidator currencyValidator) {
        this.settingsService = settingsService;
        this.stripeProperties = stripeProperties;
        this.providerProperties = providerProperties;
        this.momoRegistry = momoRegistry;
        this.payoutProviders = payoutProviders;
        this.currencyValidator = currencyValidator;
    }

    /** Canaux réellement disponibles pour un prix de catalogue. */
    @Transactional(readOnly = true)
    public List<PaymentChannel> available(SaasPlanPrice price) {
        if (price == null) {
            return List.of();
        }
        var settings = settingsService.current();
        List<PaymentChannel> result = new ArrayList<>();
        for (PaymentChannel channel : price.channels()) {
            if (isAvailable(channel, price, settings)) {
                result.add(channel);
            }
        }
        if (result.isEmpty()) {
            // Jamais de page sans issue : on propose le contact si autorisé.
            if (Boolean.TRUE.equals(settings.getAllowContactChannel())) {
                result.add(PaymentChannel.CONTACT);
            }
        }
        return List.copyOf(result);
    }

    /** Canaux disponibles pour un prix déjà résolu (sans la ligne brute). */
    @Transactional(readOnly = true)
    public List<PaymentChannel> available(String currency, String checkoutCurrency,
                                          String stripePriceId, String stripePriceStatus,
                                          List<PaymentChannel> declared) {
        var settings = settingsService.current();
        List<PaymentChannel> result = new ArrayList<>();
        for (PaymentChannel channel : declared == null ? List.<PaymentChannel>of() : declared) {
            if (isAvailable(channel, currency, checkoutCurrency, stripePriceId, stripePriceStatus, settings)) {
                result.add(channel);
            }
        }
        if (result.isEmpty() && Boolean.TRUE.equals(settings.getAllowContactChannel())) {
            result.add(PaymentChannel.CONTACT);
        }
        return List.copyOf(result);
    }

    private boolean isAvailable(PaymentChannel channel, SaasPlanPrice price,
                                com.discipolat.modules.tenants.domain.SaasPlanPricingSettings settings) {
        return isAvailable(channel, price.getCurrency(), price.effectiveCheckoutCurrency(),
                price.getStripePriceId(),
                price.getStripePriceStatus() == null ? null : price.getStripePriceStatus().name(),
                settings);
    }

    private boolean isAvailable(PaymentChannel channel, String currency, String checkoutCurrency,
                                String stripePriceId, String stripePriceStatus,
                                com.discipolat.modules.tenants.domain.SaasPlanPricingSettings settings) {
        return switch (channel) {
            case CARD -> Boolean.TRUE.equals(settings.getAllowCardChannel())
                    && isCardTechnicallyUsable(currency, checkoutCurrency, stripePriceId, stripePriceStatus);
            case STRONG -> Boolean.TRUE.equals(settings.getAllowStrongChannel())
                    && isStrongCurrencyConfigured(settings)
                    && isCardTechnicallyUsable(checkoutCurrency, checkoutCurrency, stripePriceId, stripePriceStatus);
            case MOMO -> Boolean.TRUE.equals(settings.getAllowMomoChannel())
                    && !isZeroDecimal(currency)
                    && hasRecurringCapableOperator(settings);
            case BANK -> Boolean.TRUE.equals(settings.getAllowBankChannel())
                    && (payoutProviders.getBank().isEnabled() || payoutProviders.getSepa().isEnabled());
            case CONTACT -> Boolean.TRUE.equals(settings.getAllowContactChannel());
        };
    }

    /**
     * Carte techniquement utilisable ?
     *
     * <p>Deux issues, pas une : soit la devise est à 2 décimales et Stripe accepte
     * le prix inline, soit un Price a été créé au préalable (synchro API ou saisie
     * manuelle). Sans cela, le bouton leads à un échec serveur.</p>
     */
    private boolean isCardTechnicallyUsable(String currency, String checkoutCurrency,
                                            String stripePriceId, String stripePriceStatus) {
        if (!stripeProperties.isEnabled()) {
            return false;
        }
        boolean hasPriceId = stripePriceId != null && !stripePriceId.isBlank();
        boolean isManual = "MANUAL".equalsIgnoreCase(stripePriceStatus) || "SYNCED".equalsIgnoreCase(stripePriceStatus);
        if (hasPriceId && isManual) {
            return true;
        }
        if (hasPriceId) {
            // Un identifiant existe mais la synchro est en échec : on ne fait pas confiance.
            return "SYNCED".equalsIgnoreCase(stripePriceStatus);
        }
        return !isZeroDecimal(effectiveCurrency(currency, checkoutCurrency));
    }

    private boolean isStrongCurrencyConfigured(
            com.discipolat.modules.tenants.domain.SaasPlanPricingSettings settings) {
        String strong = settings.getStrongCurrency();
        return strong != null && !strong.isBlank() && currencyValidator.isSupported(strong);
    }

    /**
     * Un seul opérateur Mobile Money est-il réellement branché ?
     *
     * <p>On vérifie qu'au moins un opérateur <em>activé</em> figure dans la liste
     * choisie par le super-admin. Liste vide = l'administrateur a déclaré « aucun
     * opérateur » = aucun CTA MoMo, ce qui est plus honnête qu'un bouton vers un 503.</p>
     */
    private boolean hasRecurringCapableOperator(
            com.discipolat.modules.tenants.domain.SaasPlanPricingSettings settings) {
        if (!providerProperties.isEnabled() || momoRegistry.activeCount() == 0) {
            return false;
        }
        List<String> chosen = settings.momoOperatorList();
        if (chosen.isEmpty()) {
            return false;
        }
        for (String operator : chosen) {
            try {
                if (momoRegistry.find(PaymentIntent.Operator.valueOf(operator)) != null) {
                    return true;
                }
            } catch (IllegalArgumentException unknown) {
                // Opérateur inconnu du référentiel : ignoré.
            }
        }
        return false;
    }

    private boolean isZeroDecimal(String currency) {
        if (currency == null || currency.isBlank()) return false;
        String code = currency.trim().toUpperCase(Locale.ROOT);
        if (ZERO_DECIMAL.contains(code)) return true;
        try {
            return currencyValidator.minorUnits(code) == 0;
        } catch (RuntimeException unknown) {
            return false;
        }
    }

    private String effectiveCurrency(String currency, String checkoutCurrency) {
        return (checkoutCurrency == null || checkoutCurrency.isBlank()) ? currency : checkoutCurrency;
    }

    /** Détail par canal, pour l'écran d'administration et le diagnostic. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> availabilityDetail(SaasPlanPrice price) {
        var settings = settingsService.current();
        Map<String, Boolean> detail = new LinkedHashMap<>();
        for (PaymentChannel channel : PaymentChannel.values()) {
            detail.put(channel.name(), isAvailable(channel, price, settings));
        }
        return detail;
    }
}