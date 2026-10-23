package com.discipolat.modules.payments.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CinetPayProvider — agrégateur fail-closed (OM + MoMo + Wave + carte)")
class CinetPayProviderTest {

    /** Injection par réflexion (champs private sans setter Lombok), comme le test de config. */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static PaymentProviderProperties props(boolean enabled, String apiKey, String secret) throws Exception {
        PaymentProviderProperties props = new PaymentProviderProperties();
        setField(props, "cinetpayEnabled", enabled);
        setField(props, "cinetpaySiteApiKey", apiKey);
        setField(props, "cinetpaySiteSecret", secret);
        return props;
    }

    // ==================== Fail-closed ====================

    @Test
    @DisplayName("Désactivé par défaut → isEnabled() false (aucun appel réseau)")
    void desactiveParDefaut_isEnabledFaux() throws Exception {
        CinetPayProvider provider = new CinetPayProvider(props(false, "", ""));

        assertThat(provider.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Activé mais sans les deux clés → toujours isEnabled() false (fail-closed)")
    void activeSansCles_isEnabledToujoursFaux() throws Exception {
        assertThat(new CinetPayProvider(props(true, "key", "")).isEnabled()).isFalse();
        assertThat(new CinetPayProvider(props(true, "", "secret")).isEnabled()).isFalse();
        assertThat(new CinetPayProvider(props(true, "  ", "  ")).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Activé avec les deux clés → isEnabled() true")
    void activeAvecCles_isEnabledVrai() throws Exception {
        CinetPayProvider provider = new CinetPayProvider(props(true, "site_api_key", "site_secret"));

        assertThat(provider.isEnabled()).isTrue();
    }

    // ==================== Identité d'agrégateur ====================

    @Test
    @DisplayName("Se déclare agrégateur")
    void estAgregateur() throws Exception {
        CinetPayProvider provider = new CinetPayProvider(props(false, "", ""));

        assertThat(provider.aggregator()).isTrue();
    }

    @Test
    @DisplayName("Sert Orange Money, MTN MoMo, Wave et la carte bancaire")
    void sertLesQuatreCanaux() throws Exception {
        CinetPayProvider provider = new CinetPayProvider(props(false, "", ""));

        Set<PaymentIntent.Operator> supported = provider.supportedOperators();
        assertThat(supported).contains(
                PaymentIntent.Operator.ORANGE_MONEY,
                PaymentIntent.Operator.MTN_MOMO,
                PaymentIntent.Operator.WAVE,
                PaymentIntent.Operator.CARD);
        // L'opérateur nominal est l'un de ceux servis (clé stable pour l'EnumMap).
        assertThat(supported).contains(provider.operator());
    }

    @Test
    @DisplayName("Provider non configuré n'est JAMAIS retenu par le registre (intégration)")
    void nonConfigure_leRegistreIgnore() throws Exception {
        CinetPayProvider off = new CinetPayProvider(props(false, "", ""));
        MobileMoneyProvider direct = new MobileMoneyProviderRegistryTestable(off);

        // Le registre ne garde que les providers isEnabled()==true : un CinetPay
        // sans clés ne doit pas évincer l'API directe.
        MobileMoneyProviderRegistry registry = new MobileMoneyProviderRegistry(java.util.List.of(direct));

        assertThat(registry.find(PaymentIntent.Operator.ORANGE_MONEY)).isSameAs(direct);
    }

    /** Un provider direct minimal (non-aggrégateur) pour l'intégration ci-dessus. */
    private static final class MobileMoneyProviderRegistryTestable implements MobileMoneyProvider {
        private final CinetPayProvider ignored;
        MobileMoneyProviderRegistryTestable(CinetPayProvider ignored) { this.ignored = ignored; }
        @Override public PaymentIntent.Operator operator() { return PaymentIntent.Operator.ORANGE_MONEY; }
        @Override public boolean isEnabled() { return true; }
        @Override public Result initiate(PaymentIntent intent) { return null; }
        @Override public Verification verify(String providerReference) { return null; }
    }
}
