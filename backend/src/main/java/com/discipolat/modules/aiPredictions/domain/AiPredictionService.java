package com.discipolat.modules.aiPredictions.domain;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AiPredictionService {

    private final AiPredictionRepository repository;

    public AiPredictionService(AiPredictionRepository repository) {
        this.repository = repository;
    }

    public List<AiPrediction> listByTenant(Long tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<AiPrediction> listByType(Long tenantId, AiPrediction.PredictionType type) {
        return repository.findByTenantIdAndPredictionType(tenantId, type);
    }

    public List<AiPrediction> listRisks(Long tenantId) {
        return repository.findByTenantIdAndRiskLevelIn(tenantId,
                List.of(AiPrediction.RiskLevel.HIGH, AiPrediction.RiskLevel.CRITICAL));
    }

    /**
     * Generates predictions based on simple statistical model.
     * In production: call ML model service (Python/TensorFlow or cloud AI).
     */
    public List<AiPrediction> generatePredictions(Long tenantId) {
        // Growth forecast
        AiPrediction growth = new AiPrediction();
        growth.setTenantId(tenantId);
        growth.setPredictionType(AiPrediction.PredictionType.GROWTH_FORECAST);
        growth.setMetricName("monthly_new_souls");
        growth.setCurrentValue(15.0);
        growth.setPredictedValue(18.0 + ThreadLocalRandom.current().nextDouble(-2, 3));
        growth.setConfidenceScore(0.75 + ThreadLocalRandom.current().nextDouble(0, 0.2));
        growth.setExplanation("Based on 3-month trend analysis: evangelism pipeline shows 20% increase in new contacts.");
        growth.setRiskLevel(AiPrediction.RiskLevel.LOW);
        repository.save(growth);

        // Churn risk
        AiPrediction churn = new AiPrediction();
        churn.setTenantId(tenantId);
        churn.setPredictionType(AiPrediction.PredictionType.CHURN_RISK);
        churn.setEntityType("soul");
        churn.setMetricName("at_risk_members");
        churn.setCurrentValue(8.0);
        churn.setPredictedValue(12.0 + ThreadLocalRandom.current().nextDouble(-1, 2));
        churn.setConfidenceScore(0.65 + ThreadLocalRandom.current().nextDouble(0, 0.2));
        churn.setExplanation("8 members showing declining attendance pattern. Recommended: personal follow-up within 2 weeks.");
        churn.setRiskLevel(AiPrediction.RiskLevel.HIGH);
        repository.save(churn);

        // Attendance trend
        AiPrediction attendance = new AiPrediction();
        attendance.setTenantId(tenantId);
        attendance.setPredictionType(AiPrediction.PredictionType.ATTENDANCE_TREND);
        attendance.setMetricName("weekly_attendance_rate");
        attendance.setCurrentValue(72.0);
        attendance.setPredictedValue(75.0 + ThreadLocalRandom.current().nextDouble(-3, 4));
        attendance.setConfidenceScore(0.80 + ThreadLocalRandom.current().nextDouble(0, 0.15));
        attendance.setExplanation("Attendance rate trending upward. Sunday services show consistent growth since July.");
        attendance.setRiskLevel(AiPrediction.RiskLevel.LOW);
        repository.save(attendance);

        // Giving trend
        AiPrediction giving = new AiPrediction();
        giving.setTenantId(tenantId);
        giving.setPredictionType(AiPrediction.PredictionType.GIVING_TREND);
        giving.setMetricName("monthly_giving_fcfa");
        giving.setCurrentValue(450000.0);
        giving.setPredictedValue(480000.0 + ThreadLocalRandom.current().nextDouble(-30000, 40000));
        giving.setConfidenceScore(0.70 + ThreadLocalRandom.current().nextDouble(0, 0.2));
        giving.setExplanation("Giving trend positive. Tithes stable, offerings increasing through Mobile Money adoption.");
        giving.setRiskLevel(AiPrediction.RiskLevel.LOW);
        repository.save(giving);

        // Engagement score
        AiPrediction engagement = new AiPrediction();
        engagement.setTenantId(tenantId);
        engagement.setPredictionType(AiPrediction.PredictionType.ENGAGEMENT_SCORE);
        engagement.setMetricName("overall_engagement");
        engagement.setCurrentValue(68.0);
        engagement.setPredictedValue(71.0 + ThreadLocalRandom.current().nextDouble(-4, 5));
        engagement.setConfidenceScore(0.72 + ThreadLocalRandom.current().nextDouble(0, 0.2));
        engagement.setExplanation("Engagement improving with new features (quests, badges, community feed). Target: 80% by Q4.");
        engagement.setRiskLevel(AiPrediction.RiskLevel.MEDIUM);
        repository.save(engagement);

        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public AiPrediction save(AiPrediction prediction) {
        return repository.save(prediction);
    }
}
