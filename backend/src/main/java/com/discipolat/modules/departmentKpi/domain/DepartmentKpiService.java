package com.discipolat.modules.departmentKpi.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DepartmentKpiService {

    private final DepartmentKpiRepository repository;

    public DepartmentKpiService(DepartmentKpiRepository repository) {
        this.repository = repository;
    }

    public List<DepartmentKpi> listByDepartment(Long departmentId) {
        return repository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
    }

    public List<DepartmentKpi> listByTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public DepartmentKpi create(DepartmentKpi kpi) {
        return repository.save(kpi);
    }

    public DepartmentKpi update(Long id, DepartmentKpi updated) {
        DepartmentKpi kpi = repository.findById(id).orElseThrow();
        kpi.setCurrentValue(updated.getCurrentValue());
        kpi.setTargetValue(updated.getTargetValue());
        return repository.save(kpi);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * KPIs calculés automatiquement pour un département.
     * Les données sont agrégées depuis les tables existantes (âmes, événements, tâches).
     *
     * En production, ces données seraient calculées via des requêtes SQL agrégées.
     * Ici, on simule avec des calculs basés sur les KPIs existants du département.
     */
    public Map<String, Object> getComputedKpis(Long departmentId, Long tenantId) {
        List<DepartmentKpi> kpis = repository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Taux de remplissage moyen (currentValue/targetValue)
        double tauxRemplissage = kpis.stream()
                .filter(k -> k.getTargetValue() != null && k.getTargetValue() > 0 && k.getCurrentValue() != null)
                .mapToDouble(k -> Math.min(100, k.getCurrentValue() / k.getTargetValue() * 100))
                .average()
                .orElse(0.0);

        // 2. Nombre total de KPIs
        long totalKpis = kpis.size();

        // 3. KPIs atteints (current >= target)
        long kpisAtteints = kpis.stream()
                .filter(k -> k.getCurrentValue() != null && k.getTargetValue() != null
                        && k.getCurrentValue() >= k.getTargetValue())
                .count();

        // 4. Taux de réalisation global
        double tauxRealisation = totalKpis > 0 ? (double) kpisAtteints / totalKpis * 100 : 0;

        // 5. Score de satisfaction (basé sur le taux de remplissage)
        double satisfaction = Math.min(100, tauxRemplissage * 0.6 + tauxRealisation * 0.4);

        // 6. Détail par KPI
        List<Map<String, Object>> details = kpis.stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", k.getId());
            m.put("nom", k.getName());
            m.put("actuel", k.getCurrentValue());
            m.put("cible", k.getTargetValue());
            m.put("unite", k.getUnit());
            m.put("periode", k.getPeriod());
            if (k.getTargetValue() != null && k.getTargetValue() > 0 && k.getCurrentValue() != null) {
                m.put("pourcentage", Math.round(k.getCurrentValue() / k.getTargetValue() * 100));
                m.put("atteint", k.getCurrentValue() >= k.getTargetValue());
            }
            return m;
        }).toList();

        // 7. KPIs en alerte (current < 60% de target)
        List<Map<String, Object>> alertes = kpis.stream()
                .filter(k -> k.getCurrentValue() != null && k.getTargetValue() != null
                        && k.getTargetValue() > 0
                        && k.getCurrentValue() / k.getTargetValue() < 0.6)
                .map(k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nom", k.getName());
                    m.put("actuel", k.getCurrentValue());
                    m.put("cible", k.getTargetValue());
                    m.put("pourcentage", Math.round(k.getCurrentValue() / k.getTargetValue() * 100));
                    m.put("ecart", k.getTargetValue() - k.getCurrentValue());
                    return m;
                }).toList();

        result.put("tauxRemplissage", Math.round(tauxRemplissage * 10.0) / 10.0);
        result.put("tauxRealisation", Math.round(tauxRealisation * 10.0) / 10.0);
        result.put("scoreSatisfaction", Math.round(satisfaction * 10.0) / 10.0);
        result.put("totalKpis", totalKpis);
        result.put("kpisAtteints", kpisAtteints);
        result.put("kpisEnAlerte", alertes.size());
        result.put("alertes", alertes);
        result.put("details", details);

        return result;
    }
}
