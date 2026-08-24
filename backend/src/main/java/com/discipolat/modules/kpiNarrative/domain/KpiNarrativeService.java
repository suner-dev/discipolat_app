package com.discipolat.modules.kpiNarrative.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service de narration automatique pour les KPIs.
 *
 * Quand un utilisateur clique sur un KPI, le moteur :
 * 1. Compare la valeur actuelle à la précédente
 * 2. Calcule la tendance et la variation
 * 3. Génère une narration en langage naturel
 * 4. Identifie les causes probables
 * 5. Suggère des actions correctives
 */
@Service
@Transactional
public class KpiNarrativeService {

    private final KpiNarrativeRepository repository;

    public KpiNarrativeService(KpiNarrativeRepository repository) {
        this.repository = repository;
    }

    /**
     * Génère une narration pour un KPI donné.
     *
     * @param typeKPI         Type de KPI
     * @param valeurActuelle  Valeur actuelle
     * @param valeurPrécédente Valeur de la période précédente
     * @param départementId   Département concerné (null = global)
     * @param contexte        Données contextuelles supplémentaires
     */
    public KpiNarrative generate(KpiNarrative.TypeKPI typeKPI, double valeurActuelle, double valeurPrécédente,
                                  UUID départementId, Map<String, Object> contexte) {
        KpiNarrative narrative = new KpiNarrative();
        narrative.setTenantId(TenantContext.getCurrentTenantId());
        narrative.setTypeKPI(typeKPI);
        narrative.setPériode(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        narrative.setValeurActuelle(valeurActuelle);
        narrative.setValeurPrécédente(valeurPrécédente);
        narrative.setDépartementId(départementId);

        // Calculate variation
        double variation = valeurPrécédente > 0 ? ((valeurActuelle - valeurPrécédente) / valeurPrécédente) * 100 : 0;
        narrative.setVariationPct(Math.round(variation * 10.0) / 10.0);

        // Determine trend
        if (variation > 10) narrative.setTendance(KpiNarrative.Tendance.SIGNIFICATIVE_HAUSSE);
        else if (variation > 0) narrative.setTendance(KpiNarrative.Tendance.HAUSSE);
        else if (variation < -10) narrative.setTendance(KpiNarrative.Tendance.SIGNIFICATIVE_BAISSE);
        else if (variation < 0) narrative.setTendance(KpiNarrative.Tendance.BAISSE);
        else narrative.setTendance(KpiNarrative.Tendance.STABLE);

        // Generate narrative text
        narrative.setNarration(générerNarration(typeKPI, valeurActuelle, variation, narrative.getTendance(), contexte));
        narrative.setCauses(générerCauses(typeKPI, variation, contexte));
        narrative.setRecommandations(générerRecommandations(typeKPI, variation, contexte));

        return repository.save(narrative);
    }

    public List<KpiNarrative> listByType(KpiNarrative.TypeKPI type) {
        return repository.findByTenantIdAndTypeKPIOrderByGénéréLeDesc(TenantContext.getCurrentTenantId(), type);
    }

    public List<KpiNarrative> listByPériode(String période) {
        return repository.findByTenantIdAndPériodeOrderByGénéréLeDesc(TenantContext.getCurrentTenantId(), période);
    }

    public List<KpiNarrative> listAll() {
        return repository.findByTenantIdOrderByGénéréLeDesc(TenantContext.getCurrentTenantId());
    }

    /**
     * Génère automatiquement des narrations pour tous les KPIs clés.
     * Appelé par un scheduler quotidien/hebdomadaire.
     */
    public List<KpiNarrative> generateAll(Map<KpiNarrative.TypeKPI, double[]> kpisData) {
        List<KpiNarrative> narratives = new ArrayList<>();
        for (Map.Entry<KpiNarrative.TypeKPI, double[]> entry : kpisData.entrySet()) {
            double[] values = entry.getValue(); // [actuel, précédent]
            if (values.length >= 2) {
                narratives.add(generate(entry.getKey(), values[0], values[1], null, Map.of()));
            }
        }
        return narratives;
    }

    // ── Moteur de narration ───────────────────────────────────────

    private String générerNarration(KpiNarrative.TypeKPI type, double valeur, double variation,
                                     KpiNarrative.Tendance tendance, Map<String, Object> ctx) {
        String kpiName = switch (type) {
            case PRÉSENCE -> "Le taux de présence";
            case CROISSANCE -> "La croissance des membres";
            case RÉTENTION -> "Le taux de rétention";
            case ENGAGEMENT -> "Le score d'engagement";
            case FINANCES -> "Les revenus";
            case RAPPORTS -> "Le taux de soumission des rapports";
            case PRIÈRES -> "L'activité de prière";
            case ÉVÉNEMENTS -> "La participation aux événements";
            case SCORE_SPIRITUEL -> "Le score spirituel moyen";
            case ALERTES -> "Le nombre d'alertes";
        };

        String tendanceText = switch (tendance) {
            case SIGNIFICATIVE_HAUSSE -> "a augmenté de manière significative";
            case HAUSSE -> "a légèrement augmenté";
            case BAISSE -> "a légèrement diminué";
            case SIGNIFICATIVE_BAISSE -> "a baissé de manière significative";
            case STABLE -> "est resté stable";
        };

        String dept = ctx.get("départementNom") != null ? " dans le département " + ctx.get("départementNom") : "";

        return String.format("%s %s (%.1f%% → %.1f%%, variation de %+.1f%%)%s.",
                kpiName, tendanceText, valeur - variation * valeur / 100, valeur, variation, dept);
    }

    private String générerCauses(KpiNarrative.TypeKPI type, double variation, Map<String, Object> ctx) {
        List<String> causes = new ArrayList<>();

        if (variation < -5) {
            switch (type) {
                case PRÉSENCE -> {
                    causes.add("Période de vacances ou congés");
                    causes.add("Manque d'événements attractifs");
                    if (ctx.get("absencesConsecutives") != null) causes.add("Absences prolongées de membres clés");
                }
                case RAPPORTS -> {
                    causes.add("Surcharge de travail des faiseurs");
                    causes.add("Manque de rappels automatisés");
                }
                case SCORE_SPIRITUEL -> {
                    causes.add("Manque d'accompagnement pastoral");
                    causes.add("Éloignement progressif de la communauté");
                }
                default -> causes.add("Tendance saisonnière à surveiller");
            }
        } else if (variation > 5) {
            causes.add("Bonnes pratiques à identifier et reproduire");
            if (ctx.get("nouveauxMembres") != null) causes.add("Afflux de nouveaux membres");
        }

        return causes.isEmpty() ? "Aucune cause identifiée" : String.join(" | ", causes);
    }

    private String générerRecommandations(KpiNarrative.TypeKPI type, double variation, Map<String, Object> ctx) {
        List<String> recs = new ArrayList<>();

        if (variation < -5) {
            switch (type) {
                case PRÉSENCE -> {
                    recs.add("Organiser un événement spécial pour raviver l'engagement");
                    recs.add("Activer les rappels automatiques 24h avant le culte");
                }
                case RAPPORTS -> {
                    recs.add("Envoyer des rappels aux faiseurs en retard");
                    recs.add("Simplifier le formulaire de rapport");
                }
                case SCORE_SPIRITUEL -> {
                    recs.add("Planifier des visites pastorales prioritaires");
                    recs.add("Mettre en place un plan d'accompagnement renforcé");
                }
                default -> recs.add("Analyser les causes profondes et adapter la stratégie");
            }
        } else if (variation > 5) {
            recs.add("Documenter les bonnes pratiques");
            recs.add("Maintenir la dynamique actuelle");
        } else {
            recs.add("Explorer de nouvelles approches pour stimuler la croissance");
        }

        return String.join(" | ", recs);
    }
}
