package com.discipolat.modules.ai.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.QuotaService;
import com.discipolat.modules.tenants.domain.TenantUsageSnapshot;
import com.discipolat.modules.tenants.domain.TenantUsageSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AiCreditsService {

    private final AiUsageRepository aiUsageRepository;
    private final QuotaService quotaService;
    private final TenantUsageSnapshotService usageSnapshotService;

    public AiCreditsService(AiUsageRepository aiUsageRepository,
                            QuotaService quotaService,
                            TenantUsageSnapshotService usageSnapshotService) {
        this.aiUsageRepository = aiUsageRepository;
        this.quotaService = quotaService;
        this.usageSnapshotService = usageSnapshotService;
    }

    /**
     * Check if tenant can make an AI request and consume credits.
     * Throws BusinessRuleException if quota exceeded.
     */
    public void consumeCredits(UUID userId, String requestType, Integer credits, String modelUsed) {
        UUID tenantId = TenantContext.requireTenantId();
        
        quotaService.checkCanConsumeAiCredits(tenantId, credits);
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

    public Map<String, Object> getUsageDashboard(UUID tenantId, LocalDate from, LocalDate to) {
        Integer totalCredits = aiUsageRepository.getTotalCreditsConsumed(tenantId, from, to);
        Long totalRequests = aiUsageRepository.findByTenantIdAndUsageDateBetween(tenantId, from, to).stream().count();
        
        List<Map<String, Object>> byType = aiUsageRepository.getUsageByType(tenantId, from, to);
        List<Map<String, Object>> byModel = aiUsageRepository.getUsageByModel(tenantId, from, to);
        
        TenantUsageSnapshot snapshot = usageSnapshotService.getSnapshotForTenant(tenantId);
        Long monthlyLimit = snapshot.aiCredits().limit();
        long usedThisMonth = snapshot.aiCredits().used();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", Map.of("from", from.toString(), "to", to.toString()));
        result.put("totalCredits", totalCredits != null ? totalCredits : 0);
        result.put("totalRequests", totalRequests);
        result.put("monthlyLimit", monthlyLimit);
        result.put("usedThisMonth", usedThisMonth);
        result.put("remainingThisMonth", monthlyLimit != null ? Math.max(0, monthlyLimit - usedThisMonth) : null);
        result.put("byType", byType);
        result.put("byModel", byModel);
        result.put("dailyUsage", getDailyUsage(tenantId, from, to));
        return result;
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