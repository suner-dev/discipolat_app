package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.tenants.enums.BillingCycle;
import com.discipolat.modules.tenants.enums.PaymentChannel;
import com.discipolat.modules.tenants.enums.PricingDisplayMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Résolution des prix — TODO_TARIFS_PAIEMENTS_INTERNATIONAL §T2.1.
 *
 * <p>Trois propriétés testées, qui sont les mêmes que les principes du produit :</p>
 * <ol>
 *   <li><b>un prix régional prime sur le prix mondial</b>, sinon la grille
 *       régionale ne sert à rien ;</li>
 *   <li><b>aucun repli de devise sans autorisation</b> — sinon un client voit un
 *       montant qui n'est pas celui de son marché ;</li>
 *   <li><b>aucun prix ne vaut zéro par défaut</b> : l'absence se dit, elle ne se
 *       compense pas, sinon un plan payant peut se lire comme gratuit.</li>
 * </ol>
 */
class PricingCatalogServiceTest {

    private SaasPlanPriceRepository priceRepository;
    private SaasPlanRepository planRepository;
    private PricingSettingsService settingsService;
    private PricingCatalogService service;

    @BeforeEach
    void setUp() {
        priceRepository = mock(SaasPlanPriceRepository.class);
        planRepository = mock(SaasPlanRepository.class);
        settingsService = mock(PricingSettingsService.class);
        service = new PricingCatalogService(planRepository, priceRepository, settingsService,
                new Iso4217CurrencyValidator());
    }

    private SaasPlanPricingSettings settings() {
        SaasPlanPricingSettings settings = SaasPlanPricingSettings.builder()
                .id(SaasPlanPricingSettings.SINGLETON_ID)
                .enabledCurrencies(new String[]{"XAF", "XOF", "EUR"})
                .baseCurrency("XAF")
                .defaultDisplayCurrency("XAF")
                .regionPricingEnabled(true)
                .showFallbackCurrency(false)
                .displayMode(PricingDisplayMode.SINGLE)
                .build();
        when(settingsService.current()).thenReturn(settings);
        return settings;
    }

    private SaasPlanPrice price(String currency, BillingCycle cycle, String region, long amountMinor) {
        return SaasPlanPrice.builder()
                .planKey("STARTUP")
                .currency(currency)
                .billingCycle(cycle)
                .regionCode(region)
                .amountMinor(amountMinor)
                .isEnabled(true)
                .isVisible(true)
                .isSelectable(true)
                .paymentChannels("MOMO,STRONG")
                .build();
    }

    @Test
    @DisplayName("Le prix régional prime sur le prix mondial de la même devise")
    void regionalPriceWins() {
        settings();
        SaasPlanPrice global = price("EUR", BillingCycle.MONTHLY, "GLOBAL", 2900L);
        SaasPlanPrice eu = price("EUR", BillingCycle.MONTHLY, "EU", 3200L);
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode("STARTUP", "EUR", BillingCycle.MONTHLY, "EU"))
                .thenReturn(Optional.of(eu));
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode("STARTUP", "EUR", BillingCycle.MONTHLY, "GLOBAL"))
                .thenReturn(Optional.of(global));

        Optional<PricingCatalogService.ResolvedPrice> resolved =
                service.resolvePrice("STARTUP", "EUR", BillingCycle.MONTHLY, "EU");

        assertThat(resolved).isPresent();
        assertThat(resolved.get().amountMinor()).isEqualTo(3200L);
        assertThat(resolved.get().regional()).isTrue();
        assertThat(resolved.get().fromFallback()).isFalse();
        assertThat(resolved.get().amount()).isEqualTo("32.00");
    }

    @Test
    @DisplayName("Sans prix régional, le prix mondial répond")
    void fallsBackToGlobalWhenNoRegionalPrice() {
        settings();
        SaasPlanPrice global = price("EUR", BillingCycle.MONTHLY, "GLOBAL", 2900L);
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode("STARTUP", "EUR", BillingCycle.MONTHLY, "EU"))
                .thenReturn(Optional.empty());
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode("STARTUP", "EUR", BillingCycle.MONTHLY, "GLOBAL"))
                .thenReturn(Optional.of(global));

        Optional<PricingCatalogService.ResolvedPrice> resolved =
                service.resolvePrice("STARTUP", "EUR", BillingCycle.MONTHLY, "EU");

        assertThat(resolved).isPresent();
        assertThat(resolved.get().regional()).isFalse();
        assertThat(resolved.get().amountMinor()).isEqualTo(2900L);
    }

    @Test
    @DisplayName("Sans repli autorisé, une devise sans prix reste absente — jamais 0")
    void noFallbackWithoutAuthorisation() {
        settings();
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(eq("STARTUP"), eq("XOF"), any(), any()))
                .thenReturn(Optional.empty());

        Optional<PricingCatalogService.ResolvedPrice> resolved =
                service.resolvePrice("STARTUP", "XOF", BillingCycle.MONTHLY, null);

        assertThat(resolved).isEmpty();
    }

    @Test
    @DisplayName("Avec repli autorisé, la devise de base est signalée comme telle")
    void fallbackWhenAuthorised() {
        SaasPlanPricingSettings settings = settings();
        settings.setShowFallbackCurrency(true);
        SaasPlanPrice base = price("XAF", BillingCycle.MONTHLY, "GLOBAL", 15000L);
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(eq("STARTUP"), eq("XOF"), any(), any()))
                .thenReturn(Optional.empty());
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(eq("STARTUP"), eq("XAF"), any(), any()))
                .thenReturn(Optional.of(base));

        Optional<PricingCatalogService.ResolvedPrice> resolved =
                service.resolvePrice("STARTUP", "XOF", BillingCycle.MONTHLY, null);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().fromFallback()).isTrue();
        assertThat(resolved.get().currency()).isEqualTo("XAF");
    }

    @Test
    @DisplayName("Une devise désactivée par le super-admin ne donne aucun prix")
    void disabledCurrencyYieldsNothing() {
        settings();
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(any(), any(), any(), any()))
                .thenReturn(Optional.of(price("JPY", BillingCycle.MONTHLY, "GLOBAL", 2000L)));

        assertThat(service.resolvePrice("STARTUP", "JPY", BillingCycle.MONTHLY, null)).isEmpty();
    }

    @Test
    @DisplayName("Les canaux déclarés sont filtrés par ce que le super-admin autorise")
    void channelsFilteredBySettings() {
        SaasPlanPricingSettings settings = settings();
        settings.setAllowMomoChannel(false);      // non activé globalement
        settings.setAllowStrongChannel(true);
        settings.setAllowContactChannel(true);

        SaasPlanPrice price = price("XAF", BillingCycle.MONTHLY, "GLOBAL", 15000L);
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(eq("STARTUP"), eq("XAF"), any(), any()))
                .thenReturn(Optional.of(price));

        PricingCatalogService.ResolvedPrice resolved =
                service.resolvePrice("STARTUP", "XAF", BillingCycle.MONTHLY, null).orElseThrow();

        // MOMO retiré car désactivé globalement ; STRONG conservé.
        assertThat(resolved.channels()).containsExactly(PaymentChannel.STRONG);
    }

    @Test
    @DisplayName("Un prix sans aucun canal autorisé retombe sur le contact")
    void contactFallbackWhenNoChannel() {
        SaasPlanPricingSettings settings = settings();
        settings.setAllowMomoChannel(false);
        settings.setAllowStrongChannel(false);
        settings.setAllowCardChannel(false);
        settings.setAllowBankChannel(false);
        settings.setAllowContactChannel(true);

        SaasPlanPrice price = price("XAF", BillingCycle.MONTHLY, "GLOBAL", 15000L);
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(eq("STARTUP"), eq("XAF"), any(), any()))
                .thenReturn(Optional.of(price));

        PricingCatalogService.ResolvedPrice resolved =
                service.resolvePrice("STARTUP", "XAF", BillingCycle.MONTHLY, null).orElseThrow();

        // Jamais une page sans issue : le contact reste proposable.
        assertThat(resolved.channels()).containsExactly(PaymentChannel.CONTACT);
    }

    @Test
    @DisplayName("Les canaux sont rendus dans l'ordre du CSV configuré")
    void channelOrderFollowsCsv() {
        SaasPlanPricingSettings settings = settings();
        settings.setAllowMomoChannel(true);
        settings.setAllowCardChannel(true);
        SaasPlanPrice price = price("XAF", BillingCycle.MONTHLY, "GLOBAL", 15000L);
        price.setPaymentChannels("STRONG,MOMO,CARD");
        when(priceRepository.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(eq("STARTUP"), eq("XAF"), any(), any()))
                .thenReturn(Optional.of(price));

        PricingCatalogService.ResolvedPrice resolved =
                service.resolvePrice("STARTUP", "XAF", BillingCycle.MONTHLY, null).orElseThrow();

        assertThat(resolved.channels()).containsExactly(
                PaymentChannel.STRONG, PaymentChannel.MOMO, PaymentChannel.CARD);
    }

    @Test
    @DisplayName("La devise demandée n'est retenue que si elle est activée")
    void displayCurrencyHonoursPolicy() {
        settings();
        settings();
        assertThat(service.resolveDisplayCurrency("EUR")).isEqualTo("EUR");
        // JPY n'est pas activée : on retombe sur la devise d'affichage par défaut.
        assertThat(service.resolveDisplayCurrency("JPY")).isEqualTo("XAF");
        assertThat(service.resolveDisplayCurrency("nimportequoi")).isEqualTo("XAF");
        assertThat(service.resolveDisplayCurrency(null)).isEqualTo("XAF");
    }
}