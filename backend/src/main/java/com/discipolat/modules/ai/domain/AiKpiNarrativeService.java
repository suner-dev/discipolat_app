package com.discipolat.modules.ai.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AiKpiNarrativeService {

    private final SoulRepository soulRepo;
    private final FamilyRepository familyRepo;
    private final MakerReportRepository reportRepo;
    private final MemberPresenceRepository presenceRepo;

    public AiKpiNarrativeService(SoulRepository soulRepo, FamilyRepository familyRepo,
                                 MakerReportRepository reportRepo, MemberPresenceRepository presenceRepo) {
        this.soulRepo = soulRepo;
        this.familyRepo = familyRepo;
        this.reportRepo = reportRepo;
        this.presenceRepo = presenceRepo;
    }

    public Map<String, Object> generateNarrative() {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(7);
        LocalDate monthStart = now.minusDays(30);

        long totalSouls = soulRepo.countByTenantId(tenantId);
        long totalFamilies = familyRepo.countByTenantId(tenantId);
        long newSoulsWeek = soulRepo.countByTenantIdAndDateIntegrationAfter(tenantId, weekStart);
        long newSoulsMonth = soulRepo.countByTenantIdAndDateIntegrationAfter(tenantId, monthStart);
        long activeReports = reportRepo.countByTenantIdAndCreatedAtAfter(tenantId, weekStart);
        double presenceRate = presenceRepo.calculatePresenceRate(tenantId, weekStart, now);

        Map<String, Object> narrative = new LinkedHashMap<>();
        narrative.put("period", Map.of("start", weekStart.toString(), "end", now.toString()));
        narrative.put("headline", generateHeadline(totalSouls, newSoulsWeek, presenceRate));
        narrative.put("summary", generateSummary(totalSouls, totalFamilies, newSoulsWeek, newSoulsMonth, activeReports, presenceRate));
        narrative.put("highlights", generateHighlights(newSoulsWeek, presenceRate, activeReports));
        narrative.put("concerns", generateConcerns(presenceRate, activeReports, totalSouls));
        narrative.put("recommendations", generateRecommendations(presenceRate, newSoulsWeek, activeReports));

        return narrative;
    }

    private String generateHeadline(long totalSouls, long newSoulsWeek, double presenceRate) {
        if (newSoulsWeek > 0 && presenceRate >= 70) {
            return "Excellente semaine : " + newSoulsWeek + " nouvelles âmes et " + Math.round(presenceRate) + "% de présence";
        } else if (newSoulsWeek > 0) {
            return "Croissance encourageante avec " + newSoulsWeek + " nouvelles âmes cette semaine";
        } else if (presenceRate >= 70) {
            return "Bonne assiduité avec " + Math.round(presenceRate) + "% de présence";
        }
        return "Semaine stable — " + totalSouls + " âmes dans le troupeau";
    }

    private Map<String, Object> generateSummary(long totalSouls, long totalFamilies, long newSoulsWeek, long newSoulsMonth, long activeReports, double presenceRate) {
        return Map.of(
            "totalSouls", totalSouls,
            "totalFamilies", totalFamilies,
            "newSoulsWeek", newSoulsWeek,
            "newSoulsMonth", newSoulsMonth,
            "activeReports", activeReports,
            "presenceRate", Math.round(presenceRate)
        );
    }

    private List<String> generateHighlights(long newSoulsWeek, double presenceRate, long activeReports) {
        List<String> highlights = new ArrayList<>();
        if (newSoulsWeek >= 3) highlights.add("🔥 " + newSoulsWeek + " nouvelles intégrations cette semaine");
        if (presenceRate >= 80) highlights.add("✅ Excellente présence : " + Math.round(presenceRate) + "%");
        if (activeReports >= 5) highlights.add("📋 " + activeReports + " rapports de suivi actifs");
        if (highlights.isEmpty()) highlights.add("📊 Semaine stable sans événement majeur");
        return highlights;
    }

    private List<String> generateConcerns(double presenceRate, long activeReports, long totalSouls) {
        List<String> concerns = new ArrayList<>();
        if (presenceRate < 50) concerns.add("⚠️ Présence faible : " + Math.round(presenceRate) + "% — action recommandée");
        if (activeReports < totalSouls * 0.1 && totalSouls > 10) concerns.add("📋 Peu de rapports de suivi par rapport au nombre d'âmes");
        return concerns;
    }

    private List<String> generateRecommendations(double presenceRate, long newSoulsWeek, long activeReports) {
        List<String> recs = new ArrayList<>();
        if (presenceRate < 60) recs.add("Organiser un événement spécial pour relancer l'engagement");
        if (newSoulsWeek > 0) recs.add("Assurer le suivi des " + newSoulsWeek + " nouvelles intégrations");
        if (activeReports < 3) recs.add("Encourager les faiseurs à soumettre leurs rapports");
        recs.add("Continuer le suivi pastoral régulier");
        return recs;
    }
}
