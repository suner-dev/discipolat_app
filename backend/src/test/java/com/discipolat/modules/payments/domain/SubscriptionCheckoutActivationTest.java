package com.discipolat.modules.payments.domain;

import com.discipolat.modules.tenants.domain.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Activation d'abonnement à la confirmation du webhook — le point où un paiement
 * CinetPay (OM / MoMo / Wave / Visa) devient un accès réellement ouvert.
 *
 * <p>On vérifie ici les trois promesses métier, sans base ni réseau : une caisse
 * payée active l'abonnement, une caisse déjà active ne l'active pas deux fois
 * (idempotence, les re-jeux de notify sont nombreux), et le prix figé porte la
 * vraie source (MOMO vs CARD). Une intention qui n'est pas un abonnement (dîme,
 * offrande) ne doit rien activer.</p>
 */
@DisplayName("SubscriptionCheckoutService.confirmActivation — activation idempotente")
class SubscriptionCheckoutActivationTest {

    private static final String REF = "CPS_TX_123";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    /** Le constructeur exige 9 collaborators ; seuls les deux-ci comptent pour confirmActivation. */
    private static SubscriptionCheckoutService service(SaasSubscriptionCheckoutRepository repo,
                                                       SubscriptionService subscriptions) {
        return new SubscriptionCheckoutService(null, null, null, null, null, null, repo, subscriptions, null);
    }

    private static SaasSubscriptionCheckout checkout(SubscriptionCheckoutStatus status, String channel) {
        return SaasSubscriptionCheckout.builder()
                .tenantId(TENANT)
                .userId(USER)
                .planKey("STARTUP")
                .billingCycle("MONTHLY")
                .currency("XOF")
                .amountMinor(2900L)
                .paymentChannel(channel)
                .operator("MOMO".equals(channel) ? "ORANGE_MONEY" : "CARD")
                .providerReference(REF)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("Caisse MOMO payée → abonnement activé, priceSource MOMO")
    void caisseMomoPayee_activeAbonnement() {
        SaasSubscriptionCheckoutRepository repo = mock(SaasSubscriptionCheckoutRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        SaasSubscriptionCheckout pending = checkout(SubscriptionCheckoutStatus.PENDING, "MOMO");
        when(repo.findByProviderReference(REF)).thenReturn(Optional.of(pending));

        boolean activated = service(repo, subscriptions).confirmActivation(REF);

        assertThat(activated).isTrue();
        assertThat(pending.getStatus()).isEqualTo(SubscriptionCheckoutStatus.ACTIVE);
        verify(subscriptions).subscribePaid(eq(TENANT), eq("STARTUP"), eq("MONTHLY"), eq(USER), eq("MOMO"));
    }

    @Test
    @DisplayName("Caisse CARD payée → priceSource CARD (pas MOMO)")
    void caisseCartePayee_priceSourceCarte() {
        SaasSubscriptionCheckoutRepository repo = mock(SaasSubscriptionCheckoutRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        when(repo.findByProviderReference(REF))
                .thenReturn(Optional.of(checkout(SubscriptionCheckoutStatus.PENDING, "CARD")));

        assertThat(service(repo, subscriptions).confirmActivation(REF)).isTrue();
        verify(subscriptions).subscribePaid(eq(TENANT), eq("STARTUP"), eq("MONTHLY"), eq(USER), eq("CARD"));
    }

    @Test
    @DisplayName("Déjà ACTIVE → idempotent : on ne souscrit pas deux fois")
    void dejaActive_nonIdempotent() {
        SaasSubscriptionCheckoutRepository repo = mock(SaasSubscriptionCheckoutRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        when(repo.findByProviderReference(REF))
                .thenReturn(Optional.of(checkout(SubscriptionCheckoutStatus.ACTIVE, "MOMO")));

        boolean activated = service(repo, subscriptions).confirmActivation(REF);

        assertThat(activated).isTrue();
        verify(subscriptions, never()).subscribePaid(any(), any(), any(), any(), any());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Référence inconnue (dîme/offrande) → rien activé, faux")
    void referenceInconnue_rienActive() {
        SaasSubscriptionCheckoutRepository repo = mock(SaasSubscriptionCheckoutRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        when(repo.findByProviderReference(REF)).thenReturn(Optional.empty());

        assertThat(service(repo, subscriptions).confirmActivation(REF)).isFalse();
        verify(subscriptions, never()).subscribePaid(any(), any(), any(), any(), any());
    }
}
