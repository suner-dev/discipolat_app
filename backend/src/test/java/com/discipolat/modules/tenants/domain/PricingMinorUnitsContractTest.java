package com.discipolat.modules.tenants.domain;

import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.tenants.enums.BillingCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrat d'unités de prix — TODO_TARIFS_PAIEMENTS_INTERNATIONAL §2.1.
 *
 * <p>Ce test verrouille la règle qui, si elle se dégrade, coûte de l'argent réel :
 * un montant monétaire est stocké et échangé en unités <b>mineures</b> ISO-4217.
 * Un montant majeur lu comme mineur divise les prix par 100 (0,29 € au lieu de
 * 29 €) ; un montant mineur lu comme majeur les multiplie par 100.</p>
 *
 * <p>La faille F1 était exactement ce second cas, en inverse : le backend passait
 * un montant majeur (29) à un champ Stripe qui attend des centimes.</p>
 */
class PricingMinorUnitsContractTest {

    private final Iso4217CurrencyValidator validator = new Iso4217CurrencyValidator();

    @Test
    @DisplayName("Les devises sans unité mineure en ont bien zéro")
    void zeroDecimalCurrencies() {
        assertThat(validator.minorUnits("XAF")).isZero();
        assertThat(validator.minorUnits("XOF")).isZero();
        assertThat(validator.minorUnits("JPY")).isZero();
        assertThat(validator.minorUnits("KRW")).isZero();
    }

    @Test
    @DisplayName("Les devises à deux décimales en ont bien deux")
    void twoDecimalCurrencies() {
        assertThat(validator.minorUnits("EUR")).isEqualTo(2);
        assertThat(validator.minorUnits("USD")).isEqualTo(2);
        assertThat(validator.minorUnits("CAD")).isEqualTo(2);
        assertThat(validator.minorUnits("GBP")).isEqualTo(2);
        assertThat(validator.minorUnits("CHF")).isEqualTo(2);
    }

    @Test
    @DisplayName("29 EUR major se convertit en 2900 unités mineures, pas 29")
    void eurosConversion() {
        long minor = validator.toMinorUnits("EUR", new BigDecimal("29.00"));
        assertThat(minor).isEqualTo(2900L);

        long annual = validator.toMinorUnits("EUR", new BigDecimal("290.00"));
        assertThat(annual).isEqualTo(29000L);
    }

    @Test
    @DisplayName("15 000 FCFA major reste 15 000 unités mineures : la devise n'a pas de centimes")
    void cfaConversion() {
        assertThat(validator.toMinorUnits("XAF", new BigDecimal("15000"))).isEqualTo(15000L);
        assertThat(validator.toMinorUnits("XOF", new BigDecimal("15000"))).isEqualTo(15000L);
        assertThat(validator.fromMinorUnits("XAF", 15000L)).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("Une devise sans centimes refuse un montant à décimales")
    void zeroDecimalRejectsDecimals() {
        assertThatThrownBy(() -> validator.validateAmount("XAF", new BigDecimal("15000.50")))
                .hasMessageContaining("XAF");
    }

    @Test
    @DisplayName("Le cycle de facturation tolère les libellés du monde réel")
    void billingCycleParsing() {
        assertThat(BillingCycle.fromString("monthly")).isEqualTo(BillingCycle.MONTHLY);
        assertThat(BillingCycle.fromString("MONTHLY")).isEqualTo(BillingCycle.MONTHLY);
        assertThat(BillingCycle.fromString("month")).isEqualTo(BillingCycle.MONTHLY);
        assertThat(BillingCycle.fromString("yearly")).isEqualTo(BillingCycle.YEARLY);
        assertThat(BillingCycle.fromString("YEARLY")).isEqualTo(BillingCycle.YEARLY);
        assertThat(BillingCycle.fromString("annual")).isEqualTo(BillingCycle.YEARLY);
        assertThat(BillingCycle.fromString("annuel")).isEqualTo(BillingCycle.YEARLY);
        assertThat(BillingCycle.fromString(null)).isEqualTo(BillingCycle.MONTHLY);
        assertThat(BillingCycle.fromString("  ")).isEqualTo(BillingCycle.MONTHLY);
    }
}