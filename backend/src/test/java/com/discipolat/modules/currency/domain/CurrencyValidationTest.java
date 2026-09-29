package com.discipolat.modules.currency.domain;

import com.discipolat.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A3 (M9) — L'autorité monétaire du serveur est ISO-4217 (java.util.Currency),
 * pas une liste codée en dur : toute devise du référentiel international est
 * jouable, et les règles de l'unité mineure sont appliquées exactement.
 *
 * <p>DomainException n'expose pas de getter : le contrat public observable
 * testé ici est le ProblemDetail HTTP (statut + title = code d'erreur), ce qui
 * est précisément ce que reçoit le client.</p>
 */
class CurrencyValidationTest {

    private final Iso4217CurrencyValidator validator = new Iso4217CurrencyValidator();

    private static String codeOf(DomainException e) {
        return (String) e.toProblemDetail().getTitle();
    }

    @Nested
    @DisplayName("Référentiel ISO-4217 complet")
    class Referentiel {

        @Test
        @DisplayName("toutes les grandes devises mondiales sont acceptées, sans liste blanche maison")
        void leMondeEntierEstJouable() {
            for (String code : new String[]{"XOF", "XAF", "EUR", "USD", "GBP", "JPY", "KES",
                    "NGN", "GHS", "ZAR", "BRL", "INR", "CAD", "CHF", "AUD", "CNY", "MAD", "TND"}) {
                assertThat(validator.isSupported(code)).as("devise %s doit être supportée", code).isTrue();
            }
            // L'ancien bug M9 : la liste dure du serveur faisait ~10 devises.
            assertThat(validator.supportedCodes()).hasSizeGreaterThan(150);
        }

        @Test
        @DisplayName("code inconnu ou absent = refus 400 CURRENCY_UNSUPPORTED / CURRENCY_REQUIRED")
        void codesInvalidesRefusees() {
            // « QQQ » est dans le plage réservée privée d'ISO 4217 : jamais émise.
            DomainException unknown = catchThrowableOfType(() -> validator.require("QQQ"), DomainException.class);
            assertThat(codeOf(unknown)).isEqualTo("CURRENCY_UNSUPPORTED");
            assertThat(unknown.toProblemDetail().getStatus()).isEqualTo(400);

            assertThat(codeOf(catchThrowableOfType(() -> validator.require(null), DomainException.class)))
                    .isEqualTo("CURRENCY_REQUIRED");
            assertThat(codeOf(catchThrowableOfType(() -> validator.require("  "), DomainException.class)))
                    .isEqualTo("CURRENCY_REQUIRED");
            assertThat(validator.isSupported("PASUNEDEVISE")).isFalse();
        }

        @Test
        @DisplayName("normalisation : la casse et les espaces ne créent pas de fausse devise")
        void normalisationMajuscules() {
            assertThat(validator.require(" eur ").getCurrencyCode()).isEqualTo("EUR");
            assertThat(validator.minorUnits("jpy")).isZero();
        }
    }

    @Nested
    @DisplayName("Règle des unités mineures")
    class Decimales {

        @Test
        @DisplayName("décimales ISO réelles : 2 pour EUR, 0 pour XOF/JPY/KRW, 3 pour BHD")
        void nombreDeDecimalesVientDuStandard() {
            assertThat(validator.minorUnits("EUR")).isEqualTo(2);
            assertThat(validator.minorUnits("USD")).isEqualTo(2);
            assertThat(validator.minorUnits("XOF")).isZero();
            assertThat(validator.minorUnits("XAF")).isZero();
            assertThat(validator.minorUnits("JPY")).isZero();
            assertThat(validator.minorUnits("KRW")).isZero();
            assertThat(validator.minorUnits("BHD")).isEqualTo(3);
        }

        @Test
        @DisplayName("une devise à 0 unité mineure refuse toute virgule — exactitude comptable")
        void zeroDecimalesRefuseLesCentimes() {
            assertThatCode(() -> validator.validateAmount("XOF", new BigDecimal("1500")))
                    .doesNotThrowAnyException();
            // Zéros trimés sans perte de valeur : 1500.00 reste un montant entier.
            assertThatCode(() -> validator.validateAmount("JPY", new BigDecimal("1000.00")))
                    .doesNotThrowAnyException();

            DomainException fraction = catchThrowableOfType(
                    () -> validator.validateAmount("XOF", new BigDecimal("1500.50")), DomainException.class);
            assertThat(codeOf(fraction)).isEqualTo("CURRENCY_DECIMALS_INVALID");
            assertThat(codeOf(catchThrowableOfType(
                    () -> validator.validateAmount("KRW", new BigDecimal("5000.5")), DomainException.class)))
                    .isEqualTo("CURRENCY_DECIMALS_INVALID");
        }

        @Test
        @DisplayName("trop de décimales sur une devise à 2 = refus ; 1 ou 2 décimales = OK")
        void deuxDecimalesMaxPourEur() {
            assertThatCode(() -> validator.validateAmount("EUR", new BigDecimal("10.5")))
                    .doesNotThrowAnyException();
            assertThatCode(() -> validator.validateAmount("EUR", new BigDecimal("10.50")))
                    .doesNotThrowAnyException();
            assertThat(codeOf(catchThrowableOfType(
                    () -> validator.validateAmount("EUR", new BigDecimal("10.567")), DomainException.class)))
                    .isEqualTo("CURRENCY_DECIMALS_INVALID");
        }

        @Test
        @DisplayName("montant nul ou non positif = AMOUNT_INVALID, quelle que soit la devise")
        void montantsPositifsExigés() {
            assertThat(codeOf(catchThrowableOfType(
                    () -> validator.validateAmount("EUR", null), DomainException.class)))
                    .isEqualTo("AMOUNT_INVALID");
            assertThat(codeOf(catchThrowableOfType(
                    () -> validator.validateAmount("EUR", BigDecimal.ZERO), DomainException.class)))
                    .isEqualTo("AMOUNT_INVALID");
            assertThat(codeOf(catchThrowableOfType(
                    () -> validator.validateAmount("USD", new BigDecimal("-0.01")), DomainException.class)))
                    .isEqualTo("AMOUNT_INVALID");
        }
    }

    @Nested
    @DisplayName("Conversions unité mineure ↔ unité majeure")
    class Conversions {

        @Test
        @DisplayName("aller-retour exact pour 0, 2 et 3 décimales")
        void allerRetourSansPerte() {
            assertThat(validator.toMinorUnits("EUR", new BigDecimal("12.34"))).isEqualTo(1234L);
            assertThat(validator.toMinorUnits("USD", new BigDecimal("0.05"))).isEqualTo(5L);
            assertThat(validator.toMinorUnits("XOF", new BigDecimal("15000"))).isEqualTo(15000L);
            assertThat(validator.toMinorUnits("JPY", new BigDecimal("23201"))).isEqualTo(23201L);
            assertThat(validator.toMinorUnits("BHD", new BigDecimal("1.234"))).isEqualTo(1234L);

            assertThat(validator.fromMinorUnits("EUR", 1234L)).isEqualByComparingTo("12.34");
            assertThat(validator.fromMinorUnits("XOF", 15000L)).isEqualByComparingTo("15000");
            assertThat(validator.fromMinorUnits("BHD", 1234L)).isEqualByComparingTo("1.234");
            // L'unité mineure d'une devise à 0 décimale est l'entier lui-même.
            assertThat(validator.fromMinorUnits("KRW", 5000L).scale()).isZero();
        }
    }

    @Nested
    @DisplayName("Catalogue exposé au client")
    class Catalogue {

        @Test
        @DisplayName("le catalogue ISO complet expose code/nom/symbole/décimales, jamais de liste maison")
        void catalogueCompletEtExact() {
            Map<String, Map<String, Object>> catalog = validator.catalog();

            assertThat(catalog).containsKeys("EUR", "USD", "XAF", "XOF", "JPY", "KES", "BHD");
            assertThat(catalog.get("EUR"))
                    .containsEntry("code", "EUR")
                    .containsEntry("decimals", 2)
                    .containsEntry("symbol", "€");
            assertThat(catalog.get("XOF")).containsEntry("decimals", 0);
            assertThat(catalog.get("JPY")).containsEntry("decimals", 0);
            assertThat(String.valueOf(catalog.get("USD").get("name"))).isNotBlank();
            // Clé = code ISO : le client ne peut pas désynchroniser code et règle.
            catalog.forEach((key, entry) -> assertThat(key).isEqualTo(entry.get("code")));
        }
    }
}
