package com.discipolat.modules.payments.domain;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration des passerelles opérateurs Mobile Money.
 *
 * <p>Chaque opérateur possède son {@code base-url}, ses credentials et son
 * secret de webhook. Tout est injecté via {@code @Value} (convention du
 * projet) et désactivé par défaut : tant qu’aucun credential n’est fourni,
 * le système reste en fallback local et aucun appel opérateur n’est effectué.</p>
 */
@Component
@Getter
public class PaymentProviderProperties {

    /** Bascule générale : passer en mode « appels opérateurs réels ». */
    @Value("${app.mobilemoney.enabled:false}")
    private boolean enabled;

    // ---- Orange Money ----
    @Value("${app.mobilemoney.orange.api-key:}")
    private String orangeApiKey;
    @Value("${app.mobilemoney.orange.merchant-key:}")
    private String orangeMerchantKey;
    @Value("${app.mobilemoney.orange.client-secret:}")
    private String orangeClientSecret;
    @Value("${app.mobilemoney.orange.base-url:https://api.orange.com}")
    private String orangeBaseUrl;
    @Value("${app.mobilemoney.orange.webhook-secret:}")
    private String orangeWebhookSecret;
    @Value("${app.mobilemoney.orange.return-url:http://localhost:5173/giving}")
    private String orangeReturnUrl;
    @Value("${app.mobilemoney.orange.notif-url:}")
    private String orangeNotifUrl;

    // ---- MTN MoMo ----
    @Value("${app.mobilemoney.mtn.base-url:https://api.mtn.com}")
    private String mtnBaseUrl;
    @Value("${app.mobilemoney.mtn.subscription-key:}")
    private String mtnSubscriptionKey;
    @Value("${app.mobilemoney.mtn.client-id:}")
    private String mtnClientId;
    @Value("${app.mobilemoney.mtn.client-secret:}")
    private String mtnClientSecret;
    @Value("${app.mobilemoney.mtn.webhook-secret:}")
    private String mtnWebhookSecret;

    // ---- M-Pesa (Safaricom Daraja) ----
    @Value("${app.mobilemoney.mpesa.base-url:https://sandbox.safaricom.co.ke}")
    private String mpesaBaseUrl;
    @Value("${app.mobilemoney.mpesa.consumer-key:}")
    private String mpesaConsumerKey;
    @Value("${app.mobilemoney.mpesa.consumer-secret:}")
    private String mpesaConsumerSecret;
    @Value("${app.mobilemoney.mpesa.short-code:}")
    private String mpesaShortCode;
    @Value("${app.mobilemoney.mpesa.passkey:}")
    private String mpesaPasskey;
    @Value("${app.mobilemoney.mpesa.webhook-secret:}")
    private String mpesaWebhookSecret;
    @Value("${app.mobilemoney.mpesa.notif-url:}")
    private String mpesaNotifUrl;

    public boolean isOrangeEnabled() {
        return enabled && orangeApiKey != null && !orangeApiKey.isBlank()
                && orangeMerchantKey != null && !orangeMerchantKey.isBlank();
    }

    public boolean isMtnEnabled() {
        return enabled && mtnSubscriptionKey != null && !mtnSubscriptionKey.isBlank()
                && mtnClientId != null && !mtnClientId.isBlank();
    }

    public boolean isMpesaEnabled() {
        return enabled && mpesaConsumerKey != null && !mpesaConsumerKey.isBlank()
                && mpesaConsumerSecret != null && !mpesaConsumerSecret.isBlank()
                && mpesaShortCode != null && !mpesaShortCode.isBlank();
    }
}
