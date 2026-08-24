package com.discipolat.modules.predictions.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class PredictionService {

    private final PredictionRepository predictionRepo;

    public PredictionService(PredictionRepository predictionRepo) {
        this.predictionRepo = predictionRepo;
    }

    public List<Prediction> listAll() {
        return predictionRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public List<Prediction> listByType(Prediction.Type type) {
        return predictionRepo.findByTenantIdAndPredictionTypeOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), type);
    }

    public Prediction generatePrediction(Prediction.Type type, Map<String, Object> historicalData) {
        Prediction prediction = new Prediction();
        prediction.setTenantId(TenantContext.getCurrentTenantId());
        prediction.setPredictionType(type);
        prediction.setCreatedAt(LocalDateTime.now());
        prediction.setPeriodStart(LocalDateTime.now());
        prediction.setPeriodEnd(LocalDateTime.now().plusMonths(prediction.getPeriodMonths()));

        // Simple linear regression on provided data
        double[] values = extractValues(historicalData);
        if (values.length >= 2) {
            double current = values[values.length - 1];
            double previous = values[values.length - 2];
            double avgGrowth = calculateAvgGrowth(values);
            
            prediction.setCurrentValue(current);
            prediction.setPredictedValue(current * (1 + avgGrowth));
            prediction.setGrowthRate(Math.round(avgGrowth * 10000.0) / 100.0);
            prediction.setTrend(avgGrowth > 0.02 ? Prediction.Trend.UP : avgGrowth < -0.02 ? Prediction.Trend.DOWN : Prediction.Trend.STABLE);
            prediction.setConfidence(values.length >= 6 ? Prediction.Confidence.HIGH : values.length >= 3 ? Prediction.Confidence.MEDIUM : Prediction.Confidence.LOW);
        } else {
            prediction.setCurrentValue(0.0);
            prediction.setPredictedValue(0.0);
            prediction.setGrowthRate(0.0);
        }

        prediction.setNarrative(generateNarrative(type, prediction));
        prediction.setFactors(generateFactors(type));
        return predictionRepo.save(prediction);
    }

    public List<Prediction> generateAllPredictions(Map<String, Map<String, Object>> dataByType) {
        List<Prediction> results = new ArrayList<>();
        for (var entry : dataByType.entrySet()) {
            try {
                Prediction.Type type = Prediction.Type.valueOf(entry.getKey());
                results.add(generatePrediction(type, entry.getValue()));
            } catch (IllegalArgumentException ignored) {}
        }
        return results;
    }

    private double[] extractValues(Map<String, Object> data) {
        if (data == null || !data.containsKey("values")) return new double[0];
        Object vals = data.get("values");
        if (vals instanceof List<?> list) {
            return list.stream().mapToDouble(v -> v instanceof Number n ? n.doubleValue() : 0).toArray();
        }
        return new double[0];
    }

    private double calculateAvgGrowth(double[] values) {
        if (values.length < 2) return 0;
        double totalGrowth = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] != 0) {
                totalGrowth += (values[i] - values[i - 1]) / values[i - 1];
            }
        }
        return totalGrowth / (values.length - 1);
    }

    private String generateNarrative(Prediction.Type type, Prediction p) {
        String typeName;
        switch (type) {
            case WORKFORCE_GROWTH: typeName = "effectifs"; break;
            case ATTENDANCE: typeName = "présences"; break;
            case BAPTISMS: typeName = "baptêmes"; break;
            case DROPOUT: typeName = "décrochages"; break;
            case ENGAGEMENT: typeName = "engagement"; break;
            case FINANCES: typeName = "finances"; break;
            default: typeName = "cette métrique";
        }
        if (p.getGrowthRate() > 5) {
            return String.format("Forte croissance prévue pour les %s : +%.1f%% sur %d mois. Tendance positive soutenue.", typeName, p.getGrowthRate(), p.getPeriodMonths());
        } else if (p.getGrowthRate() > 0) {
            return String.format("Croissance modérée prévue pour les %s : +%.1f%% sur %d mois. La tendance est favorable.", typeName, p.getGrowthRate(), p.getPeriodMonths());
        } else if (p.getGrowthRate() > -5) {
            return String.format("Légère baisse prévue pour les %s : %.1f%% sur %d mois. Surveillance recommandée.", typeName, p.getGrowthRate(), p.getPeriodMonths());
        } else {
            return String.format("Baisse significative prévue pour les %s : %.1f%% sur %d mois. Intervention recommandée.", typeName, p.getGrowthRate(), p.getPeriodMonths());
        }
    }

    private String generateFactors(Prediction.Type type) {
        switch (type) {
            case DROPOUT: return "[\"absences_soutenues\",\"manque_contact\",\"score_bas\"]";
            case ATTENDANCE: return "[\"evenements_programmes\",\"saison\",\"météo\"]";
            case FINANCES: return "[\"nombre_membres\",\"tithes_moyennes\",\"evenements\"]";
            case BAPTISMS: return "[\"pipeline_evangelisation\",\"nombre_disciples\",\"preparation\"]";
            default: return "[]";
        }
    }
}
