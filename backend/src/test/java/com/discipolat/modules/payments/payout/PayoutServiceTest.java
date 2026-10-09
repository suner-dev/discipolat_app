package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.currency.domain.Iso4217CurrencyValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A3 (M8) — Le service de décaissement doit être réellementbranché au SPI.
 *
 * <p>Le constat : {@code PayoutProviderRegistry} n'était appelé par AUCUN service
 * métier ni contrôleur — {@code initiate()} n'était atteignable que depuis un
 * test. Ces tests verrouillent le câblage et, surtout, les trois refus qui
 * protègent de l'argent des tiers.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayoutServiceTest {

    @Mock
    private PayoutProviderRegistry registry;

    @Mock
    private PayoutProvider stripe;

    @Mock
    private PayoutProvider bank;

    @Mock
    private Iso4217CurrencyValidator currencyValidator;

    private PayoutService service;

    @BeforeEach
    void setUp() {
        statusByChannel.clear();
        service = new PayoutService(registry, currencyValidator);
    }

    @AfterEach
    void clearTenantContext() {
        com.discipolat.common.multitenancy.TenantContext.clear();
    }

    /**
     * Prépare le registre pour un fournisseur donné.
     *
     * <p>La clé est passée EN ARGUMENT et non lue via {@code provider.key()} : un
     * appel à un mock non stubé DANS une instruction {@code when(...)} produit
     * un {@code UnfinishedStubbingException} qui contamine tous les tests du
     * fichier. Ici, la clé est une constante du test.</p>
     */
    private final Map<String, Map<String, Object>> statusByChannel = new HashMap<>();

    private void givenActiveProvider(PayoutProvider provider, String key,
                                     boolean enabled, String reason) {
        when(provider.isEnabled()).thenReturn(enabled);
        when(provider.disabledReason()).thenReturn(reason);
        // Cumul nécessaire : deux appels dans un même test (canal actif + canal
        // inactif) doivent produire un `status()` qui contient LES DEUX. Le
        // remplacer à chaque appel n'en laissait qu'un.
        statusByChannel.put(key, Map.of("configured", enabled,
                "reason", reason == null ? "" : reason));
        when(registry.status()).thenReturn(statusByChannel);
        when(registry.findActive(key)).thenReturn(enabled ? java.util.Optional.of(provider)
                : java.util.Optional.empty());
    }

    @Test
    @DisplayName("un décaissement actif atteint le fournisseur du bon canal")
    void decaiementAtteintLeFournisseur() {
        givenActiveProvider(stripe, "stripe", true, null);
        when(currencyValidator.isSupported("EUR")).thenReturn(true);
        when(stripe.initiate(any())).thenReturn(PayoutResult.pending("tr_1", "accepté"));

        PayoutResult result = service.initiate("stripe", new PayoutRequest(
                "ref-1", "acct_123", 1000L, "EUR", "COMM-2026-001", Map.of()));

        assertThat(result.providerReference()).isEqualTo("tr_1");
        assertThat(result.status()).isEqualTo(PayoutStatus.PENDING);
        verify(stripe, times(1)).initiate(any());
    }

    @Test
    @DisplayName("un canal INCONNU est refusé en 400 — jamais de repli sur un autre opérateur")
    void canalInconnuEstRefuse() {
        statusByChannel.put("stripe", Map.of("configured", true, "reason", ""));
        when(registry.status()).thenReturn(statusByChannel);
        when(registry.findActive("westernunion")).thenReturn(java.util.Optional.empty());

        DomainException failure = org.junit.jupiter.api.Assertions.assertThrows(
                DomainException.class,
                () -> service.initiate("westernunion", new PayoutRequest(
                        "ref-1", "acct_123", 1000L, "EUR", "COMM-1", Map.of())));

        assertThat(failure.toProblemDetail().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(failure.getCode()).isEqualTo("PAYOUT_PROVIDER_UNKNOWN");
        // Point central : aucun fournisseur ne doit avoir été sollicité. Un
        // repli « sur le défaut » transférerait de l'argent chez le mauvais
        // opérateur.
        verify(stripe, never()).initiate(any());
        verify(bank, never()).initiate(any());
    }

    @Test
    @DisplayName("un canal connu mais INACTIF est refusé en 503 avec la raison honnête")
    void canalInactifEstRefuse() {
        givenActiveProvider(stripe, "stripe", false, "STRIPE_SECRET_KEY absente");

        DomainException failure = org.junit.jupiter.api.Assertions.assertThrows(
                DomainException.class,
                () -> service.initiate("stripe", new PayoutRequest(
                        "ref-1", "acct_123", 1000L, "EUR", "COMM-1", Map.of())));

        assertThat(failure.toProblemDetail().getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(failure.getCode()).isEqualTo("PAYOUT_PROVIDER_NOT_CONFIGURED");
        assertThat(failure.getMessage()).contains("STRIPE_SECRET_KEY absente");
        verify(stripe, never()).initiate(any());
    }

    @Test
    @DisplayName("une devise inconnue du catalogue ISO-4217 est refusée AVANT l'appel fournisseur")
    void deviseInconnueEstRefusee() {
        givenActiveProvider(stripe, "stripe", true, null);
        when(currencyValidator.isSupported("XYZ")).thenReturn(false);

        DomainException failure = org.junit.jupiter.api.Assertions.assertThrows(
                DomainException.class,
                () -> service.initiate("stripe", new PayoutRequest(
                        "ref-1", "acct_123", 1000L, "XYZ", "COMM-1", Map.of())));

        assertThat(failure.toProblemDetail().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(failure.getCode()).isEqualTo("PAYOUT_CURRENCY_UNKNOWN");
        verify(stripe, never()).initiate(any());
    }

    @Test
    @DisplayName("la clé d'idempotence est STABLE pour une même référence — un rejeu ne double pas le versement")
    void cleIdempotenceEstStable() {
        givenActiveProvider(stripe, "stripe", true, null);
        when(currencyValidator.isSupported("EUR")).thenReturn(true);
        when(stripe.initiate(any())).thenReturn(PayoutResult.pending("tr_1", "accepté"));

        UUID tenant = UUID.randomUUID();
        java.util.List<String> keys = new java.util.ArrayList<>();

        com.discipolat.common.multitenancy.TenantContext.runAsTenant(tenant, () -> {
            service.initiate("stripe", new PayoutRequest(
                    null, "acct_123", 1000L, "EUR", "COMM-2026-001", Map.of()));
            service.initiate("stripe", new PayoutRequest(
                    null, "acct_123", 1000L, "EUR", "COMM-2026-001", Map.of()));
        });

        org.mockito.ArgumentCaptor<PayoutRequest> captor =
                org.mockito.ArgumentCaptor.forClass(PayoutRequest.class);
        verify(stripe, times(2)).initiate(captor.capture());
        captor.getAllValues().forEach(request -> keys.add(request.idempotencyKey()));

        assertThat(keys.get(0)).isEqualTo(keys.get(1));
        assertThat(keys.get(0)).startsWith("payout-");
    }

    @Test
    @DisplayName("un fournisseur injoignable donne UNKNOWN, jamais une exception")
    void fournisseurInjoignableDonneUnknown() {
        when(registry.findActive("stripe")).thenReturn(java.util.Optional.empty());

        assertThat(service.queryStatus("stripe", "tr_1")).isEqualTo(PayoutStatus.UNKNOWN);
        // Entrées invalides : pas d'exception non plus. Un job de réconciliation
        // doit pouvoir велерouter sans planter.
        assertThat(service.queryStatus(null, "tr_1")).isEqualTo(PayoutStatus.UNKNOWN);
        assertThat(service.queryStatus("stripe", null)).isEqualTo(PayoutStatus.UNKNOWN);
        assertThat(service.queryStatus("", "")).isEqualTo(PayoutStatus.UNKNOWN);
    }

    @Test
    @DisplayName("la vérification de signature est fail-closed sur TOUTE anomalie")
    void signatureEstFailClosed() {
        givenActiveProvider(stripe, "stripe", true, null);

        // Canal inactif → refus immédiat.
        assertThat(service.verifyWebhookSignature("inconnu", Map.of(), "body")).isFalse();

        // Canal actif mais signature invalide → le SPI tranche, on ne l'assouplit pas.
        when(stripe.verifyWebhookSignature(any(), any())).thenReturn(false);
        assertThat(service.verifyWebhookSignature("stripe", Map.of(), "body")).isFalse();

        when(stripe.verifyWebhookSignature(any(), any())).thenReturn(true);
        assertThat(service.verifyWebhookSignature("stripe", Map.of("x", "y"), "body")).isTrue();
    }

    @Test
    @DisplayName("le diagnostic des canaux n'expose aucun secret")
    void diagnosticNExposeAucunSecret() {
        givenActiveProvider(stripe, "stripe", true, null);
        givenActiveProvider(bank, "bank", false, "IBAN_BANK non renseigné");
        when(registry.activeCount()).thenReturn(1);

        Map<String, Object> status = service.channelStatus();

        assertThat(status.get("activeCount")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> channels = (Map<String, Object>) status.get("channels");
        assertThat(channels).containsKeys("stripe", "bank");

        // Sérialisation : aucune valeur ne doit contenir un mot-clé de secret.
        String serialized = channels.toString().toLowerCase();
        assertThat(serialized).doesNotContain("sk_live", "sk_test", "secret", "token", "password");
        assertThat(serialized).contains("iban_bank non renseigné");
    }

    @Test
    @DisplayName("la clé fournie par l'appelant est respectée (traçabilité amont)")
    void cleFournieEstRespectee() {
        givenActiveProvider(stripe, "stripe", true, null);
        when(currencyValidator.isSupported("EUR")).thenReturn(true);
        when(stripe.initiate(any())).thenReturn(PayoutResult.pending("tr_9", "accepté"));

        service.initiate("stripe", new PayoutRequest(
                "cle-amont-42", "acct_123", 1000L, "EUR", "COMM-1", Map.of()));

        org.mockito.ArgumentCaptor<PayoutRequest> captor =
                org.mockito.ArgumentCaptor.forClass(PayoutRequest.class);
        verify(stripe).initiate(captor.capture());
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("cle-amont-42");
    }

    @Test
    @DisplayName("un montant nul ou négatif est refusé par le contrat, avant le service")
    void montantInvalideEstRefuseParLeContrat() {
        assertThatThrownBy(() -> new PayoutRequest(
                "k", "acct", 0L, "EUR", "REF", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountMinor");
    }

    @Test
    @DisplayName("un destinataire vide est refusé — on ne décaisse pas dans le vide")
    void destinataireVideEstRefuse() {
        assertThatThrownBy(() -> new PayoutRequest(
                "k", "  ", 1000L, "EUR", "REF", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipient");
    }
}