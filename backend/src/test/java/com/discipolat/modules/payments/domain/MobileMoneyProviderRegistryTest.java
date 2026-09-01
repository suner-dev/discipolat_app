package com.discipolat.modules.payments.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MobileMoneyProviderRegistry — résolution des providers Mobile Money")
class MobileMoneyProviderRegistryTest {

    // ==================== Helpers ====================

    private static MobileMoneyProvider mockProvider(PaymentIntent.Operator operator, boolean enabled) {
        MobileMoneyProvider p = mock(MobileMoneyProvider.class);
        when(p.operator()).thenReturn(operator);
        when(p.isEnabled()).thenReturn(enabled);
        return p;
    }

    // ==================== Tests ====================

    @Test
    @DisplayName("Liste vide → aucun provider actif")
    void listeVide_aucunProviderActif() {
        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(Collections.emptyList());

        assertThat(registry.activeCount()).isZero();
        assertThat(registry.find(PaymentIntent.Operator.MTN_MOMO)).isNull();
        assertThat(registry.find(PaymentIntent.Operator.ORANGE_MONEY)).isNull();
        assertThat(registry.find(PaymentIntent.Operator.M_PESA)).isNull();
    }

    @Test
    @DisplayName("Provider désactivé → ignoré par le registre")
    void providerDesactive_ignore() {
        MobileMoneyProvider disabled = mockProvider(PaymentIntent.Operator.MTN_MOMO, false);

        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(List.of(disabled));

        assertThat(registry.activeCount()).isZero();
        assertThat(registry.find(PaymentIntent.Operator.MTN_MOMO)).isNull();
    }

    @Test
    @DisplayName("Provider null dans la liste → ignoré sans erreur")
    void providerNull_ignore() {
        List<MobileMoneyProvider> providers = new ArrayList<>();
        providers.add(null);
        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(providers);

        assertThat(registry.activeCount()).isZero();
    }

    @Test
    @DisplayName("Provider activé → retrouvable par opérateur")
    void providerActive_retrouvableParOperateur() {
        MobileMoneyProvider orange = mockProvider(PaymentIntent.Operator.ORANGE_MONEY, true);

        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(List.of(orange));

        assertThat(registry.activeCount()).isEqualTo(1);
        assertThat(registry.find(PaymentIntent.Operator.ORANGE_MONEY)).isSameAs(orange);
    }

    @Test
    @DisplayName("Plusieurs providers activés → tous enregistrés")
    void plusieursProvidersActifs_tousEnregistres() {
        MobileMoneyProvider orange = mockProvider(PaymentIntent.Operator.ORANGE_MONEY, true);
        MobileMoneyProvider mtn = mockProvider(PaymentIntent.Operator.MTN_MOMO, true);
        MobileMoneyProvider mpesa = mockProvider(PaymentIntent.Operator.M_PESA, true);

        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(List.of(orange, mtn, mpesa));

        assertThat(registry.activeCount()).isEqualTo(3);
        assertThat(registry.find(PaymentIntent.Operator.ORANGE_MONEY)).isSameAs(orange);
        assertThat(registry.find(PaymentIntent.Operator.MTN_MOMO)).isSameAs(mtn);
        assertThat(registry.find(PaymentIntent.Operator.M_PESA)).isSameAs(mpesa);
    }

    @Test
    @DisplayName("Mélange actif/désactivé/null → seuls les actifs sont retenus")
    void melangeActifDesactiveNull_seulsLesActifsRetenus() {
        MobileMoneyProvider activeOrange = mockProvider(PaymentIntent.Operator.ORANGE_MONEY, true);
        MobileMoneyProvider disabledMtn = mockProvider(PaymentIntent.Operator.MTN_MOMO, false);

        List<MobileMoneyProvider> providers = new ArrayList<>();
        providers.add(activeOrange);
        providers.add(null);
        providers.add(disabledMtn);
        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(providers);

        assertThat(registry.activeCount()).isEqualTo(1);
        assertThat(registry.find(PaymentIntent.Operator.ORANGE_MONEY)).isSameAs(activeOrange);
        assertThat(registry.find(PaymentIntent.Operator.MTN_MOMO)).isNull();
    }

    @Test
    @DisplayName("Deux providers pour le même opérateur → le dernier gagne (EnumMap overwrite)")
    void deuxProvidersMemeOperateur_dernierGagne() {
        MobileMoneyProvider first = mockProvider(PaymentIntent.Operator.MTN_MOMO, true);
        MobileMoneyProvider second = mockProvider(PaymentIntent.Operator.MTN_MOMO, true);

        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(List.of(first, second));

        assertThat(registry.activeCount()).isEqualTo(1);
        assertThat(registry.find(PaymentIntent.Operator.MTN_MOMO)).isSameAs(second);
    }

    @Test
    @DisplayName("find() retourne null pour un opérateur non enregistré")
    void findRetourneNullPourOperateurNonEnregistre() {
        MobileMoneyProvider orange = mockProvider(PaymentIntent.Operator.ORANGE_MONEY, true);

        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(List.of(orange));

        assertThat(registry.find(PaymentIntent.Operator.WAVE)).isNull();
        assertThat(registry.find(PaymentIntent.Operator.CARD)).isNull();
        assertThat(registry.find(PaymentIntent.Operator.CASH)).isNull();
    }

    @Test
    @DisplayName("Registry est immutable après construction")
    void registryImmutableApresConstruction() {
        MobileMoneyProvider orange = mockProvider(PaymentIntent.Operator.ORANGE_MONEY, true);
        List<MobileMoneyProvider> providers = new java.util.ArrayList<>(List.of(orange));

        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(providers);

        // Modifier la liste source ne doit pas affecter le registre
        providers.clear();

        assertThat(registry.activeCount()).isEqualTo(1);
        assertThat(registry.find(PaymentIntent.Operator.ORANGE_MONEY)).isSameAs(orange);
    }
}
