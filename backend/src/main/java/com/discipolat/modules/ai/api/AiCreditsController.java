package com.discipolat.modules.ai.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.ai.domain.AiCreditsService;
import com.discipolat.modules.ai.domain.AiUsage;
import com.discipolat.modules.ai.domain.AiUsageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/credits")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_ADMIN')")
public class AiCreditsController {

    private final AiCreditsService aiCreditsService;
    private final AiUsageRepository aiUsageRepository;
    private final SecurityUtils securityUtils;

    public AiCreditsController(AiCreditsService aiCreditsService,
                               AiUsageRepository aiUsageRepository,
                               SecurityUtils securityUtils) {
        this.aiCreditsService = aiCreditsService;
        this.aiUsageRepository = aiUsageRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Get AI usage dashboard for current tenant (admin view).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusDays(30);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        return ResponseEntity.ok(aiCreditsService.getUsageDashboard(tenantId, fromDate, toDate));
    }

    /**
     * Get current user's personal AI usage.
     */
    @GetMapping("/my-usage")
    public ResponseEntity<List<AiUsage>> getMyUsage() {
        return ResponseEntity.ok(aiCreditsService.getUserUsage(securityUtils.getCurrentUserId()));
    }

    /**
     * Get tenant's AI usage (paginated) for admin.
     */
    @GetMapping("/tenant-usage")
    public ResponseEntity<Page<AiUsage>> getTenantUsage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(aiUsageRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable));
    }

    /**
     * Get daily AI usage for the current month.
     */
    @GetMapping("/daily-usage")
    public ResponseEntity<List<Map<String, Object>>> getDailyUsage(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        Object dailyUsage = aiCreditsService.getUsageDashboard(tenantId, fromDate, toDate).get("dailyUsage");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) dailyUsage;
        return ResponseEntity.ok(result);
    }
}