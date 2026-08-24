package com.discipolat.modules.churchComparison.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class ChurchComparisonService {

    private final ChurchComparisonRepository repo;

    public ChurchComparisonService(ChurchComparisonRepository repo) { this.repo = repo; }

    public List<ChurchComparison> listAll() {
        return repo.findByTenantIdOrderByEffectifDesc(TenantContext.getCurrentTenantId());
    }

    public List<ChurchComparison> listByCategory(String cat) {
        return repo.findByCategorieOrderByEffectifDesc(cat);
    }

    public List<ChurchComparison> listByCountry(String pays) {
        return repo.findByPaysOrderByEffectifDesc(pays);
    }

    public ChurchComparison get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("ChurchComparison", id));
    }

    public ChurchComparison save(ChurchComparison c) {
        c.setTenantId(TenantContext.getCurrentTenantId());
        return repo.save(c);
    }

    public void delete(UUID id) { repo.deleteById(id); }

    /** Benchmark: compare our church to average of same category */
    public Map<String, Object> benchmark(UUID ourId) {
        ChurchComparison ours = get(ourId);
        List<ChurchComparison> peers = repo.findByCategorieOrderByEffectifDesc(ours.getCategorie());
        if (peers.isEmpty()) return Map.of("message", "Pas de pairs pour comparaison");

        double avgPresence = peers.stream().mapToDouble(ChurchComparison::getTauxPresence).average().orElse(0);
        double avgRetention = peers.stream().mapToDouble(ChurchComparison::getTauxRetention).average().orElse(0);
        double avgConversion = peers.stream().mapToDouble(ChurchComparison::getTauxConversion).average().orElse(0);
        double avgScore = peers.stream().mapToDouble(ChurchComparison::getScoreSpirituelMoyen).average().orElse(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notreEglise", ours.getNomEglise());
        result.put("nbPairs", peers.size());
        result.put("presence", Map.of("nous", ours.getTauxPresence(), "moyenne", Math.round(avgPresence * 100.0) / 100.0));
        result.put("retention", Map.of("nous", ours.getTauxRetention(), "moyenne", Math.round(avgRetention * 100.0) / 100.0));
        result.put("conversion", Map.of("nous", ours.getTauxConversion(), "moyenne", Math.round(avgConversion * 100.0) / 100.0));
        result.put("scoreSpirituel", Map.of("nous", ours.getScoreSpirituelMoyen(), "moyenne", Math.round(avgScore * 100.0) / 100.0));
        return result;
    }
}
