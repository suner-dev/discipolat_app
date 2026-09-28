package com.discipolat.modules.payments.stripe;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.TenantSubscription;
import com.discipolat.modules.tenants.domain.TenantSubscriptionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Facturation SaaS côté tenant (admin) : passage en caisse Stripe et portail
 * client. Désactivé tant que STRIPE_SECRET_KEY n'est pas configurée — le
 * frontend masque les CTA via /api/v1/public/billing/status.
 */
@RestController
@RequestMapping("/api/v1/billing/stripe")
@PreAuthorize("@authz.isTenantAdmin()")
public class StripeBillingController {

    private final StripeBillingService billingService;
    private final StripeBillingProperties props;
    private final TenantSubscriptionRepository subscriptionRepository;

    public StripeBillingController(StripeBillingService billingService,
                                   StripeBillingProperties props,
                                   TenantSubscriptionRepository subscriptionRepository) {
        this.billingService = billingService;
        this.props = props;
        this.subscriptionRepository = subscriptionRepository;
    }

    public record CheckoutRequest(String planKey, String billingCycle) {
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = billingService.createCheckoutSession(
                tenantId, userId, request.planKey(), request.billingCycle());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/portal")
    public ResponseEntity<Map<String, Object>> portal() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(billingService.createPortalSession(tenantId));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", props.isEnabled());
        body.put("webhookReady", props.isWebhookEnabled());
        return ResponseEntity.ok(body);
    }

    /** Abonnement courant du tenant (page Facturation) — sans données sensibles. */
    @GetMapping("/subscription")
    public ResponseEntity<Map<String, Object>> currentSubscription() {
        UUID tenantId = TenantContext.requireTenantId();
        Map<String, Object> body = new LinkedHashMap<>();
        TenantSubscription sub = subscriptionRepository.findCurrentByTenantId(tenantId).orElse(null);
        if (sub == null) {
            body.put("subscription", null);
            return ResponseEntity.ok(body);
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("planKey", sub.getPlanKey());
        view.put("billingCycle", sub.getBillingCycle());
        view.put("status", sub.getStatus() != null ? sub.getStatus().name() : null);
        view.put("cancelAtPeriodEnd", sub.getCancelAtPeriodEnd());
        view.put("currentPeriodStart", sub.getCurrentPeriodStart() != null ? sub.getCurrentPeriodStart().toString() : null);
        view.put("currentPeriodEnd", sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd().toString() : null);
        view.put("trialEndsAt", sub.getTrialEndsAt() != null ? sub.getTrialEndsAt().toString() : null);
        view.put("hasStripeCustomer", sub.getStripeCustomerId() != null && !sub.getStripeCustomerId().isBlank());
        body.put("subscription", view);
        return ResponseEntity.ok(body);
    }
}
