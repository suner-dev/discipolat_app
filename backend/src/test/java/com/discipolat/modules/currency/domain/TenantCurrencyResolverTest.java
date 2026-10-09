package com.discipolat.modules.currency.domain;

import com.discipolat.modules.finances.domain.FinanceAccount;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.payments.domain.RecurringDonation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * A3 (M9) — Plus aucune écriture comptable ne doit porter « XOF » par défaut.
 *
 * <p>Le constat : {@code PaymentIntent}, {@code RecurringDonation},
 * {@code FinanceAccount} et {@code FinanceDonation} portaient
 * {@code @Builder.Default private String devise = "XOF"}. Ce n'était pas un défaut
 * d'affichage mais une <b>donnée</b> : un tenant en EUR créait un compte, un don
 * ou une intention de paiement en XOF, sans l'avoir demandé, et l'écart
 * devenait invisible parce que la colonne était renseignée.</p>
 *
 * <p>Ces tests verrouillent les deux moitiés du correctif : l'entité ne ment
 * plus, et la résolution passe par la devise du tenant.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantCurrencyResolverTest {

    @Mock
    private CurrencyService currencyService;

    private TenantCurrencyResolver resolver() {
        return new TenantCurrencyResolver(currencyService);
    }

    private static CurrencyConfig config(String code) {
        CurrencyConfig config = new CurrencyConfig();
        config.setCurrencyCode(code);
        return config;
    }

    // ── 1. Les entités ne portent plus de valeur par défaut ──────────────────

    @Test
    @DisplayName("FinanceAccount sans devise explicite n'est plus pré-remplie en XOF")
    void financeAccountNaPlusDeDefautXof() {
        FinanceAccount account = FinanceAccount.builder().name("Compte courant").build();

        // Avant : "XOF". Une colonne NOT NULL pré-remplie sur une devise
        // étrangère au tenant produisait un écart comptable invisible.
        assertThat(account.getDevise()).isNull();
    }

    @Test
    @DisplayName("PaymentIntent sans devise explicite n'est plus pré-remplie en XOF")
    void paymentIntentNaPlusDeDefautXof() {
        PaymentIntent intent = PaymentIntent.builder()
                .amount(java.math.BigDecimal.TEN)
                .phoneNumber("+2250700000000")
                .build();

        assertThat(intent.getCurrency()).isNull();
    }

    @Test
    @DisplayName("RecurringDonation sans devise explicite n'est plus pré-remplie en XOF")
    void recurringDonationNaPlusDeDefautXof() {
        RecurringDonation donation = RecurringDonation.builder()
                .amount(java.math.BigDecimal.TEN)
                .build();

        assertThat(donation.getCurrency()).isNull();
    }

    @Test
    @DisplayName("le builder accepte toujours une devise explicite — la capacité n'est pas retirée")
    void deviseExpliciteResteAcceptee() {
        FinanceAccount account = FinanceAccount.builder().devise("EUR").build();
        PaymentIntent intent = PaymentIntent.builder()
                .currency("EUR")
                .amount(java.math.BigDecimal.TEN)
                .build();

        assertThat(account.getDevise()).isEqualTo("EUR");
        assertThat(intent.getCurrency()).isEqualTo("EUR");
    }

    // ── 2. La résolution passe par la devise du tenant ───────────────────────

    @Test
    @DisplayName("sans valeur explicite, la devise primaire du tenant est utilisée")
    void utiliseLaDeviseDuTenant() {
        when(currencyService.getPrimaryCurrency()).thenReturn(config("EUR"));

        assertThat(resolver().resolve(null)).isEqualTo("EUR");
        assertThat(resolver().resolve("")).isEqualTo("EUR");
        assertThat(resolver().resolve("   ")).isEqualTo("EUR");
    }

    @Test
    @DisplayName("la devise du tenant est normalisée en majuscules")
    void normaliseEnMajuscules() {
        when(currencyService.getPrimaryCurrency()).thenReturn(config("eur"));

        assertThat(resolver().resolveFromTenant()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("une devise explicite PRIME toujours sur la devise du tenant")
    void deviseExplicitePrime() {
        when(currencyService.getPrimaryCurrency()).thenReturn(config("EUR"));

        // Un don en USD dans un tenant EUR est légitime (dons de l'étranger) :
        // l'explicite ne doit jamais être écrasé par le défaut du tenant.
        assertThat(resolver().resolve("usd")).isEqualTo("USD");
        assertThat(resolver().resolve(" GBP ")).isEqualTo("GBP");
    }

    @Test
    @DisplayName("hors contexte tenant (webhook, job), le défaut documenté s'applique sans lever")
    void horsContexteTenantNeLevePas() {
        when(currencyService.getPrimaryCurrency()).thenThrow(new IllegalStateException("no tenant context"));

        // Un webhook opérateur ne doit pas échouer à cause d'un contexte de
        // devise absent : c'est une situation ATTENDUE, pas une erreur.
        assertThat(resolver().resolveFromTenant()).isEqualTo(TenantCurrencyResolver.FALLBACK);
    }

    @Test
    @DisplayName("tenant sans devise configurée : défaut unique et documenté")
    void tenantSansDeviseUtiliseLeDefaut() {
        when(currencyService.getPrimaryCurrency()).thenReturn(null);

        assertThat(resolver().resolveFromTenant()).isEqualTo(TenantCurrencyResolver.FALLBACK);
        // Le défaut est unique : plus de divergence XOF côté entités / XAF côté
        // FinanceService.
        assertThat(TenantCurrencyResolver.FALLBACK).isEqualTo("XAF");
    }

    @Test
    @DisplayName("une devise configurée mais vide retombe sur le défaut")
    void deviseConfigureeMaisVideUtiliseLeDefaut() {
        when(currencyService.getPrimaryCurrency()).thenReturn(config("  "));

        assertThat(resolver().resolveFromTenant()).isEqualTo(TenantCurrencyResolver.FALLBACK);
    }
}