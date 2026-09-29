package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.payments.domain.MobileMoneyProvider;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * A3 (M8) — Chaque provider de décaissement s'active/désactive par la
 * configuration {@code app.payments.providers.*}, et le registre n'expose que
 * les fournisseurs RÉELLEMENT exploitables (flag + credentials).
 *
 * <p>Aucun appel réseau dans ce test : {@code isEnabled()} est une décision de
 * configuration, {@code initiate()} hors-ligne est testé sur son refus honnête.
 * La vérification de webhook est testée fail-closed dans les deux sens.</p>
 */
class PayoutProviderRegistryTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private static PayoutProvidersProperties.Provider provider(boolean enabled, String key,
                                                               String secret, String baseUrl) {
        PayoutProvidersProperties.Provider p = new PayoutProvidersProperties.Provider();
        p.setEnabled(enabled);
        p.setApiKey(key);
        p.setApiSecret(secret);
        p.setBaseUrl(baseUrl);
        return p;
    }

    @Nested
    @DisplayName("Activation par configuration")
    class Activation {

        @Test
        @DisplayName("défaut absolu : tout est désactivé, le registre est vide")
        void parDefautAucunProviderActif() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            PayoutProviderRegistry registry = new PayoutProviderRegistry(List.of(
                    new StripePayoutProvider(properties.getStripe(), HTTP, JSON),
                    new PayPalPayoutProvider(properties.getPaypal(), HTTP, JSON),
                    new SepaDirectDebitProvider(properties.getSepa(), HTTP, JSON),
                    new BankTransferProvider(properties.getBank())));

            assertThat(registry.activeCount()).isZero();
            assertThat(registry.findActive("stripe")).isEmpty();
            assertThat(registry.findActive("paypal")).isEmpty();
            assertThat(registry.findActive("sepa")).isEmpty();
            assertThat(registry.findActive("bank")).isEmpty();
            // Diagnostic honnête : chaque clé inconnue du registre actif a une raison.
            assertThat(registry.status()).hasSize(4)
                    .allSatisfy((key, entry) -> {
                        assertThat(entry).containsEntry("configured", false);
                        assertThat(String.valueOf(entry.get("reason"))).isNotBlank();
                    });
        }

        @Test
        @DisplayName("Stripe : flag sans clé = inactif ; flag + clé = actif")
        void stripeExigeFlagEtCle() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            StripePayoutProvider stripe = new StripePayoutProvider(properties.getStripe(), HTTP, JSON);

            properties.getStripe().setEnabled(true);
            assertThat(stripe.isEnabled()).as("enabled=true sans api-key reste fail-closed").isFalse();
            assertThat(stripe.disabledReason()).contains("clé d'API");

            properties.getStripe().setApiKey("sk_live_test");
            assertThat(stripe.isEnabled()).isTrue();
            assertThat(stripe.disabledReason()).isNull();
        }

        @Test
        @DisplayName("PayPal : client-id ET client-secret requis")
        void paypalExigeLesDeuxCredentials() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            PayPalPayoutProvider paypal = new PayPalPayoutProvider(properties.getPaypal(), HTTP, JSON);

            properties.getPaypal().setEnabled(true);
            properties.getPaypal().setApiKey("client-id");
            assertThat(paypal.isEnabled()).as("secret manquant → inactif").isFalse();

            properties.getPaypal().setApiSecret("client-secret");
            assertThat(paypal.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("SEPA : exige le connecteur (base-url) en plus du flag")
        void sepaExigeConnecteur() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            SepaDirectDebitProvider sepa = new SepaDirectDebitProvider(properties.getSepa(), HTTP, JSON);

            properties.getSepa().setEnabled(true);
            assertThat(sepa.isEnabled()).as("aucun connecteur bancaire configuré").isFalse();

            properties.getSepa().setBaseUrl("http://localhost:9999/non-utilise");
            assertThat(sepa.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Virement bancaire : le flag suffit (canal manuel par nature)")
        void bankSufitLeFlag() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            BankTransferProvider bank = new BankTransferProvider(properties.getBank());
            assertThat(bank.isEnabled()).isFalse();
            properties.getBank().setEnabled(true);
            assertThat(bank.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Registre")
    class Registre {

        @Test
        @DisplayName("findActive insensible à la casse, vide pour une clé inconnue")
        void rechercheInsensibleALaCasse() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            properties.getBank().setEnabled(true);
            PayoutProviderRegistry registry = new PayoutProviderRegistry(List.of(
                    new BankTransferProvider(properties.getBank())));

            assertThat(registry.findActive("BANK")).isPresent();
            assertThat(registry.findActive("bank")).isPresent();
            assertThat(registry.findActive("paypal")).isEmpty();
            assertThat(registry.findActive(null)).isEmpty();
        }

        @Test
        @DisplayName("clé définie en double : la première gagnante, pas d'écrasement silencieux")
        void cleDedupliereeGardeLaPremiere() {
            PayoutProvidersProperties.Provider first = provider(true, null, null, null);
            PayoutProvidersProperties.Provider second = provider(true, null, null, null);
            BankTransferProvider a = new BankTransferProvider(first);
            BankTransferProvider b = new BankTransferProvider(second);
            PayoutProviderRegistry registry = new PayoutProviderRegistry(List.of(a, b));

            assertThat(registry.findActive("bank")).containsSame(a);
            assertThat(registry.activeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Refus honnêtes et webhooks")
    class Honnetete {

        @Test
        @DisplayName("initiate() sans configuration = 503 avec code explicite, jamais un faux succès")
        void initiateRefuseHonnetement() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            StripePayoutProvider stripe = new StripePayoutProvider(properties.getStripe(), HTTP, JSON);
            PayoutRequest request = new PayoutRequest(null, "acct_x", 1000L, "EUR", "ref-1", Map.of());

            DomainException refused = catchThrowableOfType(() -> stripe.initiate(request), DomainException.class);
            assertThat(refused).hasMessageContaining("non configuré");
            // DomainException n'expose pas de getter : le contrat public observable est ProblemDetail.
            assertThat(refused.toProblemDetail().getStatus())
                    .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(refused.toProblemDetail().getTitle()).isEqualTo("PAYOUT_STRIPE_NOT_CONFIGURED");
        }

        @Test
        @DisplayName("webhook Stripe : HMAC officiel t=,v1= validé ; falsifié ou absent rejeté")
        void webhookStripeFailClosed() {
            PayoutProvidersProperties.Provider config = provider(true, "sk", "whsec_secret", null);
            config.setWebhookSecret("whsec_secret");
            StripePayoutProvider stripe = new StripePayoutProvider(config, HTTP, JSON);

            String body = "{\"id\":\"tr_1\",\"type\":\"transfer.paid\"}";
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String signature = PayoutCrypto.hmacSha256Hex("whsec_secret", timestamp + "." + body);

            assertThat(stripe.verifyWebhookSignature(
                    Map.of("Stripe-Signature", "t=" + timestamp + ",v1=" + signature), body)).isTrue();
            assertThat(stripe.verifyWebhookSignature(
                    Map.of("stripe-signature", "t=" + timestamp + ",v1=00deadbeef"), body)).isFalse();
            assertThat(stripe.verifyWebhookSignature(Map.of(), body)).isFalse();
            assertThat(stripe.verifyWebhookSignature(
                    Map.of("Stripe-Signature", "t=" + timestamp + ",v1=" + signature), null)).isFalse();
            // Sans secret configuré, RIEN n'est accepté (fail-closed).
            StripePayoutProvider sansSecret = new StripePayoutProvider(
                    provider(true, "sk", null, null), HTTP, JSON);
            assertThat(sansSecret.verifyWebhookSignature(
                    Map.of("Stripe-Signature", "t=" + timestamp + ",v1=" + signature), body)).isFalse();
        }

        @Test
        @DisplayName("webhook Stripe : signature trop âgée = rejeu refusé")
        void webhookStripeAntiReplay() {
            PayoutProvidersProperties.Provider config = provider(true, "sk", null, null);
            config.setWebhookSecret("whsec");
            StripePayoutProvider stripe = new StripePayoutProvider(config, HTTP, JSON);
            String body = "{}";
            String vieux = String.valueOf(System.currentTimeMillis() / 1000 - 3600);
            String signature = PayoutCrypto.hmacSha256Hex("whsec", vieux + "." + body);

            assertThat(stripe.verifyWebhookSignature(
                    Map.of("Stripe-Signature", "t=" + vieux + ",v1=" + signature), body)).isFalse();
        }
    }

    @Nested
    @DisplayName("Adaptateur Mobile Money (décaissement non intégré)")
    class MobileMoney {

        /** Stub de collecte : seul ce que l'opérateur sait faire aujourd'hui. */
        private static class CollectOnlyProvider implements MobileMoneyProvider {
            @Override
            public PaymentIntent.Operator operator() {
                return PaymentIntent.Operator.M_PESA;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public Result initiate(PaymentIntent intent) {
                throw new UnsupportedOperationException("collecte non testée ici");
            }

            @Override
            public Verification verify(String providerReference) {
                return new Verification(true, "SUCCESS", null);
            }
        }

        @Test
        @DisplayName("initiate refuse avec 503 PAYOUT_DISBURSEMENT_NOT_INTEGRATED — jamais simulé")
        void initiateRefuseCarDisbursementNonIntegre() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            properties.getMpesa().setEnabled(true);
            MobileMoneyPayoutAdapter adapter = new MobileMoneyPayoutAdapter(
                    "mpesa", PaymentIntent.Operator.M_PESA, new CollectOnlyProvider(), properties.getMpesa());

            assertThat(adapter.isEnabled()).isTrue();
            DomainException refused = catchThrowableOfType(
                    () -> adapter.initiate(new PayoutRequest(null, "254700000000", 500L, "KES", "ref-9", Map.of())),
                    DomainException.class);
            assertThat(refused).hasMessageContaining("n'est pas intégrée");
            assertThat(refused.toProblemDetail().getStatus())
                    .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE.value());
            assertThat(refused.toProblemDetail().getTitle()).isEqualTo("PAYOUT_DISBURSEMENT_NOT_INTEGRATED");
        }

        @Test
        @DisplayName("le flag payout seul n'invente rien : sans provider collecté actif, inactif")
        void sansDelegateResteInactif() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            properties.getMtn().setEnabled(true);
            MobileMoneyPayoutAdapter adapter = new MobileMoneyPayoutAdapter(
                    "mtn", PaymentIntent.Operator.MTN_MOMO, null, properties.getMtn());

            assertThat(adapter.isEnabled()).isFalse();
            assertThat(adapter.disabledReason()).contains("credentials opérateur");
        }

        @Test
        @DisplayName("queryStatus réutilise la vraie vérification opérateur")
        void queryStatusDelegueAuVraiVerify() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            properties.getMpesa().setEnabled(true);
            MobileMoneyPayoutAdapter adapter = new MobileMoneyPayoutAdapter(
                    "mpesa", PaymentIntent.Operator.M_PESA, new CollectOnlyProvider(), properties.getMpesa());

            assertThat(adapter.queryStatus("QRY123")).isEqualTo(PayoutStatus.SETTLED);
            assertThat(adapter.queryStatus(null)).isEqualTo(PayoutStatus.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Référentiels intégrés (SEPA / virement)")
    class Referentiels {

        @Test
        @DisplayName("IBAN : validation mod-97 ISO-13616 réelle")
        void ibanValideInvalide() {
            assertThat(SepaDirectDebitProvider.isValidIban("FR1420041010050500013M02606")).isTrue();
            assertThat(SepaDirectDebitProvider.isValidIban("FR1420041010050500013M02607")).isFalse();
            assertThat(SepaDirectDebitProvider.isValidIban("DE89370400440532013000")).isTrue();
            assertThat(SepaDirectDebitProvider.isValidIban("mot-de-passe")).isFalse();
            assertThat(SepaDirectDebitProvider.isValidIban(null)).isFalse();
        }

        @Test
        @DisplayName("référence E2E : 35 caractères, base 36 sans I/O, stable en longueur")
        void referenceEndToEndValide() {
            String ref = BankTransferProvider.endToEndReference("offrande-2026/09#12");
            assertThat(ref).hasSize(35);
            assertThat(ref).matches("^[0-9A-HJ-NP-Z]{35}$");
        }

        @Test
        @DisplayName("virement actif : initiate() émet PENDING (jamais SETTLED sans preuve externe)")
        void bankInitieEnPending() {
            PayoutProvidersProperties properties = new PayoutProvidersProperties();
            properties.getBank().setEnabled(true);
            BankTransferProvider bank = new BankTransferProvider(properties.getBank());

            PayoutResult result = bank.initiate(
                    new PayoutRequest(null, "IBAN:DE89370400440532013000", 2500L, "EUR", "sal-01", Map.of()));

            assertThat(result.status()).isEqualTo(PayoutStatus.PENDING);
            assertThat(bank.queryStatus(result.providerReference())).isEqualTo(PayoutStatus.UNKNOWN);
        }
    }
}
