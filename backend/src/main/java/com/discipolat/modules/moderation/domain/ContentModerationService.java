package com.discipolat.modules.moderation.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class ContentModerationService {

    private final ModerationItemRepository moderationRepo;

    // Simple rule-based content filters
    private static final Pattern SPAM_PATTERN = Pattern.compile(
        "(buy now|click here|free money|winner|congratulations you won|act now|limited time|100% free)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern HARASSMENT_PATTERN = Pattern.compile(
        "(stupid|idiot|hate you|kill yourself|ugly|worthless|loser)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern INAPPROPRIATE_PATTERN = Pattern.compile(
        "(porn|sex|nude|naked|drugs|alcohol|gambling)",
        Pattern.CASE_INSENSITIVE);

    public ContentModerationService(ModerationItemRepository moderationRepo) {
        this.moderationRepo = moderationRepo;
    }

    public ModerationItem submitForModeration(ModerationItem item) {
        item.setTenantId(TenantContext.getCurrentTenantId());
        item.setStatus(ModerationItem.Status.PENDING);

        // AI analysis (rule-based for now)
        var analysis = analyzeContent(item.getContent());
        item.setAiConfidence((Double) analysis.get("confidence"));
        item.setRiskLevel((ModerationItem.RiskLevel) analysis.get("riskLevel"));
        item.setAiFlags((String) analysis.get("flags"));

        // Auto-approve low risk
        if (item.getRiskLevel() == ModerationItem.RiskLevel.LOW) {
            item.setStatus(ModerationItem.Status.APPROVED);
        } else if (item.getRiskLevel() == ModerationItem.RiskLevel.CRITICAL) {
            item.setStatus(ModerationItem.Status.REJECTED);
        }

        return moderationRepo.save(item);
    }

    public ModerationItem reviewItem(UUID id, ModerationItem.Status decision, String notes, UUID reviewerId) {
        ModerationItem item = moderationRepo.findById(id).orElseThrow();
        item.setStatus(decision);
        item.setModeratorNotes(notes);
        item.setReviewedBy(reviewerId);
        item.setReviewedAt(LocalDateTime.now());
        return moderationRepo.save(item);
    }

    public List<ModerationItem> listPending() {
        return moderationRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), ModerationItem.Status.PENDING);
    }

    public List<ModerationItem> listAll() {
        return moderationRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.PENDING));
        stats.put("approved", moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.APPROVED));
        stats.put("rejected", moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.REJECTED));
        stats.put("flagged", moderationRepo.countByTenantIdAndStatus(tenantId, ModerationItem.Status.FLAGGED));
        stats.put("highRisk", moderationRepo.countByTenantIdAndRiskLevel(tenantId, ModerationItem.RiskLevel.HIGH));
        stats.put("criticalRisk", moderationRepo.countByTenantIdAndRiskLevel(tenantId, ModerationItem.RiskLevel.CRITICAL));
        return stats;
    }

    private Map<String, Object> analyzeContent(String content) {
        Map<String, Object> result = new HashMap<>();
        List<String> flags = new ArrayList<>();
        double score = 1.0;

        if (content == null || content.isBlank()) {
            result.put("confidence", 1.0);
            result.put("riskLevel", ModerationItem.RiskLevel.LOW);
            result.put("flags", "[]");
            return result;
        }

        if (SPAM_PATTERN.matcher(content).find()) {
            flags.add("spam");
            score -= 0.4;
        }
        if (HARASSMENT_PATTERN.matcher(content).find()) {
            flags.add("harassment");
            score -= 0.5;
        }
        if (INAPPROPRIATE_PATTERN.matcher(content).find()) {
            flags.add("inappropriate");
            score -= 0.3;
        }
        if (content.length() > 5000) {
            flags.add("unusually_long");
            score -= 0.1;
        }
        // Excessive caps
        long capsCount = content.chars().filter(Character::isUpperCase).count();
        if (capsCount > content.length() * 0.6 && content.length() > 20) {
            flags.add("excessive_caps");
            score -= 0.15;
        }

        score = Math.max(0, score);
        ModerationItem.RiskLevel level;
        if (score >= 0.8) level = ModerationItem.RiskLevel.LOW;
        else if (score >= 0.5) level = ModerationItem.RiskLevel.MEDIUM;
        else if (score >= 0.3) level = ModerationItem.RiskLevel.HIGH;
        else level = ModerationItem.RiskLevel.CRITICAL;

        result.put("confidence", Math.round(score * 100.0) / 100.0);
        result.put("riskLevel", level);
        result.put("flags", flags.isEmpty() ? "[]" : String.join(",", flags));
        return result;
    }
}
