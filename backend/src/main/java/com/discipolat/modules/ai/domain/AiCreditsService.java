package com.discipolat.modules.ai.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.QuotaService;
import com.discipolat.modules.tenants.domain.SaasPlanRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AiCreditsService {

    private final AiUsageRepository aiUsageRepository;
    private final QuotaService quotaService;
    private final SaasPlanRepository saasPlanRepository;
    private final TenantRepository tenantRepository;

    public AiCreditsService(AiUsageRepository aiUsageRepository,
                            QuotaService quotaService,
                            SaasPlanRepository saasPlanRepository,
                            TenantRepository tenantRepository) {
        this.aiUsageRepository = aiUsageRepository;
        this.quotaService = quotaService;
        this.saasPlanRepository = saasPlanRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Check if tenant can make an AI request and consume credits.
     * Throws BusinessRuleException if quota exceeded.
     */
    public void consumeCredits(UUID userId, String requestType, Integer credits, String modelUsed) {
        UUID tenantId = TenantContext.requireTenantId();
        
        // Check quota first
        quotaService.checkCanMakeAiRequest(tenantId);
        
        // Get monthly limit from plan
        Integer monthlyLimit = getMonthlyAiLimit(tenantId);
        if (monthlyLimit == null) {
            // No limit configured, allow
            recordUsage(tenantId, userId, requestType, credits, modelUsed, null, null, null, true, null);
            return;
        }
        
        // Check current month usage
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        Integer usedCredits = aiUsageRepository.getTotalCreditsConsumed(tenantId, monthStart, monthEnd);
        if (usedCredits == null) usedCredits = 0;
        
        if (usedCredits + credits > monthlyLimit) {
            // Record failed attempt
            recordUsage(tenantId, userId, requestType, credits, modelUsed, null, null, null, false, 
                    "Quota IA mensuel dépassé: " + usedCredits + "/" + monthlyLimit + " crédits utilisés");
            throw new BusinessRuleException(
                    "Quota IA mensuel dépassé: " + usedCredits + "/" + monthlyLimit + " crédits utilisés. " +
                    "Veuillez mettre à jour votre plan pour plus de crédits IA.",
                    "QUOTA_EXCEEDED_AI_CREDITS");
        }
        
        // Record successful usage
        recordUsage(tenantId, userId, requestType, credits, modelUsed, null, null, null, true, null);
    }

    /**
     * Record AI usage (success or failure).
     */
    public void recordUsage(UUID tenantId, UUID userId, String requestType, Integer credits,
                           String modelUsed, Integer tokensInput, Integer tokensOutput,
                           Integer responseTimeMs, Boolean success, String errorMessage) {
        AiUsage usage = AiUsage.builder()
                .tenantId(tenantId)
                .userId(userId)
                .requestType(requestType)
                .creditsConsumed(credits)
                .modelUsed(modelUsed)
                .tokensInput(tokensInput)
                .tokensOutput(tokensOutput)
                .responseTimeMs(responseTimeMs)
                .success(success)
                .errorMessage(errorMessage)
                .usageDate(LocalDate.now())
                .build();
        aiUsageRepository.save(usage);
    }

    /**
     * Get monthly AI credits limit from tenant's plan.
     */
    private Integer getMonthlyAiLimit(UUID tenantId) {
        // Le plan du tenant est porté par Tenant.plan ; TenantSubscription
        // (tenants.subscription) est géré par SubscriptionService côté facturation.
        var plan = tenantRepository.findById(tenantId)
                .map(t -> t.getPlan())
                .flatMap(planKey -> saasPlanRepository.findById(planKey));
        if (plan.isPresent() && plan.get().getLimitsJson() != null) {
            try {
                Map<String, Object> limits = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(plan.get().getLimitsJson(), Map.class);
                if (limits.containsKey("max_ai_requests_month")) {
                    return ((Number) limits.get("max_ai_requests_month")).intValue();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    /**
     * Get AI usage dashboard for admin.
     */
    public Map<String, Object> getUsageDashboard(UUID tenantId, LocalDate from, LocalDate to) {
        Integer totalCredits = aiUsageRepository.getTotalCreditsConsumed(tenantId, from, to);
        Long totalRequests = aiUsageRepository.findByTenantIdAndUsageDateBetween(tenantId, from, to).stream().count();
        
        List<Map<String, Object>> byType = aiUsageRepository.getUsageByType(tenantId, from, to);
        List<Map<String, Object>> byModel = aiUsageRepository.getUsageByModel(tenantId, from, to);
        
        Integer monthlyLimit = getMonthlyAiLimit(tenantId);
        Integer usedThisMonth = 0;
        if (monthlyLimit != null) {
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            usedThisMonth = aiUsageRepository.getTotalCreditsConsumed(tenantId, monthStart, LocalDate.now());
            if (usedThisMonth == null) usedThisMonth = 0;
        }
        
        return Map.of(
                "period", Map.of("from", from.toString(), "to", to.toString()),
                "totalCredits", totalCredits != null ? totalCredits : 0,
                "totalRequests", totalRequests,
                "monthlyLimit", monthlyLimit,
                "usedThisMonth", usedThisMonth,
                "remainingThisMonth", monthlyLimit != null ? Math.max(0, monthlyLimit - usedThisMonth) : null,
                "byType", byType,
                "byModel", byModel,
                "dailyUsage", getDailyUsage(tenantId, from, to)
        );
    }

    private List<Map<String, Object>> getDailyUsage(UUID tenantId, LocalDate from, LocalDate to) {
        // Build daily usage for the period
        List<AiUsage> usages = aiUsageRepository.findByTenantIdAndUsageDateBetween(tenantId, from, to);
        Map<LocalDate, Integer> totals = usages.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AiUsage::getUsageDate,
                        java.util.stream.Collectors.summingInt(AiUsage::getCreditsConsumed)));
        List<Map<String, Object>> daily = new java.util.ArrayList<>();
        for (Map.Entry<LocalDate, Integer> e : totals.entrySet()) {
            daily.add(Map.of("date", e.getKey().toString(), "credits", e.getValue()));
        }
        return daily;
    }

    /**
     * Get user's personal AI usage.
     */
    public List<AiUsage> getUserUsage(UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        return aiUsageRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }

    /**
     * Get tenant's AI usage for admin view.
     */
    public List<AiUsage> getTenantUsage(UUID tenantId) {
        return aiUsageRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}