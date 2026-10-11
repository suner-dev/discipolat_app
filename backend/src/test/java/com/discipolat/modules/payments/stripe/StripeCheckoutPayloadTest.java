package com.discipolat.modules.payments.stripe;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import com.discipolat.modules.tenants.domain.PricingCatalogService;
import com.discipolat.modules.tenants.domain.SaasPlan;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import com.discipolat.modules.tenants.enums.BillingCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Charge utile réellement envoyée à Stripe — TODO_TARIFS_PAIEMENTS_INTERNATIONAL T10.5.
 *
 * <p><b>Ce test est leHeartbeat de la facturation.</b> Il intercepte la requête
 * au moment où elle quitte le SDK et vérifie le champ
 * {@code line_items[0][price_data][unit_amount]}. C'est exactement le champ que
 * la faille F1 avait mal alimenté : un montant <em>majeur</em> y donnait 0,29 €
 * pour un plan affiché à 29 €.</p>
 *
 * <p>Il est volontairement <b>sans clé Stripe et sans réseau</b> : le getter de
 * réponse du SDK est remplacé par un double qui enregistre la charge utile. C'est
 * ce qui rend la vérification reproductible en CI, tous les jours, alors qu'un
 * test manuel avec une clé `sk_test_…` ne le ferait qu'une fois.</p>
 */
class StripeCheckoutPayloadTest {

    /** Charge utile interceptée, telle que le SDK l'enverrait à Stripe. */
    private static final List<Map<String, Object>> CAPTURED = new ArrayList<>();

    private SaasPlanRepository planRepository;
    private PricingCatalogService catalogService;

    @BeforeEach
    void setUp() {
        CAPTURED.clear();
        StripePayloadSpy.install();
        planRepository = mock(SaasPlanRepository.class);
        catalogService = mock(PricingCatalogService.class);
    }

    @AfterEach
    void tearDown() {
        StripePayloadSpy.uninstall();
    }

    private StripeBillingService service(String secretKey) {
        StripeBillingProperties props = new StripeBillingProperties();
        injectSecretKey(props, secretKey);
        return new StripeBillingService(props, planRepository,
                mock(TenantSubscriptionRepository.class),
                new SimpleObjectProvider<>(catalogService),
                new Iso4217CurrencyValidator());
    }

    private void injectSecretKey(StripeBillingProperties props, String secretKey) {
        try {
            var field = StripeBillingProperties.class.getDeclaredField("secretKey");
            field.setAccessible(true);
            field.set(props, secretKey);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("secretKey inaccessible", e);
        }
    }

    private void givenPlan(SaasPlan plan) {
        when(planRepository.findByKeyIgnoreCaseAndIsActiveTrue(plan.getKey()))
                .thenReturn(Optional.of(plan));
    }

    private void givenResolvedPrice(long amountMinor, String currency, String checkoutCurrency) {
        when(catalogService.resolvePrice(any(), any(), any(BillingCycle.class), any()))
                .thenReturn(Optional.of(new PricingCatalogService.ResolvedPrice(
                        "GROWTH", currency, BillingCycle.MONTHLY, "GLOBAL", amountMinor,
                        amountMinor / 100.0 + "", 2, false, false, true, true,
                        List.of(), checkoutCurrency, false, null, null, 0, null)));
    }

    // ── F1 : le montant est en unités mineures ───────────────────────────────

    @Test
    @DisplayName("29 EUR majors partent en 2900 centimes, pas en 29")
    void eurosAreSentAsMinorUnits() {
        givenPlan(plan("GROWTH", "EUR"));
        givenResolvedPrice(2900L, "EUR", "EUR");

        service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "GROWTH", "monthly", "EUR", null);

        assertThat(unitAmount()).isEqualTo(2900L);
        // La régression exacte : 29 au lieu de 2900 facturait 0,29 EUR.
        assertThat(unitAmount()).isNotEqualTo(29L);
        assertThat(lineItemCurrency()).isEqualTo("eur");
    }

    @Test
    @DisplayName("99 USD majors partent en 9900 centimes")
    void dollarsAreSentAsMinorUnits() {
        givenPlan(plan("NETWORK", "USD"));
        givenResolvedPrice(9900L, "USD", "USD");

        service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "NETWORK", "monthly", "USD", null);

        assertThat(unitAmount()).isEqualTo(9900L);
    }

    @Test
    @DisplayName("Affichage XAF, débit EUR : le montant n'est pas mis à l'échelle")
    void strongCurrencyDoesNotRescaleTheAmount() {
        givenPlan(plan("STARTUP", "XAF"));
        // Catalogue en devise forte : le super-admin saisit le montant qu'il veut
        // débiter. On ne convertit surtout pas — ce serait inventer un taux.
        givenResolvedPrice(2500L, "EUR", "EUR");

        service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "STARTUP", "monthly", "XAF", null);

        // Le montant part tel quel : 2500, pas 2500 x 100.
        assertThat(unitAmount()).isEqualTo(2500L);
        assertThat(lineItemCurrency()).isEqualTo("eur");
    }

    @Test
    @DisplayName("Le canal devise forte débite bien la devise de débit, pas celle d'affichage")
    void strongCurrencyChargesTheCheckoutCurrency() {
        givenPlan(plan("STARTUP", "EUR"));
        // Affichage en XAF, débit en EUR : c'est ce que le canal STRONG promet.
        givenResolvedPrice(2500L, "EUR", "EUR");

        service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "STARTUP", "monthly", "EUR", null);

        assertThat(unitAmount()).isEqualTo(2500L);
        assertThat(lineItemCurrency()).isEqualTo("eur");
    }

    // ── F2 : les devises sans centimes exigent un Price pré-existant ────────

    @Test
    @DisplayName("Sans prix de catalogue, le refus dit PLAN_PRICE_MISSING, pas une erreur Stripe")
    void missingCatalogPriceIsReportedAsSuch() {
        givenPlan(plan("STARTUP", "XOF"));
        when(catalogService.resolvePrice(any(), any(), any(BillingCycle.class), any()))
                .thenReturn(Optional.empty());

        BusinessRuleException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "STARTUP",
                        "monthly", "XOF", null));

        assertThat(thrown.getCode()).isEqualTo("PLAN_PRICE_MISSING");
        assertThat(CAPTURED).isEmpty();
    }

    @Test
    @DisplayName("Le refus d'une devise sans centimes cite la devise et la marche a suivre")
    void zeroDecimalRefusalIsActionable() {
        givenPlan(plan("STARTUP", "XOF"));
        givenResolvedPrice(15000L, "XOF", "XOF");

        BusinessRuleException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "STARTUP",
                        "monthly", "XOF", null));

        assertThat(thrown.getCode()).isEqualTo("STRIPE_PRICE_ID_REQUIRED");
        // Le message doit dire quoi faire : un message d'erreur qui n'indique
        // pas la marche à suivre coûte un ticket support à chaque fois.
        assertThat(thrown.getMessage())
                .contains("XOF")
                .containsIgnoringCase("Price");
    }

    @Test
    @DisplayName("Un prix de catalogue en devise sans centimes exige un Price Stripe")
    void zeroDecimalCatalogPriceStillRequiresAStripePrice() {
        givenPlan(plan("STARTUP", "XOF"));
        givenResolvedPrice(15000L, "XOF", "XOF");

        BusinessRuleException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "STARTUP",
                        "monthly", "XOF", null));

        assertThat(thrown.getCode()).isEqualTo("STRIPE_PRICE_ID_REQUIRED");
        assertThat(CAPTURED).isEmpty();
    }

    @Test
    @DisplayName("Avec un Price Stripe pré-existant, le checkout passe par ce Price")
    void preExistingStripePriceIsUsedVerbatim() {
        SaasPlan plan = plan("STARTUP", "XAF");
        plan.setStripePriceIdMonthly("price_test_existing");
        givenPlan(plan);

        Map<String, Object> result = service("sk_test_dummy")
                .createCheckoutSession(UUID.randomUUID(), null, "STARTUP", "monthly", "XAF", null);

        // Un Price pré-existant porte SON montant : on ne le recalcule surtout pas.
        assertThat(payloadAt("line_items", 0, "price")).isEqualTo("price_test_existing");
        assertThat(payloadAt("line_items", 0, "price_data")).isNull();
        assertThat(result).containsKey("url");
    }

    // ── Fail-closed ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sans clé Stripe, aucune requête n'est émise")
    void noApiCallWithoutSecretKey() {
        givenPlan(plan("GROWTH", "EUR"));
        givenResolvedPrice(2900L, "EUR", "EUR");

        BusinessRuleException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service("").createCheckoutSession(UUID.randomUUID(), null, "GROWTH", "monthly", "EUR", null));

        assertThat(thrown.getCode()).isEqualTo("STRIPE_DISABLED");
        // Le point essentiel : aucun appel réseau ne doit partir sans clé.
        assertThat(CAPTURED).isEmpty();
    }

    @Test
    @DisplayName("Un plan sans prix au catalogue est refusé plutôt que facturé 0")
    void planWithoutPriceIsRefusedRatherThanChargedZero() {
        givenPlan(plan("CUSTOM", "EUR"));
        when(catalogService.resolvePrice(any(), any(), any(BillingCycle.class), any()))
                .thenReturn(Optional.empty());

        BusinessRuleException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "CUSTOM",
                        "monthly", "EUR", null));

        assertThat(thrown.getCode()).isEqualTo("PLAN_PRICE_MISSING");
        assertThat(CAPTURED).isEmpty();
    }

    @Test
    @DisplayName("Le Price ID hérité sur le plan reste prioritaire")
    void legacyStripePriceOnPlanStillWorks() {
        SaasPlan plan = plan("STARTUP", "EUR");
        plan.setStripePriceIdMonthly("price_legacy");
        givenPlan(plan);

        service("sk_test_dummy").createCheckoutSession(UUID.randomUUID(), null, "STARTUP", "monthly", "EUR", null);

        assertThat(payloadAt("line_items", 0, "price")).isEqualTo("price_legacy");
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    private Object unitAmount() {
        return payloadAt("line_items", 0, "price_data", "unit_amount");
    }

    private Object lineItemCurrency() {
        return payloadAt("line_items", 0, "price_data", "currency");
    }

    /**
     * Parcours d'un chemin dans la charge utile. Les items de ligne arrivent
     * comme une liste, les paramètres comme une carte : les deux formes sont
     * gérées, sinon le test ne pourrait pas lire le premier poste.
     */
    private Object payloadAt(Object... path) {
        Object current = CAPTURED.get(0);
        for (Object key : path) {
            if (current == null) return null;
            if (current instanceof List<?> list) {
                current = list.isEmpty() ? null : list.get(0);
            } else if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * Fournisseur minimal : évite une réflexion sur l'injection Spring et rend
     * le test lisible sans démarrer de contexte.
     */
    private static final class SimpleObjectProvider<T>
            implements org.springframework.beans.factory.ObjectProvider<T> {
        private final T instance;

        SimpleObjectProvider(T instance) {
            this.instance = instance;
        }

        @Override public T getObject() {
            return instance;
        }

        @Override public T getObject(Object... args) {
            return instance;
        }

        @Override public T getIfAvailable() {
            return instance;
        }

        @Override public T getIfUnique() {
            return instance;
        }
    }

    private SaasPlan plan(String key, String currency) {
        return SaasPlan.builder().key(key).name(key).description("Plan de test").currency(currency)
                .isActive(true).isPublic(true).trialDays(30).annualDiscountPct(17)
                .limitsJson("{}").featuresJson("{}").build();
    }

    /**
     * Double qui enregistre la charge utile au lieu de la transmettre.
     *
     * <p>Le SDK expose un point d'extension statique (`ApiResource`) : on le
     * remplace le temps du test, puis on le restaure. Sans cela, la seule façon
     * de vérifier l'unité envoyée serait de payer un vrai checkout.</p>
     */
    private static final class StripePayloadSpy implements com.stripe.net.StripeResponseGetter {

        private static com.stripe.net.StripeResponseGetter previous;

        static void install() {
            previous = readGlobal();
            com.stripe.net.ApiResource.setStripeResponseGetter(new StripePayloadSpy());
        }

        static void uninstall() {
            if (previous != null) {
                com.stripe.net.ApiResource.setStripeResponseGetter(previous);
            }
        }

        private static com.stripe.net.StripeResponseGetter readGlobal() {
            try {
                var method = com.stripe.net.ApiResource.class.getDeclaredMethod("getGlobalResponseGetter");
                method.setAccessible(true);
                return (com.stripe.net.StripeResponseGetter) method.invoke(null);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends com.stripe.model.StripeObjectInterface> T request(
                com.stripe.net.BaseAddress baseAddress,
                com.stripe.net.ApiResource.RequestMethod method,
                String path,
                Map<String, Object> params,
                Type type,
                com.stripe.net.RequestOptions options,
                com.stripe.net.ApiMode apiMode) {
            CAPTURED.add(new LinkedHashMap<>(params));
            if (type == com.stripe.model.checkout.Session.class) {
                com.stripe.model.checkout.Session session = new com.stripe.model.checkout.Session();
                session.setId("cs_test_capture");
                session.setUrl("https://checkout.stripe.test/cs_test_capture");
                return (T) session;
            }
            if (type == com.stripe.model.Price.class) {
                com.stripe.model.Price price = new com.stripe.model.Price();
                price.setId("price_test_capture");
                return (T) price;
            }
            if (type == com.stripe.model.Product.class) {
                com.stripe.model.Product product = new com.stripe.model.Product();
                product.setId("prod_test_capture");
                return (T) product;
            }
            return null;
        }

        @Override
        public java.io.InputStream requestStream(com.stripe.net.BaseAddress baseAddress,
                                                com.stripe.net.ApiResource.RequestMethod method,
                                                String path,
                                                Map<String, Object> params,
                                                com.stripe.net.RequestOptions options,
                                                com.stripe.net.ApiMode apiMode) {
            throw new UnsupportedOperationException("flux non testé");
        }
    }
}