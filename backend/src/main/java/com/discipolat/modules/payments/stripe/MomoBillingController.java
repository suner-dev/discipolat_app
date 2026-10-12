package com.discipolat.modules.payments.stripe;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.payments.domain.MomoSubscriptionService;
import com.discipolat.modules.payments.domain.SaasMomoSubscription;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Facturation SaaS via Mobile Money (MTN MoMo) — abonnements récurrents.
 * Protégé par rôle tenant admin.
 */
@RestController
@RequestMapping("/api/v1/billing/momo")
@PreAuthorize("@authz.isTenantAdmin()")
public class MomoBillingController {

    private final MomoSubscriptionService momoSubscriptionService;

    public MomoBillingController(MomoSubscriptionService momoSubscriptionService) {
        this.momoSubscriptionService = momoSubscriptionService;
    }

    public record MomoSubscribeRequest(String planKey, String billingCycle, String phone) {
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody MomoSubscribeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        SaasMomoSubscription subscription = momoSubscriptionService.subscribe(
                tenantId, request.planKey(), request.billingCycle(), request.phone());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subscriptionId", subscription.getId().toString());
        body.put("planKey", subscription.getPlanKey());
        body.put("status", subscription.getStatus().name());
        body.put("message", "Demande de paiement Mobile Money envoyée. Validez sur votre téléphone.");
        body.put("providerReference", subscription.getProviderReference());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/subscription")
    public ResponseEntity<Map<String, Object>> currentSubscription() {
        UUID tenantId = TenantContext.requireTenantId();
        // TODO: Implémenter la récupération de l'abonnement MoMo courant
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subscription", null);
        return ResponseEntity.ok(body);
    }
}