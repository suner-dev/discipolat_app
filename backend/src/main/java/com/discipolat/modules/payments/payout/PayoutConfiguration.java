package com.discipolat.modules.payments.payout;

import com.discipolat.modules.payments.domain.MobileMoneyProvider;
import com.discipolat.modules.payments.domain.PaymentIntent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A3 (M8) — Câblage Spring du SPI de décaissement.
 *
 * <p>Les providers sont déclarés en beans même désactivés : {@code isEnabled()}
 * reflète la configuration au moment de l'usage, et {@link PayoutProviderRegistry}
 * ne retient que les actifs. Les implémentations restent des classes simples
 * (pas d'annotation Spring) pour rester testables sans contexte.</p>
 *
 * <p>Les adaptateurs Mobile Money sont résolus par {@code operator()} : les
 * classes opérateurs sont package-private dans {@code modules.payments.domain}
 * et l'injection par {@code List<MobileMoneyProvider>} évite de casser cette
 * encapsulation.</p>
 */
@Configuration
@EnableConfigurationProperties(PayoutProvidersProperties.class)
public class PayoutConfiguration {

    @Bean
    public HttpClient payoutHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Bean
    public StripePayoutProvider stripePayoutProvider(PayoutProvidersProperties properties,
                                                     HttpClient httpClient,
                                                     ObjectMapper objectMapper) {
        return new StripePayoutProvider(properties.getStripe(), httpClient, objectMapper);
    }

    @Bean
    public PayPalPayoutProvider payPalPayoutProvider(PayoutProvidersProperties properties,
                                                     HttpClient httpClient,
                                                     ObjectMapper objectMapper) {
        return new PayPalPayoutProvider(properties.getPaypal(), httpClient, objectMapper);
    }

    @Bean
    public SepaDirectDebitProvider sepaDirectDebitProvider(PayoutProvidersProperties properties,
                                                           HttpClient httpClient,
                                                           ObjectMapper objectMapper) {
        return new SepaDirectDebitProvider(properties.getSepa(), httpClient, objectMapper);
    }

    @Bean
    public BankTransferProvider bankTransferProvider(PayoutProvidersProperties properties) {
        return new BankTransferProvider(properties.getBank());
    }

    @Bean
    public PayoutProvider mtnPayoutProvider(PayoutProvidersProperties properties,
                                            List<MobileMoneyProvider> mobileMoneyProviders) {
        return new MobileMoneyPayoutAdapter("mtn", PaymentIntent.Operator.MTN_MOMO,
                byOperator(mobileMoneyProviders, PaymentIntent.Operator.MTN_MOMO), properties.getMtn());
    }

    @Bean
    public PayoutProvider orangePayoutProvider(PayoutProvidersProperties properties,
                                               List<MobileMoneyProvider> mobileMoneyProviders) {
        return new MobileMoneyPayoutAdapter("orange", PaymentIntent.Operator.ORANGE_MONEY,
                byOperator(mobileMoneyProviders, PaymentIntent.Operator.ORANGE_MONEY), properties.getOrange());
    }

    @Bean
    public PayoutProvider mpesaPayoutProvider(PayoutProvidersProperties properties,
                                              List<MobileMoneyProvider> mobileMoneyProviders) {
        return new MobileMoneyPayoutAdapter("mpesa", PaymentIntent.Operator.M_PESA,
                byOperator(mobileMoneyProviders, PaymentIntent.Operator.M_PESA), properties.getMpesa());
    }

    private static MobileMoneyProvider byOperator(List<MobileMoneyProvider> providers,
                                                  PaymentIntent.Operator operator) {
        return providers.stream()
                .filter(p -> p.operator() == operator)
                .findFirst()
                .orElse(null);   // l'adaptateur traite l'absence (« non configuré », 503 explicite)
    }
}
