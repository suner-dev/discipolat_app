package com.discipolat.modules.payments.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.payments.domain.SubscriptionCheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Caisse d'abonnement SaaS multi-canaux — Orange Money, MTN MoMo, Wave, Visa.
 * TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité T6.
 *
 * <p>Complète (n'écrase pas) la caisse Stripe : là où Stripe n'est pas branché
 * ou où le client règle en Mobile Money, ce endpoint ouvre une caisse CinetPay
 * à redirection externe et renvoie l'URL au client. L'activation de l'abonnement
 * ne se fait PAS ici mais au webhook, quand le prestataire a confirmé le paiement
 * ({@code CinetPayWebhookController}).</p>
 *
 * <p>Protégé par rôle admin du tenant, comme le caisse Stripe existante — un
 * achat d'abonnement est un acte d'administration du compte.</p>
 */
@RestController
@RequestMapping("/api/v1/billing/subscription")
@PreAuthorize("@authz.isTenantAdmin()")
public class SubscriptionCheckoutController {

    private final SubscriptionCheckoutService checkoutService;

    public SubscriptionCheckoutController(SubscriptionCheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Demande de caisse. {@code channel} : {@code CARD} (Visa/Mastercard) ou
     * {@code MOMO} (Mobile Money). {@code operator} optionnel pour préciser
     * l'opérateur mobile ({@code ORANGE_MONEY} / {@code MTN_MOMO} / {@code WAVE}).
     */
    public record CheckoutRequest(String planKey, String billingCycle, String currency,
                                  String region, String channel, String operator, String phone) {
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = checkoutService.checkout(
                tenantId, userId, request.planKey(), request.billingCycle(),
                request.currency(), request.region(), request.channel(),
                request.operator(), request.phone());
        return ResponseEntity.ok(result);
    }

    /** État d'une caisse après redirection — le client boole ce statut pour savoir si son abonnement est actif. */
    @GetMapping("/checkout/{providerReference}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String providerReference) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(checkoutService.statusByReference(providerReference, tenantId));
    }
}
