package com.discipolat.modules.ai.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AiFamilyCohesionService {

    private final FamilyRepository familyRepo;
    private final SoulRepository soulRepo;
    private final MemberPresenceRepository presenceRepo;

    public AiFamilyCohesionService(FamilyRepository familyRepo, SoulRepository soulRepo, MemberPresenceRepository presenceRepo) {
        this.familyRepo = familyRepo;
        this.soulRepo = soulRepo;
        this.presenceRepo = presenceRepo;
    }

    public Map<String, Object> analyzeFamilyCohesion(UUID familyId) {
        UUID tenantId = TenantContext.requireTenantId();
        Family family = familyRepo.findByIdAndTenantId(familyId, tenantId)
                .orElseThrow(() -> new RuntimeException("Famille non trouvée"));

        List<Soul> members = soulRepo.findByFamilleIdAndTenantId(familyId, tenantId);
        LocalDate now = LocalDate.now();
        LocalDate monthAgo = now.minusDays(30);

        double avgPresence = 0;
        int activeCount = 0;
        int atRiskCount = 0;

        for (Soul member : members) {
            long presences = soulRepo.countPresencesBySoulId(member.getId());
            long events = soulRepo.countTotalEvents();
            double rate = events > 0 ? (double) presences / events * 100 : 0;
            avgPresence += rate;
            if (rate >= 50) activeCount++;
            if (rate < 30) atRiskCount++;
        }

        if (!members.isEmpty()) avgPresence /= members.size();

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("familyId", familyId);
        analysis.put("familyName", family.getNom());
        analysis.put("memberCount", members.size());
        analysis.put("averagePresence", Math.round(avgPresence));
        analysis.put("activeMembers", activeCount);
        analysis.put("atRiskMembers", atRiskCount);
        analysis.put("cohesionScore", calculateCohesionScore(avgPresence, members.size(), atRiskCount));
        analysis.put("cohesionLevel", getCohesionLevel(avgPresence));
        analysis.put("recommendations", generateFamilyRecommendations(avgPresence, atRiskCount, members.size()));

        return analysis;
    }

    public List<Map<String, Object>> analyzeAllFamilies() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Family> families = familyRepo.findByTenantId(tenantId);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Family f : families) {
            try {
                Map<String, Object> analysis = analyzeFamilyCohesion(f.getId());
                results.add(Map.of("familyId", f.getId(), "familyName", f.getNom(),
                    "cohesionScore", analysis.get("cohesionScore"),
                    "cohesionLevel", analysis.get("cohesionLevel"),
                    "memberCount", analysis.get("memberCount")));
            } catch (Exception e) {
                // Ignorer les familles sans données suffisantes
            }
        }
        return results;
    }

    private int calculateCohesionScore(double avgPresence, int memberCount, int atRiskCount) {
        int score = (int) (avgPresence * 0.6);
        if (memberCount > 3) score += 10;
        if (atRiskCount == 0) score += 20;
        else if (atRiskCount == 1) score += 10;
        return Math.min(100, Math.max(0, score));
    }

    private String getCohesionLevel(double avgPresence) {
        if (avgPresence >= 75) return "FORTE";
        if (avgPresence >= 50) return "MOYENNE";
        if (avgPresence >= 25) return "FAIBLE";
        return "CRITIQUE";
    }

    private List<String> generateFamilyRecommendations(double avgPresence, int atRiskCount, int memberCount) {
        List<String> recs = new ArrayList<>();
        if (avgPresence < 50) recs.add("Organiser une rencontre de famille pour renforcer les liens");
        if (atRiskCount > 0) recs.add("Visiter le(s) " + atRiskCount + " membre(s) à risque de décrochage");
        if (memberCount < 3) recs.add("Envisager la fusion avec une autre famille");
        recs.add("Maintenir les moments de prière familiale réguliers");
        return recs;
    }
}
