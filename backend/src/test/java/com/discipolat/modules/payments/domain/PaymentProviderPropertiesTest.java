package com.discipolat.modules.payments.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentProviderProperties — configuration des passerelles Mobile Money")
class PaymentProviderPropertiesTest {

    private PaymentProviderProperties props;

    @BeforeEach
    void setUp() throws Exception {
        props = new PaymentProviderProperties();
    }

    /** Utilitaire reflection pour injecter un champ private (pas de setter Lombok). */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ==================== Valeurs par défaut ====================

    @Test
    @DisplayName("Par défaut, aucun provider n'est activé")
    void parDefaut_aucunProviderActive() {
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.isOrangeEnabled()).isFalse();
        assertThat(props.isMtnEnabled()).isFalse();
        assertThat(props.isMpesaEnabled()).isFalse();
    }

    // ==================== Orange Money ====================

    @Test
    @DisplayName("isOrangeEnabled : false si global disabled même avec credentials")
    void isOrangeEnabled_falseSiGlobalDisabled() throws Exception {
        setField(props, "enabled", false);
        setField(props, "orangeApiKey", "test-key-123");

        assertThat(props.isOrangeEnabled()).isFalse();
    }

    @Test
    @DisplayName("isOrangeEnabled : false si api-key vide")
    void isOrangeEnabled_falseSiApiKeyVide() throws Exception {
        setField(props, "enabled", true);
        setField(props, "orangeApiKey", "");

        assertThat(props.isOrangeEnabled()).isFalse();
    }

    @Test
    @DisplayName("isOrangeEnabled : false si api-key null")
    void isOrangeEnabled_falseSiApiKeyNull() throws Exception {
        setField(props, "enabled", true);
        setField(props, "orangeApiKey", null);

        assertThat(props.isOrangeEnabled()).isFalse();
    }

    @Test
    @DisplayName("isOrangeEnabled : true si global enabled et api-key présente")
    void isOrangeEnabled_trueSiEnabledEtApiKey() throws Exception {
        setField(props, "enabled", true);
        setField(props, "orangeApiKey", "orange-api-key-abc");

        assertThat(props.isOrangeEnabled()).isTrue();
    }

    // ==================== MTN MoMo ====================

    @Test
    @DisplayName("isMtnEnabled : false si global disabled")
    void isMtnEnabled_falseSiGlobalDisabled() throws Exception {
        setField(props, "enabled", false);
        setField(props, "mtnSubscriptionKey", "sub-key");
        setField(props, "mtnClientId", "client-id");

        assertThat(props.isMtnEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMtnEnabled : false si subscription-key manquante")
    void isMtnEnabled_falseSiSubscriptionKeyManquante() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mtnSubscriptionKey", null);
        setField(props, "mtnClientId", "client-id");

        assertThat(props.isMtnEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMtnEnabled : false si client-id manquant")
    void isMtnEnabled_falseSiClientIdManquant() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mtnSubscriptionKey", "sub-key");
        setField(props, "mtnClientId", null);

        assertThat(props.isMtnEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMtnEnabled : false si client-id vide (blanc)")
    void isMtnEnabled_falseSiClientIdVide() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mtnSubscriptionKey", "sub-key");
        setField(props, "mtnClientId", "  ");

        assertThat(props.isMtnEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMtnEnabled : true si global enabled et toutes les credentials présentes")
    void isMtnEnabled_trueSiEnabledEtCredentials() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mtnSubscriptionKey", "mtn-sub-key");
        setField(props, "mtnClientId", "mtn-client-id");

        assertThat(props.isMtnEnabled()).isTrue();
    }

    // ==================== M-Pesa ====================

    @Test
    @DisplayName("isMpesaEnabled : false si global disabled")
    void isMpesaEnabled_falseSiGlobalDisabled() throws Exception {
        setField(props, "enabled", false);
        setField(props, "mpesaConsumerKey", "consumer-key");
        setField(props, "mpesaConsumerSecret", "consumer-secret");
        setField(props, "mpesaShortCode", "short-code");

        assertThat(props.isMpesaEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMpesaEnabled : false si consumer-key manquante")
    void isMpesaEnabled_falseSiConsumerKeyManquante() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mpesaConsumerKey", null);
        setField(props, "mpesaConsumerSecret", "secret");
        setField(props, "mpesaShortCode", "code");

        assertThat(props.isMpesaEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMpesaEnabled : false si consumer-secret manquant")
    void isMpesaEnabled_falseSiConsumerSecretManquant() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mpesaConsumerKey", "key");
        setField(props, "mpesaConsumerSecret", null);
        setField(props, "mpesaShortCode", "code");

        assertThat(props.isMpesaEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMpesaEnabled : false si short-code manquant")
    void isMpesaEnabled_falseSiShortCodeManquant() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mpesaConsumerKey", "key");
        setField(props, "mpesaConsumerSecret", "secret");
        setField(props, "mpesaShortCode", null);

        assertThat(props.isMpesaEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMpesaEnabled : false si short-code vide (blanc)")
    void isMpesaEnabled_falseSiShortCodeVide() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mpesaConsumerKey", "key");
        setField(props, "mpesaConsumerSecret", "secret");
        setField(props, "mpesaShortCode", "   ");

        assertThat(props.isMpesaEnabled()).isFalse();
    }

    @Test
    @DisplayName("isMpesaEnabled : true si global enabled et toutes les credentials présentes")
    void isMpesaEnabled_trueSiEnabledEtCredentials() throws Exception {
        setField(props, "enabled", true);
        setField(props, "mpesaConsumerKey", "mpesa-key");
        setField(props, "mpesaConsumerSecret", "mpesa-secret");
        setField(props, "mpesaShortCode", "mpesa-code");

        assertThat(props.isMpesaEnabled()).isTrue();
    }

    // ==================== Cas limites ====================

    @Test
    @DisplayName("Les trois providers peuvent être activés simultanément")
    void troisProvidersActivesSimultanement() throws Exception {
        setField(props, "enabled", true);
        setField(props, "orangeApiKey", "orange-key");
        setField(props, "mtnSubscriptionKey", "mtn-key");
        setField(props, "mtnClientId", "mtn-client");
        setField(props, "mpesaConsumerKey", "mpesa-key");
        setField(props, "mpesaConsumerSecret", "mpesa-secret");
        setField(props, "mpesaShortCode", "mpesa-code");

        assertThat(props.isOrangeEnabled()).isTrue();
        assertThat(props.isMtnEnabled()).isTrue();
        assertThat(props.isMpesaEnabled()).isTrue();
    }

    @Test
    @DisplayName("Getters Lombok retournent les valeurs des champs")
    void gettersLombok_retournentValeursDesChamps() throws Exception {
        setField(props, "orangeBaseUrl", "https://custom-orange.com");
        setField(props, "mtnBaseUrl", "https://custom-mtn.com");
        setField(props, "mpesaBaseUrl", "https://custom-mpesa.com");
        setField(props, "orangeReturnUrl", "https://myapp.com/giving");
        setField(props, "orangeWebhookSecret", "wh-secret");

        assertThat(props.getOrangeBaseUrl()).isEqualTo("https://custom-orange.com");
        assertThat(props.getMtnBaseUrl()).isEqualTo("https://custom-mtn.com");
        assertThat(props.getMpesaBaseUrl()).isEqualTo("https://custom-mpesa.com");
        assertThat(props.getOrangeReturnUrl()).isEqualTo("https://myapp.com/giving");
        assertThat(props.getOrangeWebhookSecret()).isEqualTo("wh-secret");
    }
}
