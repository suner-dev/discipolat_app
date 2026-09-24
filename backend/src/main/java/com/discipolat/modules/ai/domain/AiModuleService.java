package com.discipolat.modules.ai.domain;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.aiPredictions.domain.AiPrediction;
import com.discipolat.modules.aiPredictions.domain.AiPredictionService;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.FeatureAccessService;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class AiModuleService {

    private final FeatureAccessService featureAccess;
    private final PlatformFeatureFlagService featureFlagService;
    private final TenantMembershipRepository membershipRepo;
    private final AiPredictionService predictionService;
    private final SoulRepository soulRepo;
    private final FamilyRepository familyRepo;
    private final MakerReportRepository reportRepo;

    public AiModuleService(FeatureAccessService featureAccess,
                           PlatformFeatureFlagService featureFlagService,
                           TenantMembershipRepository membershipRepo,
                           AiPredictionService predictionService,
                           SoulRepository soulRepo,
                           FamilyRepository familyRepo,
                           MakerReportRepository reportRepo) {
        this.featureAccess = featureAccess;
        this.featureFlagService = featureFlagService;
        this.membershipRepo = membershipRepo;
        this.predictionService = predictionService;
        this.soulRepo = soulRepo;
        this.familyRepo = familyRepo;
        this.reportRepo = reportRepo;
    }

    public void requireAiAccess() {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.AI_ENABLED);
        UUID tenantId = TenantContext.requireTenantId();
        featureAccess.requireFeature(tenantId, "ai_copilot");
    }

    public void requireAiPermission() {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.AI_ENABLED);
        UUID userId = TenantContext.getCurrentUserId();
        UUID tenantId = TenantContext.requireTenantId();
        if (userId == null) throw new SecurityException("Authentification requise");
        boolean hasPerm = membershipRepo.findByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE).isPresent();
        if (!hasPerm) throw new SecurityException("Accès IA refusé");
    }

    public Map<String, Object> getTenantAiSummary() {
        requireAiAccess();
        UUID tenantId = TenantContext.requireTenantId();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenantId);
        summary.put("totalSouls", soulRepo.countByTenantId(tenantId));
        summary.put("totalFamilies", familyRepo.countByTenantId(tenantId));
        try {
            List<AiPrediction> predictions = predictionService.generatePredictions(tenantId);
            AiPrediction growth = predictions.stream()
                    .filter(p -> p.getPredictionType() == AiPrediction.PredictionType.GROWTH_FORECAST)
                    .findFirst()
                    .orElse(null);
            if (growth != null) {
                summary.put("growthPrediction", Map.of("value", growth.getPredictedValue(), "metricName", growth.getMetricName(), "explanation", growth.getExplanation()));
            } else {
                summary.put("growthPrediction", Map.of("error", "Prédiction non disponible"));
            }
        } catch (Exception e) {
            summary.put("growthPrediction", Map.of("error", "Prédiction non disponible"));
        }
        return summary;
    }

    public Map<String, Object> analyzeSoul(UUID soulId) {
        requireAiAccess();
        UUID tenantId = TenantContext.requireTenantId();
        Soul soul = soulRepo.findByIdAndTenantId(soulId, tenantId)
                .orElseThrow(() -> new RuntimeException("Âme non trouvée"));
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("soulId", soulId);
        analysis.put("nom", soul.getNom() + " " + soul.getPrenom());
        analysis.put("statut", soul.getStatut());
        analysis.put("typeDisciple", soul.getTypeDisciple());
        long presences = soulRepo.countPresencesBySoulId(soulId);
        long totalEvents = soulRepo.countTotalEvents();
        double presenceRate = totalEvents > 0 ? (double) presences / totalEvents * 100 : 0;
        analysis.put("presenceRate", Math.round(presenceRate));
        analysis.put("presenceLevel", presenceRate >= 75 ? "ÉLEVÉ" : presenceRate >= 50 ? "MOYEN" : "FAIBLE");
        analysis.put("churnRisk", presenceRate < 30 ? "ÉLEVÉ" : presenceRate < 60 ? "MODÉRÉ" : "FAIBLE");
        List<String> recommendations = new ArrayList<>();
        if (presenceRate < 50) recommendations.add("Visite de suivi recommandée");
        if (presenceRate < 30) recommendations.add("Relance urgente par le faiseur");
        if (soul.getTypeDisciple() == null) recommendations.add("Définir le type de disciple");
        if (recommendations.isEmpty()) recommendations.add("Continuer le suivi régulier");
        analysis.put("recommendations", recommendations);
        return analysis;
    }

    public String generateEncouragement(UUID soulId) {
        requireAiAccess();
        UUID tenantId = TenantContext.requireTenantId();
        Soul soul = soulRepo.findByIdAndTenantId(soulId, tenantId)
                .orElseThrow(() -> new RuntimeException("Âme non trouvée"));
        String prenom = soul.getPrenom() != null ? soul.getPrenom() : "cher(e) disciple";
        String statut = soul.getStatut() != null ? soul.getStatut().name() : "EN_CROISSANCE";
        return switch (statut) {
            case "ACTIF", "EN_CROISSANCE" -> "🌟 " + prenom + " votre progression est remarquable ! Continuez à grandir dans la foi.";
            case "NOUVEL_ARRIVANT" -> "🌱 Bienvenue " + prenom + " ! Les premiers pas sont les plus importants.";
            case "INACTIF" -> "💙 " + prenom + " nous pensons à vous ! Votre place est importante.";
            default -> "🙏 " + prenom + " que Dieu bénisse votre cheminement.";
        };
    }
}
